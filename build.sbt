lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "americium",
  version := "0.2.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  libraryDependencies += "junit" % "junit" % "4.10" % "test",
  libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test",
  resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals",
  libraryDependencies += "cpsuite" % "cpsuite" % "1.2.5",
  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.0",
  libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  libraryDependencies += "io.github.nicolasstucki" %% "multisets" % "0.3" % "test",
  publishMavenStyle := true,
  bintrayReleaseOnPublish in ThisBuild := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("git@github.com:sageserpent-open/americium.git"))

lazy val americium = (project in file(".")).settings(settings: _*)

resolvers += Resolver.jcenterRepo