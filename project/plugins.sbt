logLevel := Level.Warn

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")


resolvers += Resolver.sonatypeRepo("releases")
