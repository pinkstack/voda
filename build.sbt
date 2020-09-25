import sbt.Keys.resolvers

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.pinkstack"
ThisBuild / organizationName := "voda"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.pinkstack.voda.buildinfo"
  )
  .settings(
    name := "Voda",
    libraryDependencies ++= (
      Dependencies.fp ++
        Dependencies.akka ++
        Dependencies.logging ++
        Dependencies.jsoup ++
        Dependencies.config ++
        Dependencies.testlibs
      )
  )

ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds"
)