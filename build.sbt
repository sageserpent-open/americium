import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype._

val jUnitVersion = "5.7.0"

val javaVersion = "1.8"

lazy val settings = Seq(
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
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  name         := "americium",
  scalaVersion := "2.13.5",
  scalacOptions += s"-target:jvm-$javaVersion",
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies += "com.propensive" %% "mercator"              % "0.3.0",
  libraryDependencies += "com.propensive" %% "magnolia"              % "0.17.0",
  libraryDependencies += "org.typelevel"  %% "cats-core"             % "2.6.1",
  libraryDependencies += "org.typelevel"  %% "cats-free"             % "2.6.1",
  libraryDependencies += "org.typelevel"  %% "cats-collections-core" % "0.9.3",
  libraryDependencies += "io.circe"       %% "circe-core"            % "0.14.1",
  libraryDependencies += "io.circe"       %% "circe-generic"         % "0.14.1",
  libraryDependencies += "io.circe"       %% "circe-parser"          % "0.14.1",
  libraryDependencies += "com.google.guava" % "guava"   % "30.1.1-jre",
  libraryDependencies += "com.oath.cyclops" % "cyclops" % "10.4.0",
  libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.8.0",
  libraryDependencies += "org.typelevel"  %% "cats-laws"  % "2.6.1"  % Test,
  libraryDependencies += "org.scalatest"  %% "scalatest"  % "3.2.9"  % Test,
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test,
  libraryDependencies += "org.scalamock" %% "scalamock" % "5.1.0" % Test,
  libraryDependencies += "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.8.0" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.8.0" % Test
  ),
  libraryDependencies += "org.hamcrest" % "hamcrest" % "2.2" % Test
)

lazy val americium = (project in file(".")).settings(settings: _*)
