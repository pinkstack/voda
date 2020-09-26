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
import akka.stream.scaladsl._
import com.pinkstack.voda.Configuration._
import com.pinkstack.voda.Model.{Postaja, PostajaMeritevZgodovinska}
import kantan.csv._
import kantan.csv.ops._

import scala.util.{Failure, Success}

object VodaArchive {
  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)

  def stationsSource(implicit config: Config): Source[Postaja, NotUsed] = {
    Source(config.stations.map(_._2))
  }

  def buildRequests(implicit system: ActorSystem, config: Config): Flow[Postaja, (HttpRequest, Postaja), NotUsed] = {
    def from(year: Int)(postaja: Postaja): HttpRequest =
      HttpRequest(uri = Uri(config.hidroPodatki.arhivURL.toString)
        .withQuery(Query(Map(
          "b_arhiv" -> "Prikaži",
          "p_export" -> "txt",
          "p_vodotok" -> postaja.reka,
          "p_postaja" -> postaja.sifra,
          "p_leto" -> year.toString))))

    //FlowWithContext[Postaja, Postaja].map { postaja =>
    Flow[Postaja].map { case (postaja: Postaja) =>
      val (startYear: Int, stopYear: Int) = (Year.now.getValue - 20 - 3, Year.now.getValue - 3)
      Range(startYear, stopYear).map(from(_)(postaja)).map(request => (request, postaja))
    }.mapConcat(identity)
  }

  def collectMetrics(implicit system: ActorSystem, config: Config): Flow[Postaja, PostajaMeritevZgodovinska, NotUsed] = {
    import system.dispatcher

    case class Row(datum: String, vodostaj: Option[Double], pretok: Option[Double], temperature: Option[Double])
    implicit val rowDecoder: RowDecoder[Row] = RowDecoder.ordered {
      (datum: String,
       vodostaj: Option[Double],
       pretok: Option[Double],
       tempVode: Option[Double]
      ) =>
        Row(datum, vodostaj, pretok, tempVode)
    }

    def parseCSV(raw: String)(request: HttpRequest) = {
      val r = raw.asCsvReader[Row](rfc.withCellSeparator(';').withHeader).map(_.toOption).toList
      if (r.isEmpty) System.err.println(s"Failed at ${request.uri.toString}")
      r
    }

    def rowWithPostaja(r: Row)(p: Postaja): PostajaMeritevZgodovinska = {
      implicit val stringToLocalDate: String => LocalDate = LocalDate.parse(_, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

      PostajaMeritevZgodovinska(
        p.sifra, p.merilnoMesto, p.reka, p.imeKratko, p.geDolzina, p.geSirina, p.kota,
        r.datum, r.vodostaj, r.pretok, r.temperature)
    }

    VodaArchive.buildRequests
      .mapAsync(4) { case (r, ctx) =>
        Http().singleRequest(r)
          .flatMap(response => Unmarshal(response).to[String])
          .map(body => parseCSV(body)(r))
          .map(_.map(_.map(rowWithPostaja(_)(ctx))))
      }
      .filterNot(_.isEmpty)
      .mapConcat(identity)
      .filterNot(_.isEmpty)
      .map(_.getOrElse(throw new Exception("Only Row pass this point.")))
  }
}

object ArchiveMain extends App {
  implicit val system: ActorSystem = ActorSystem("voda-arhiv")
  implicit val config: Config = Configuration.load

  import system.dispatcher

  val f = VodaArchive.stationsSource
    .via(VodaArchive.collectMetrics)
    .runWith(Sink.foreach(println))

  f.onComplete {
    case Success(_) =>
      system.terminate()
    case Failure(exception) =>
      System.err.println(exception)
  }
}
