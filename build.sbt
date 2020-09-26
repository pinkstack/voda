import sbt.Keys.resolvers
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.0.3"
ThisBuild / organization := "com.pinkstack"
ThisBuild / organizationName := "voda"

lazy val dockerSettings = Seq(
  dockerUsername := Some("pinkstack"),
  dockerUpdateLatest := false,
  dockerBaseImage := "azul/zulu-openjdk-alpine:11-jre",
  dockerCommands := dockerCommands.value.flatMap {
    case add@Cmd("RUN", args@_*) if args.contains("id") =>
      List(
        Cmd("LABEL", "maintainer Oto Brglez <otobrglez@gmail.com>"),
        Cmd("LABEL", "org.opencontainers.image.url https://github.com/pinkstack/voda"),
        Cmd("LABEL", "org.opencontainers.image.source https://github.com/pinkstack/voda"),
        Cmd("RUN", "apk add --no-cache bash jq curl"),
        Cmd("ENV", "SBT_VERSION", sbtVersion.value),
        Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
        Cmd("ENV", "VODA_VERSION", version.value),
        add
      )
    case other => List(other)
  },
  // Additional aliases
  dockerAliases ++= {
    if (!sys.env.contains("CI"))
      Seq(dockerAlias.value.withTag(Option("local")))
    else
      Seq(
        dockerAlias.value.withRegistryHost(Option("ghcr.io"))
          .withUsername(Option("pinkstack"))
          .withName("voda")
          .withTag(Option(version.value))
        ,
        dockerAlias.value.withRegistryHost(Option("ghcr.io"))
          .withUsername(Option("pinkstack"))
          .withName("voda")
          .withTag(Option("latest"))
        ,
        dockerAlias.value.withRegistryHost(Option("docker.pkg.github.com"))
          .withUsername(Option("pinkstack/voda"))
          .withName("voda")
          .withTag(Option("latest"))
        ,
        dockerAlias.value.withRegistryHost(Option("docker.pkg.github.com"))
          .withUsername(Option("pinkstack/voda"))
          .withName("voda")
          .withTag(Option(version.value))
        ,
        dockerAlias.value.withTag(Option("latest")),
        dockerAlias.value.withTag(Option(version.value))
      )
  }
)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin, JavaAgent, GitBranchPrompt) // JavaAppPackaging)
  .settings(
    name := "Voda",
    libraryDependencies ++= (
      Dependencies.fp ++
        Dependencies.akka ++
        Dependencies.logging ++
        Dependencies.jsoup ++
        Dependencies.config ++
        Dependencies.testlibs
      ),
    Compile / mainClass := Some("com.pinkstack.voda.Main"),
  )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.pinkstack.voda.buildinfo"
  )
  .enablePlugins(JavaAppPackaging)
  .settings(assemblyJarName in assembly := "voda.jar")
  .settings(dockerSettings: _*)


ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("zamblauskas", "maven")
)

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds"
)