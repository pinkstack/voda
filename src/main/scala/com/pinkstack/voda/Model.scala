package com.pinkstack.voda

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  type Sifra = String
  type Reka = String
  type MerilnoMesto = String
  type GeDolzina = Double
  type GeSirina = Double

  sealed trait IPostaja {
    def sifra: Sifra

    def merilnoMesto: MerilnoMesto

    def reka: Reka
  }

  sealed trait Location {
    def geDolzina: GeDolzina

    def geSirina: GeSirina
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
                                    kotaZero: Option[Double] = None,
                                    datum: String,
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
                                       datum: String,
                                       vodostaj: Option[Double] = None,
                                       pretok: Option[Double] = None,
                                       tempVode: Option[Double] = None,
                                      ) extends IPostaja with Meritev

  case class Postaja(sifra: Sifra,
                     merilnoMesto: MerilnoMesto,
                     reka: Reka,
                     imeKratko: String,
                     geDolzina: Double,
                     geSirina: Double) extends IPostaja with Location

}
