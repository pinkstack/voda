package com.pinkstack.voda

import java.net.URL

import akka.NotUsed
import cats._
import cats.implicits._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  case class Postaja(sifra: String,
                     geDolzina: Double,
                     geSirina: Double,
                     kotaZero: Option[Double] = None,
                     reka: String,
                     merilnoMesto: String,
                     imeKratko: String,
                     datum: String,
                     vodostaj: Option[Double] = None,
                     pretok: Option[Double] = None,
                     pretokZnacilni: Option[String] = None,
                     tempVode: Option[Double] = None,
                     prviVVPretok: Option[Double] = None,
                     drugiVVpretok: Option[Double] = None,
                     tretjiVVpretok: Option[Double] = None,
                     znacilnaVisinaValov: Option[Double] = None,
                     smerValovanja: Option[Int] = None
                    )

}

object HidroPodatki extends ScalaXmlSupport {

  import Model._

  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)

  private[this] def flow(url: URL)
                        (implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Postaja, NotUsed] = {
    import system.dispatcher

    val toPostajaSeq: NodeSeq => Seq[Postaja] = xml =>
      (xml \\ "postaja").map { x =>
        Postaja(
          sifra = x \@ "sifra",
          geDolzina = (x \@ "ge_dolzina").toDouble,
          geSirina = (x \@ "ge_sirina").toDouble,
          kotaZero = (x \@ "kota_0").toDoubleOption,
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

  def dnevni(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Postaja, NotUsed] =
    flow(config.hidroPodatki.dnevniURL)

  def zadnji(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Postaja, NotUsed] =
    flow(config.hidroPodatki.zadnjiURL)
}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("voda")
  implicit val config: Configuration.Config = Configuration.load

  import system.dispatcher

  val r = Source.tick(0.seconds, 5.seconds, Model.Tick)
    .via(HidroPodatki.zadnji)
    .runWith(Sink.foreach(println))

  r.onComplete {
    case Success(_) =>
      system.terminate()
    case Failure(exception) =>
      System.err.println("💥" * 10)
      System.err.println(exception)
      system.terminate()
  }
}
