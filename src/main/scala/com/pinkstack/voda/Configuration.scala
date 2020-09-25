package com.pinkstack.voda

import java.net.URL

import akka.http.scaladsl.model.Uri
import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic.auto._

object Configuration {

  case class HidroPodatki(zadnjiURL: URL, dnevniURL: URL)

  case class Config(hidroPodatki: HidroPodatki)

  final def load: Config = ConfigSource.default.at("voda").loadOrThrow[Config]
}
