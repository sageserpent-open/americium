import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype._

lazy val jUnitVersion = "5.7.0"

lazy val javaVersion = "1.8"

lazy val scala2_13_Version = "2.13.7"

lazy val settings = Seq(
  crossScalaVersions     := Seq(scala2_13_Version),
  publishTo              := sonatypePublishToBundle.value,
  pomIncludeRepository   := { _ => false },
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  publishMavenStyle      := true,
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization     := "com.sageserpent",
  organizationName := "sageserpent",
  description      := "Generation of test data for parameterised testing",
  sonatypeProjectHosting := Some(
    GitHubHosting(
      user = "sageserpent-open",
      repository = "americium",
      email = "gjmurphy1@icloud.com"
    )
  ),
  releaseCrossBuild := false, // Don't use the support for cross-building provided by `sbt-release`....
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining(
      "+test"
    ), // ... instead, cross-build the testing step using SBT's own mechanism ...
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining(
      "+publishSigned"
    ), // ... likewise, cross-build the publishing step using SBT's own mechanism.
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  name         := "americium",
  scalaVersion := "2.13.8",
  scalacOptions ++= Seq("-Xsource:3"),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies += "com.softwaremill.magnolia1_2" %% "magnolia" % "1.0.0",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  libraryDependencies += "org.typelevel" %% "cats-core"             % "2.7.0",
  libraryDependencies += "org.typelevel" %% "cats-free"             % "2.7.0",
  libraryDependencies += "org.typelevel" %% "cats-collections-core" % "0.9.3",
  libraryDependencies += "io.circe"      %% "circe-core"            % "0.14.1",
  libraryDependencies += "io.circe"      %% "circe-generic"         % "0.14.1",
  libraryDependencies += "io.circe"      %% "circe-parser"          % "0.14.1",
  libraryDependencies += "com.google.guava" % "guava"   % "30.1.1-jre",
  libraryDependencies += "com.oath.cyclops" % "cyclops" % "10.4.0",
  libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.8.2",
  libraryDependencies += "org.typelevel"  %% "cats-laws"  % "2.7.0"  % Test,
  libraryDependencies += "org.scalatest"  %% "scalatest"  % "3.2.9"  % Test,
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-core" % "4.2.0" % Test,
  libraryDependencies += "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.8.2" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.8.2" % Test
  ),
  libraryDependencies += "org.hamcrest"      % "hamcrest" % "2.2"     % Test,
  libraryDependencies += "org.projectlombok" % "lombok"   % "1.18.22" % Provided
)

lazy val americium = (project in file(".")).settings(settings: _*)
