import xerial.sbt.Sonatype._

val jUnitVersion = "5.7.0"

val javaVersion = "1.8"

lazy val settings = Seq(
  publishTo := sonatypePublishToBundle.value,
  pomIncludeRepository := { _ => false },
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  publishMavenStyle := true,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  organization := "com.sageserpent",
  organizationName := "sageserpent",
  description := "Generation of test data for paramterised testing",
  sonatypeProjectHosting := Some(
    GitHubHosting(
      user = "sageserpent-open",
      repository = "americium",
      email = "gjmurphy1@icloud.com"
    )
  ),
  name := "americium",
  scalaVersion := "2.13.5",
  scalacOptions += s"-target:jvm-${javaVersion}",
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies += "com.propensive"             %% "mercator"                  % "0.3.0",
  libraryDependencies += "com.propensive"             %% "magnolia"                  % "0.17.0",
  libraryDependencies += "org.typelevel"              %% "cats-core"                 % "2.4.2",
  libraryDependencies += "org.typelevel"              %% "cats-free"                 % "2.4.2",
  libraryDependencies += "org.typelevel"              %% "cats-collections-core"     % "0.9.1",
  libraryDependencies += "io.circe"                   %% "circe-core"                % "0.14.0-M4",
  libraryDependencies += "io.circe"                   %% "circe-generic"             % "0.14.0-M4",
  libraryDependencies += "io.circe"                   %% "circe-parser"              % "0.14.0-M4",
  libraryDependencies += "org.typelevel"              %% "cats-laws"                 % "2.4.2"                          % Test,
  libraryDependencies += "org.scalatest"              %% "scalatest"                 % "3.2.5"                          % Test,
  libraryDependencies += "org.scalacheck"             %% "scalacheck"                % "1.15.3"                         % Test,
  libraryDependencies += "org.scalatestplus"          %% "scalacheck-1-15"           % "3.2.6.0"                        % Test,
  libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.5"                          % Test,
  libraryDependencies += "org.scalamock"              %% "scalamock"                 % "5.0.0"                          % Test,
  libraryDependencies += "net.aichler"                 % "jupiter-interface"         % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.7.0" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.7.0" % Test
  )
)

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo
