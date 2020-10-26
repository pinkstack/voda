package com.pinkstack.voda

import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Year}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ThrottleMode
import akka.stream.scaladsl._
import com.azure.messaging.eventhubs.EventData
import com.pinkstack.voda.Configuration._
import com.pinkstack.voda.Model.{Station, StationReadingHistorical}
import com.typesafe.scalalogging.LazyLogging
import kantan.csv._
import kantan.csv.ops._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object VodaArchive extends LazyLogging {

  case class CSVRow(datum: String, vodostaj: Option[Double], pretok: Option[Double], temperature: Option[Double])

  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)
  implicit val stringToLocalDate: String => LocalDate =
    LocalDate.parse(_, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
  implicit val rowDecoder: RowDecoder[CSVRow] = RowDecoder.ordered {
    (datum: String, vodostaj: Option[Double], pretok: Option[Double], tempVode: Option[Double]) =>
      CSVRow(datum, vodostaj, pretok, tempVode)
  }

  def stationsSource(implicit config: Config): Source[Station, NotUsed] =
    Source(config.stations.map(_._2))

  def buildRequests(implicit system: ActorSystem, config: Config): Flow[Station, (HttpRequest, Station), NotUsed] = {
    def from(year: Int)(station: Station): HttpRequest =
      HttpRequest(uri = Uri(config.hydroData.historicalURL.toString)
        .withQuery(Query(Map(
          "b_arhiv" -> "Prikaži",
          "p_export" -> "txt",
          "p_vodotok" -> station.reka,
          "p_postaja" -> station.sifra,
          "p_leto" -> year.toString))))

    Flow[Station].map { station =>
      val (startYear: Int, stopYear: Int) = (Year.now.getValue - 20 - 3, Year.now.getValue - 2)
      Range(startYear, stopYear).map(from(_)(station)).map(request => (request, station))
    }.mapConcat(identity)
  }

  def collectMetrics(implicit system: ActorSystem, config: Config): Flow[Station, StationReadingHistorical, NotUsed] = {
    import system.dispatcher
    def parseCSV(raw: String)(request: HttpRequest): List[Option[CSVRow]] = {
      val r = raw.asCsvReader[CSVRow](rfc.withCellSeparator(';').withHeader).map(_.toOption).toList
      if (r.isEmpty) logger.error(s"Failed at ${request.uri.toString}")
      r
    }

    def concat(r: CSVRow)(p: Station): StationReadingHistorical = {
      StationReadingHistorical(
        p.sifra, p.merilnoMesto, p.reka, p.imeKratko, p.geDolzina, p.geSirina, p.kota,
        r.datum, r.vodostaj, r.pretok, r.temperature)
    }

    VodaArchive.buildRequests
      .throttle(60, 1.second, 40, ThrottleMode.Shaping)
      .mapAsync(4) { case (r, ctx) =>
        Http().singleRequest(r)
          .flatMap(response => Unmarshal(response).to[String])
          .map(body => parseCSV(body)(r))
          .map(_.map(_.map(concat(_)(ctx))))
      }
      .filterNot(_.isEmpty)
      .mapConcat(identity)
      .filterNot(_.isEmpty)
      .map(_.getOrElse(throw new Exception("Only Row pass this point.")))
  }
}

object ArchiveMain extends LazyLogging {

  import io.circe.generic.auto._, io.circe.syntax._

  private[this] def shouldEmit[A, B](yes: Seq[StationReadingHistorical] => A)
                                    (no: Seq[StationReadingHistorical] => B)
                                    (measurements: Seq[StationReadingHistorical])
                                    (implicit config: Configuration.Config): Any =
    Option.when(config.collecting.historicalMeasurements.enabled)(yes).getOrElse(no)(measurements)

  def main(args: Array[String] = Array.empty): Unit = {
    implicit val system: ActorSystem = ActorSystem("voda-arhiv")
    implicit val config: Config = Configuration.load

    import system.dispatcher

    val hub = AzureEventBus.historicalMeasurementsProducer

    implicit val toEventData: StationReadingHistorical => EventData = m => new EventData(m.asJson.noSpaces)

    val f = VodaArchive.stationsSource
      .via(VodaArchive.collectMetrics)
      .groupedWithin(5000, 1.second)
      .map(shouldEmit(measurements => {
        hub.send(measurements.foldLeft(hub.createBatch()) { (batch, m) =>
          batch.tryAdd(m)
          batch
        })
        logger.info(s"Collected and emitted ${measurements.size} historical measurements.")
      })(jsons => jsons.map(_.asJson).foreach(println)))
      .runWith(Sink.ignore)

    system.registerOnTermination { () => hub.close() }

    f.onComplete {
      case Success(_) =>
        system.terminate()
      case Failure(exception) =>
        System.err.println(exception)
    }
  }
}
