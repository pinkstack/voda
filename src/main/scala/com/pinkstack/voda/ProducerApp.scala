/**
 * As ARSO went down I had to fake the data. ;///
 */

package com.pinkstack.voda

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import cats._
import cats.implicits._

import scala.concurrent.duration._
import com.azure.messaging.eventhubs.{EventData, EventHubProducerClient}
import com.pinkstack.voda.Configuration.TrenutneMeritve
import com.pinkstack.voda.Model.{PostajaMeritevTrenutna, PostajaMeritevZgodovinska}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.util.Random
import scala.util.{Failure, Success}

object ProducerApp {

  import scala.jdk.CollectionConverters._
  import io.circe.generic.auto._, io.circe.syntax._
  import io.circe.Decoder, io.circe.generic.semiauto.deriveDecoder
  import io.circe.Encoder, io.circe.generic.semiauto.deriveEncoder

  implicit val trenutnaEncoder: Encoder[Model.PostajaMeritevTrenutna] = deriveEncoder[Model.PostajaMeritevTrenutna]
  implicit val arhivskaEncoder: Encoder[Model.PostajaMeritevZgodovinska] = deriveEncoder[Model.PostajaMeritevZgodovinska]

  def fakeApp[T](eb: EventHubProducerClient)
                (fakeMethod: Configuration.Config => Seq[T])
                (implicit system: ActorSystem, config: Configuration.Config) = {
    import system.dispatcher
    import io.circe.generic.auto._, io.circe.syntax._

    val f = Source.tick(0.seconds, 5.second, "Tick")
      .map { _ => Source(fakeMethod(config)) }
      .flatMapConcat(identity)
      .map {
        case (p: Model.PostajaMeritevZgodovinska) => p.asJson.noSpaces
        case (p: Model.PostajaMeritevTrenutna) => p.asJson.noSpaces
      }
      .map { jsonString: String =>
        eb.send(Seq(new EventData(jsonString)).asJava)
        jsonString
      }
      .runWith(Sink.foreach(println))

    system.registerOnTermination { () =>
      eb.close()
    }

    f.onComplete {
      case Success(_) =>
        system.terminate()
      case Failure(exception) =>
        System.err.println(exception)
        system.terminate()
    }
  }

  def fakeTrenutneMeritve(implicit system: ActorSystem, configuration: Configuration.Config) = {
    fakeApp[Model.PostajaMeritevTrenutna](AzureEventBus.trenutneMeritveProducer)(buildFakeTrenutneMeritve)
  }

  def fakeArhivskeMeritve(implicit system: ActorSystem, configuration: Configuration.Config) = {
    fakeApp[Model.PostajaMeritevZgodovinska](AzureEventBus.arhivskeMeritveProducer)(buildFakeArhivskeMeritve)
  }

  val buildFakeTrenutneMeritve: Configuration.Config => Seq[Model.PostajaMeritevTrenutna] = { configuration =>
    Seq({
      val p = configuration.stations.get("5030").get
      PostajaMeritevTrenutna(
        p.sifra,
        p.reka,
        p.merilnoMesto,
        p.imeKratko,
        p.geDolzina,
        p.geSirina,
        p.kota,
        LocalDateTime.now(),
        vodostaj = (300.0 + (Random.between(-10, 50))).some,
        pretok = (50.6 + (Random.between(-5, 50))).some,
        tempVode = (12.0 + (Random.between(-2, 4))).some
      )
    },
      {
        val p = configuration.stations.get("5040").get
        PostajaMeritevTrenutna(
          p.sifra,
          p.reka,
          p.merilnoMesto,
          p.imeKratko,
          p.geDolzina,
          p.geSirina,
          p.kota,
          LocalDateTime.now()
        )
      },
      {
        val p = configuration.stations.get("5078").get
        PostajaMeritevTrenutna(
          p.sifra,
          p.reka,
          p.merilnoMesto,
          p.imeKratko,
          p.geDolzina,
          p.geSirina,
          p.kota,
          LocalDateTime.now(),
          vodostaj = (148.0 + (Random.between(-10, 60))).some,
          pretok = (103.6 + (Random.between(-20, 70))).some,
          tempVode = (14.0 + (Random.between(0, 6))).some
        )
      },
    )
  }

  val buildFakeArhivskeMeritve: Configuration.Config => Seq[Model.PostajaMeritevZgodovinska] = { configuration =>
    Seq({
      val p = configuration.stations.get("5030").get
      PostajaMeritevZgodovinska(
        p.sifra,
        p.reka,
        p.merilnoMesto,
        p.imeKratko,
        p.geDolzina,
        p.geSirina,
        p.kota,
        LocalDate.now(),
        vodostaj = (300.0 + (Random.between(-10, 50))).some,
        pretok = (50.6 + (Random.between(-5, 50))).some,
        tempVode = (12.0 + (Random.between(-2, 4))).some
      )
    },
      {
        val p = configuration.stations.get("5040").get
        PostajaMeritevZgodovinska(
          p.sifra,
          p.reka,
          p.merilnoMesto,
          p.imeKratko,
          p.geDolzina,
          p.geSirina,
          p.kota,
          LocalDate.now()
        )
      },
      {
        val p = configuration.stations.get("5078").get
        PostajaMeritevZgodovinska(
          p.sifra,
          p.reka,
          p.merilnoMesto,
          p.imeKratko,
          p.geDolzina,
          p.geSirina,
          p.kota,
          LocalDate.now(),
          vodostaj = (148.0 + (Random.between(-10, 60))).some,
          pretok = (103.6 + (Random.between(-20, 70))).some,
          tempVode = (14.0 + (Random.between(0, 6))).some
        )
      },
    )
  }


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("test")
    implicit val config: Configuration.Config = Configuration.load

    if (args.contains("--archive"))
      fakeArhivskeMeritve
    else
      fakeTrenutneMeritve
  }
}
