package com.pinkstack.voda

import java.net.URL

import pureconfig._
import pureconfig.generic.auto._
import akka.http.scaladsl.model.Uri
import pureconfig.ConfigReader.Result

object Configuration {

  case class HidroPodatki(zadnjiURL: URL, dnevniURL: URL, arhivURL: URL)

  case class TrenutneMeritve(enabled: Boolean, connectionString: String)

  case class ArhivskeMeritve(enabled: Boolean, connectionString: String)

  case class Collecting(trenutneMeritve: TrenutneMeritve,
                        arhivskeMeritve: ArhivskeMeritve)

  case class Config(hidroPodatki: HidroPodatki,
                    stations: scala.collection.immutable.Map[String, Model.Postaja],
                    collecting: Collecting
                   )

  final def load: Config = ConfigSource.default.at("voda").loadOrThrow[Config]
}
