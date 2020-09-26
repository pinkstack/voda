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

  sealed trait IPostaja extends Product with Serializable {
    def sifra: Sifra

    def merilnoMesto: MerilnoMesto

    def reka: Reka
  }

  sealed trait Location {
    def geDolzina: GeDolzina

    def geSirina: GeSirina

    def kota: Option[Kota]
  }

  sealed trait Meritev {
    def vodostaj: Option[Double]

    def pretok: Option[Double]

    def tempVode: Option[Double]
  }

  case class PostajaMeritevTrenutna(sifra: Sifra,
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
                                   ) extends IPostaja with Location with Meritev

  case class PostajaMeritevZgodovinska(sifra: Sifra,
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
                                      ) extends IPostaja with Meritev with Location

  case class Postaja(sifra: Sifra,
                     merilnoMesto: MerilnoMesto,
                     reka: Reka,
                     imeKratko: String,
                     geDolzina: Double,
                     geSirina: Double,
                     kota: Option[Kota] = None) extends IPostaja with Location

}
