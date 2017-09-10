lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "americium",
  scalaVersion := "2.12.1",
  libraryDependencies += "junit"                   % "junit"           % "4.10" % "test",
  libraryDependencies += "com.novocode"            % "junit-interface" % "0.10" % "test",
  libraryDependencies += "org.scalaz"              %% "scalaz-core"    % "7.3.0-M10",
  libraryDependencies += "org.scalatest"           %% "scalatest"      % "3.0.1" % "test",
  libraryDependencies += "org.scalacheck"          %% "scalacheck"     % "1.13.5" % "test",
  libraryDependencies += "io.github.nicolasstucki" %% "multisets"      % "0.4" % "test",
  publishMavenStyle := true,
  bintrayReleaseOnPublish in ThisBuild := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:sageserpent-open/americium.git")
)

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo
