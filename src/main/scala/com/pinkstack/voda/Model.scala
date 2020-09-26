package com.pinkstack.voda

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  case class Postaja(sifra: String,
                     geDolzina: Double,
                     geSirina: Double,
                     kotaZero: Option[Double] = None,
                     reka: String,
                     merilnoMesto: String,
                     imeKratko: String,
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
                    )

}
