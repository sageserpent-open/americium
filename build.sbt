lazy val settings = Seq(
  organization := "com.sageserpent",
  name := "Americium",
  version := "SNAPSHOT",
  scalaVersion := "2.11.6",
  libraryDependencies += "junit" % "junit" % "4.10",
  libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test",
  resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals",
  libraryDependencies += "cpsuite" % "cpsuite" % "1.2.5",
  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.3",
  libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  libraryDependencies += "io.github.nicolasstucki" %% "multisets" % "0.3" % "test",
  publishMavenStyle := true)

lazy val americium = (project in file(".")).settings(settings: _*)