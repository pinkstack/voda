package com.pinkstack.voda

import java.time.{LocalDate, LocalDateTime}

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  type Sifra = String
  type Reka = String
  type MerilnoMesto = String
  type GeDolzina = Double
  type GeSirina = Double
  type Kota = Double

  sealed trait IStation extends Product with Serializable {
    def sifra: Sifra

    def merilnoMesto: MerilnoMesto

    def reka: Reka
  }

  sealed trait Location {
    def geDolzina: GeDolzina

    def geSirina: GeSirina

    def kota: Option[Kota]
  }

  sealed trait Reading {
    def vodostaj: Option[Double]

    def pretok: Option[Double]

    def tempVode: Option[Double]
  }

  case class StationReadingCurrent(sifra: Sifra,
                                   merilnoMesto: MerilnoMesto,
                                   reka: Reka,
                                   imeKratko: String,
                                   geDolzina: GeDolzina,
                                   geSirina: GeSirina,
                                   kota: Option[Kota] = None,
                                   datum: LocalDateTime,
                                   vodostaj: Option[Double] = None,
                                   pretok: Option[Double] = None,
                                   pretokZnacilni: Option[String] = None,
                                   tempVode: Option[Double] = None,
                                   prviVVPretok: Option[Double] = None,
                                   drugiVVpretok: Option[Double] = None,
                                   tretjiVVpretok: Option[Double] = None,
                                   znacilnaVisinaValov: Option[Double] = None,
                                   smerValovanja: Option[Int] = None
                                  ) extends IStation with Location with Reading

  case class StationReadingHistorical(sifra: Sifra,
                                      merilnoMesto: MerilnoMesto,
                                      reka: Reka,
                                      imeKratko: String,
                                      geDolzina: GeDolzina,
                                      geSirina: GeSirina,
                                      kota: Option[Kota] = None,
                                      datum: LocalDate,
                                      vodostaj: Option[Double] = None,
                                      pretok: Option[Double] = None,
                                      tempVode: Option[Double] = None
                                     ) extends IStation with Reading with Location

  case class Station(sifra: Sifra,
                     merilnoMesto: MerilnoMesto,
                     reka: Reka,
                     imeKratko: String,
                     geDolzina: Double,
                     geSirina: Double,
                     kota: Option[Kota] = None) extends IStation with Location

}
