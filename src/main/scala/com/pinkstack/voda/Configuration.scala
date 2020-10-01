package com.pinkstack.voda

import java.net.URL

import pureconfig._
import pureconfig.generic.auto._
import akka.http.scaladsl.model.Uri
import pureconfig.ConfigReader.Result

object Configuration {

  case class HydroData(currentURL: URL, historicalURL: URL)

  case class CurrentMeasurements(enabled: Boolean, connectionString: String)

  case class HistoricalMeasurements(enabled: Boolean, connectionString: String)

  case class Collecting(currentMeasurements: CurrentMeasurements,
                        historicalMeasurements: HistoricalMeasurements)

  case class Config(hydroData: HydroData,
                    stations: scala.collection.immutable.Map[String, Model.Station],
                    collecting: Collecting
                   )

  final def load: Config = ConfigSource.default.at("voda").loadOrThrow[Config]
}
