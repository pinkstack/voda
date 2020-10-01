package com.pinkstack.voda

import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl._
import cats.implicits._
import com.azure.messaging.eventhubs._
import com.pinkstack.voda.Model.StationReadingCurrent
import com.pinkstack.voda.buildinfo.BuildInfo
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq


object HydroData extends ScalaXmlSupport {

  import Model._

  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)

  private[this] def flow(url: URL)
                        (implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, StationReadingCurrent, NotUsed] = {
    import system.dispatcher

    implicit val stringToLocalDateTime: String => LocalDateTime =
      LocalDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    val toStationSeq: NodeSeq => Seq[StationReadingCurrent] = xml =>
      (xml \\ "postaja").map { x =>
        StationReadingCurrent(
          sifra = x \@ "sifra",
          geDolzina = (x \@ "ge_dolzina").toDouble,
          geSirina = (x \@ "ge_sirina").toDouble,
          kota = (x \@ "kota_0").toDoubleOption,
          reka = (x \ "reka").text,
          merilnoMesto = (x \ "merilno_mesto").text,
          imeKratko = (x \ "ime_kratko").text,
          datum = (x \ "datum").text,
          vodostaj = (x \ "vodostaj").text.toDoubleOption,
          pretok = (x \ "pretok").text.toDoubleOption,
          pretokZnacilni = (x \ "pretok_znacilni").text.some,
          tempVode = (x \ "temp_vode").text.toDoubleOption,
          prviVVPretok = (x \ "prvi_vv_pretok").text.toDoubleOption,
          drugiVVpretok = (x \ "drugi_vv_pretok").text.toDoubleOption,
          tretjiVVpretok = (x \ "tretji_vv_pretok").text.toDoubleOption,
          znacilnaVisinaValov = (x \ "znacilna_visina_valov").text.toDoubleOption,
          smerValovanja = (x \ "smer_valovanja").text.toIntOption
        )
      }

    val fetch: Future[Seq[StationReadingCurrent]] =
      Http().singleRequest(HttpRequest(uri = url))
        .flatMap(r => Unmarshal(r).to[NodeSeq])
        .map(toStationSeq)

    RestartFlow.onFailuresWithBackoff(5.seconds, 30.seconds, 0.3, 10) { () =>
      Flow[Model.Tick]
        .mapAsyncUnordered(1)(_ => fetch)
        .mapConcat(identity)
    }
  }

  def currentFlow(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, StationReadingCurrent, NotUsed] =
    flow(config.hydroData.currentURL)
}

object Liveness {
  def route(implicit system: ActorSystem): RequestContext => Future[RouteResult] = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    pathSingleSlash(get(complete(Map("status" -> "ok"))))
  }
}

object FetchMain extends LazyLogging {

  import io.circe.generic.auto._
  import io.circe.syntax._

  private[this] def shouldEmit[A, B](yes: Seq[StationReadingCurrent] => A)
                                    (no: Seq[StationReadingCurrent] => B)
                                    (measurements: Seq[StationReadingCurrent])
                                    (implicit config: Configuration.Config): Any =
    Option.when(config.collecting.currentMeasurements.enabled)(yes).getOrElse(no)(measurements)

  def main(args: Array[String] = Array.empty): Unit = {
    implicit val system: ActorSystem = ActorSystem("voda")
    import system.dispatcher
    implicit val config: Configuration.Config = Configuration.load

    logger.info(s"Version: ${BuildInfo.version} Scala: ${BuildInfo.scalaVersion} SBT: ${BuildInfo.sbtVersion}")

    val sf = Http().newServerAt("0.0.0.0", 7070).bindFlow(Liveness.route)
    val hub = AzureEventBus.currentMeasurementsProducer

    implicit val toEventData: StationReadingCurrent => EventData = m => new EventData(m.asJson.noSpaces)

    val r = Source.tick(0.seconds, 5.seconds, Model.Tick)
      .via(HydroData.currentFlow)
      .groupedWithin(100, 100.milliseconds)
      .map(shouldEmit(measurements => {
        hub.send(measurements.foldLeft(hub.createBatch()) { (batch, m) =>
          batch.tryAdd(m.copy(datum = LocalDateTime.now))
          batch
        })
        logger.info(s"Collected and emitted ${measurements.size} measurements.")
      })(measurements => measurements.map(_.asJson).foreach(println))
      ).runWith(Sink.ignore)

    def logAttempt[T]: Try[T] => Unit = {
      case Success(value) =>
        logger.info(s"Completed with ${value}")
      case Failure(exception) =>
        System.err.println("💥" * 10)
        System.err.println(exception)
    }

    system.registerOnTermination { () => hub.close() }
    sf.onComplete(logAttempt andThen (_ => logger.info(s"Server booted on http://0.0.0.0:7070/")))
    r.onComplete(logAttempt andThen (_ => system.terminate()))
  }
}
