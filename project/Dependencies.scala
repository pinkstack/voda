import sbt._

object Dependencies {
  lazy val fp: Seq[sbt.ModuleID] = Seq[ModuleID](
    // Cats
    "org.typelevel" %% "cats-core" % "2.0.0",

    // Circe 4 JSON
    "de.heikoseeberger" %% "akka-http-circe" % "1.34.0",
    "io.circe" %% "circe-core" % "0.13.0",
    "io.circe" %% "circe-generic" % "0.13.0",
    "io.circe" %% "circe-generic-extras" % "0.13.0",
    "io.circe" %% "circe-parser" % "0.13.0",
    "io.circe" %% "circe-optics" % "0.13.0",
    "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3",

    // App and Config
    "com.monovore" %% "decline" % "1.3.0",
    "com.github.pureconfig" %% "pureconfig" % "0.14.0"
  )

  lazy val akka: Seq[sbt.ModuleID] = Seq[ModuleID](
    // Akka
    "com.typesafe.akka" %% "akka-actor" % "2.6.8",
    "com.typesafe.akka" %% "akka-stream" % "2.6.8",

    "com.typesafe.akka" %% "akka-http" % "10.2.0",
    "com.typesafe.akka" %% "akka-http-xml" % "10.2.0",

    // Alpakka (+integration)
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.2",
    "com.github.tototoshi" %% "scala-csv" % "1.3.6",

    "com.nrinaudo" %% "kantan.csv" % "0.6.1",
    "com.nrinaudo" %% "kantan.csv-java8" % "0.6.1",
    "com.nrinaudo" %% "kantan.csv-cats" % "0.6.1"
  )

  lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.8"
  )

  lazy val jsoup: Seq[sbt.ModuleID] = Seq[ModuleID](
    "org.jsoup" % "jsoup" % "1.13.1"
  )

  lazy val config: Seq[sbt.ModuleID] = Seq[ModuleID](
    "com.typesafe" % "config" % "1.4.0"
  )

  lazy val testlibs: Seq[sbt.ModuleID] = Seq[ModuleID](
    "org.scalatest" %% "scalatest" % "3.2.0" % "test"
  )
}
