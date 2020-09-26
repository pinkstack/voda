package com.pinkstack.voda

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl._
import cats._
import cats.implicits._
import Configuration._
import akka.http.scaladsl.model._

object VodaArchive {
  implicit val urlToURI: URL => Uri = (url: URL) => Uri(url.toString)


  def fetchMainCategories(implicit system: ActorSystem, config: Config) = {
    import system.dispatcher

    val r = HttpRequest(uri = config.hidroPodatki.dnevniURL)
    Http().singleRequest(r).flatMap(r =>
      r
    )
  }
}

object ArchiveMain extends App {
  implicit val system = ActorSystem("voda-arhiv")
  implicit val config: Config = Configuration.load

  // 1. fetch categories
  // 2. fetch rows


}
