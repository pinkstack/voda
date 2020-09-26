package com.pinkstack.voda

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import Configuration.Config

import scala.io.StdIn

object ServerMain extends LazyLogging {
  implicit val system: ActorSystem = ActorSystem("voda-server")
  implicit val config: Config = Configuration.load

  def main(args: Array[String] = Array.empty[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("voda-server")
    implicit val config: Config = Configuration.load
    import system.dispatcher
    val f = Http().newServerAt("0.0.0.0", 7070).bindFlow(route)

    StdIn.readLine("Hit ENTER to exit")
    f.flatMap(_.unbind()).onComplete(_ => system.terminate)
  }

  private def route(implicit actorSystem: ActorSystem, config: Config) = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.auto._

    pathSingleSlash {
      get {
        complete(Map("stations" -> config.stations.values))
      }
    }
  }
}