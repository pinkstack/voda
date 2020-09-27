package com.pinkstack.voda

import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl._
import cats.implicits._
import com.azure.messaging.eventhubs._
import com.pinkstack.voda.buildinfo.BuildInfo
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.xml.NodeSeq


object HidroPodatki extends ScalaXmlSupport {

  import Model._

  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)

  private[this] def flow(url: URL)
                        (implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, PostajaMeritevTrenutna, NotUsed] = {
    import system.dispatcher

    implicit val stringToLocalDateTime: String => LocalDateTime =
      LocalDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    val toPostajaSeq: NodeSeq => Seq[PostajaMeritevTrenutna] = xml =>
      (xml \\ "postaja").map { x =>
        PostajaMeritevTrenutna(
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

    val fetch = Http().singleRequest(HttpRequest(uri = url))
      .flatMap(r => Unmarshal(r).to[NodeSeq])
      .map(toPostajaSeq)

    RestartFlow.onFailuresWithBackoff(5.seconds, 30.seconds, 0.3, 10) { () =>
      Flow[Model.Tick]
        .mapAsyncUnordered(1)(_ => fetch)
        .mapConcat(identity)
    }
  }

  def dnevni(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, PostajaMeritevTrenutna, NotUsed] =
    flow(config.hidroPodatki.dnevniURL)

  def zadnji(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, PostajaMeritevTrenutna, NotUsed] =
    flow(config.hidroPodatki.zadnjiURL)
}


object Liveness {
  def route(implicit system: ActorSystem) = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

    pathSingleSlash {
      get(complete(Map("status" -> "ok")))
    }
  }
}

object FetchMain extends LazyLogging {

  import scala.jdk.CollectionConverters._
  import io.circe.generic.auto._, io.circe.syntax._


  def main(args: Array[String] = Array.empty): Unit = {
    implicit val system: ActorSystem = ActorSystem("voda")
    implicit val config: Configuration.Config = Configuration.load

    logger.info(s"Booting ${BuildInfo.name} version ${BuildInfo.version} with " +
      s"Scala ${BuildInfo.scalaVersion} and SBT ${BuildInfo.sbtVersion}")

    import system.dispatcher

    val sf = Http().newServerAt("0.0.0.0", 7070).bindFlow(Liveness.route)

    val eh = AzureEventBus.trenutneMeritveProducer

    val r = Source.tick(0.seconds, 5.seconds, Model.Tick)
      .via(HidroPodatki.zadnji)
      .map(_.asJson)
      .groupedWithin(100, 100.milliseconds)
      .map { jsons =>
        if (config.collecting.trenutneMeritve.enabled) {
          eh.send(jsons.map(_.noSpaces).map(new EventData(_)).asJava)
          Map("tm - emitted_in_batch" -> jsons.size)
        } else
          Map("tm - collected_in_batch" -> jsons.size)
      }
      .runWith(Sink.foreach(println))

    system.registerOnTermination { () =>
      logger.info("Closing EH")
      eh.close()
    }

    sf.onComplete {
      case Success(_) => logger.info("Server booted,...")
      case Failure(ex) =>
        System.err.println(ex)
        system.terminate()
    }

    r.onComplete {
      case Success(r) =>
        logger.info(s"Stream completed with ${r}")
        system.terminate()
      case Failure(exception) =>
        System.err.println("💥" * 10)
        System.err.println(exception)
        system.terminate()
    }
  }
}
