val jUnitVersion = "5.7.0"

lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "americium",
  scalaVersion := "2.12.11",
  libraryDependencies += "com.propensive"             %% "mercator"                  % "0.3.0",
  libraryDependencies += "com.propensive"             %% "magnolia"                  % "0.17.0",
  libraryDependencies += "org.typelevel"              %% "cats-core"                 % "2.3.1",
  libraryDependencies += "org.typelevel"              %% "cats-free"                 % "2.3.1",
  libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.5",
  libraryDependencies += "io.circe"                   %% "circe-core"                % "0.14.0-M4",
  libraryDependencies += "io.circe"                   %% "circe-generic"             % "0.14.0-M4",
  libraryDependencies += "io.circe"                   %% "circe-parser"              % "0.14.0-M4",
  libraryDependencies += "org.typelevel"              %% "cats-laws"                 % "2.3.1" % Test,
  libraryDependencies += "org.scalatest"              %% "scalatest"                 % "3.0.1" % Test,
  libraryDependencies += "org.scalacheck"             %% "scalacheck"                % "1.13.5" % Test,
  libraryDependencies += "org.scalamock"              %% "scalamock"                 % "5.0.0" % Test,
  libraryDependencies += "net.aichler"                % "jupiter-interface"          % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.7.0" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.7.0" % Test
  ),
  publishMavenStyle := true,
  bintrayReleaseOnPublish in ThisBuild := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:sageserpent-open/americium.git")
)

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo
