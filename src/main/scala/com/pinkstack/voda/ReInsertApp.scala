package com.pinkstack.voda

import Configuration._
import akka.actor.ActorSystem
import com.azure.messaging.eventhubs._
import com.typesafe.scalalogging.LazyLogging

object ReInsertApp extends LazyLogging {


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("re-insert")
    implicit val config: Config = Configuration.load

    lazy val eb = AzureEventBus.historicalMeasurementsConsumer(eventContext => {
      val body: String = eventContext.getEventData.getBodyAsString
      println(body)
    })(errorContext => {
      println("boom", errorContext)
    })

    eb.start()
  }
}
