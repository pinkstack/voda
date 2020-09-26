package com.pinkstack.voda

import java.net.URL

import pureconfig._
import pureconfig.generic.auto._
import akka.http.scaladsl.model.Uri
import pureconfig.ConfigReader.Result

object Configuration {
  case class HidroPodatki(zadnjiURL: URL,
                          dnevniURL: URL,
                          povArhivURL: URL)

  case class Config(hidroPodatki: HidroPodatki)

  final def load: Config = ConfigSource.default.at("voda").loadOrThrow[Config]
}
