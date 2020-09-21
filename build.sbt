lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "americium",
  scalaVersion := "2.12.11",
  libraryDependencies += "com.propensive" %% "mercator"     % "0.3.0",
  libraryDependencies += "com.propensive" %% "magnolia"     % "0.16.0",
  libraryDependencies += "org.scalatest"  %% "scalatest"    % "3.0.1",
  libraryDependencies += "org.scalacheck" %% "scalacheck"   % "1.13.5" % "test",
  libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.8",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  publishMavenStyle := true,
  bintrayReleaseOnPublish in ThisBuild := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:sageserpent-open/americium.git")
)

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo
