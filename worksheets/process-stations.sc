/**
 * This code generates HOCON configuration out of stations XML.
 * The configuration can then be used inside the rest of the project.
 */

import java.io.{BufferedWriter, File, FileWriter}
import scala.xml.XML
import scala.xml._

// Read and process
val exampleFile = XML.loadFile("hacking/hidro_podatki_zadnji.xml")
val content: Seq[String] = (exampleFile \\ "postaja").toList.map { n =>
  val (sifra, reka, merilnoMesto, geDolzina, geSirina, imeKratko) = (
    n \@ "sifra", (n \ "reka").text, (n \ "merilno_mesto").text,
    n \@ "ge_dolzina", n \@ "ge_sirina", (n \ "ime_kratko").text
  )

  s"""
     |"${sifra}": {
     |  sifra = "${sifra}"
     |  reka = "${reka}"
     |  merilno-mesto = "${merilnoMesto}"
     |  ime-kratko = "${imeKratko}"
     |  ge-sirina = ${geSirina}
     |  ge-dolzina = ${geDolzina}
     | }""".stripMargin
}

val template: String => String = { middle =>
  s"""
     |# This is machine generated file
     |# To re-generate run "worksheets/process-stations.sc"
     |# Otherwise don't touch it.
     |
     |voda.stations {${middle}""".stripMargin
}
val outputConfiguration: String = template(content.mkString("\n"))

// Write configuration to file
val bw = new BufferedWriter(new FileWriter(new File("src/main/resources/stations.conf")))
for (line <- outputConfiguration) bw.write(line)
bw.close()