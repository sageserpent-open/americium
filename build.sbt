val hedgehogVersion = "0.2.5"

lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "americium",
  scalaVersion := "2.12.11",
  libraryDependencies += "org.scalaz"              %% "scalaz-core"     % "7.3.0-M10",
  libraryDependencies += "com.propensive"          %% "mercator"        % "0.3.0",
  libraryDependencies += "com.propensive"          %% "magnolia"        % "0.16.0",
  libraryDependencies += "qa.hedgehog"             %% "hedgehog-core"   % hedgehogVersion,
  libraryDependencies += "qa.hedgehog"             %% "hedgehog-runner" % hedgehogVersion,
  libraryDependencies += "qa.hedgehog"             %% "hedgehog-sbt"    % hedgehogVersion,
  libraryDependencies += "org.scalatest"           %% "scalatest"       % "3.0.1",
  libraryDependencies += "org.scala-lang"          % "scala-reflect"    % scalaVersion.value % Provided,
  publishMavenStyle := true,
  bintrayReleaseOnPublish in ThisBuild := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:sageserpent-open/americium.git")
)

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo

resolvers += "bintray-scala-hedgehog" at "https://dl.bintray.com/hedgehogqa/scala-hedgehog"
