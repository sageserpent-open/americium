import sbt.Tests.{Group, SubProcess}
import sbtrelease.ReleaseStateTransformations.*
import xerial.sbt.Sonatype.*

lazy val javaVersion = "1.9"

lazy val scala2_13_Version = "2.13.12"

lazy val scala3_Version = "3.3.1"

ThisBuild / scalaVersion := scala2_13_Version

lazy val settings = Seq(
  crossScalaVersions     := Seq(scala2_13_Version, scala3_Version),
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
    releaseStepCommandAndRemaining(
      "+clean"
    ), // ... instead, cross-build the clean step using SBT's own mechanism ...
    releaseStepCommandAndRemaining(
      "+test"
    ), // ... and the testing step using SBT's own mechanism ...
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining(
      "+publishSigned"
    ), // ... finally the publishing step using SBT's own mechanism.
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  name := "americium",
  scalacOptions ++= (CrossVersion.partialVersion(
    scalaVersion.value
  ) match {
    case Some((2, _)) =>
      Seq("-Xsource:3")
    case Some((3, _)) =>
      Seq("-explain")

    case _ => Nil
  }),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies ++= (CrossVersion.partialVersion(
    scalaVersion.value
  ) match {
    case Some((2, _)) =>
      Seq(
        "com.softwaremill.magnolia1_2" %% "magnolia"      % "1.1.3",
        "org.scala-lang"                % "scala-reflect" % scalaVersion.value
      )
    case Some((3, _)) =>
      Seq("com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.2")

    case _ => Seq.empty
  }),
  Test / testGrouping := {
    val tests = (Test / definedTests).value

    tests
      .groupBy(_.name)
      .map { case (groupName, group) =>
        new Group(
          groupName,
          group,
          SubProcess(
            (Test / forkOptions).value.withRunJVMOptions(
              Vector(
                s"-Dtrials.runDatabase=trialsRunDatabaseForGroup$groupName"
              )
            )
          )
        )
      }
      .toSeq
  },
  Global / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 6)),
  Test / fork                     := true,
  Test / testForkedParallel       := false,
  libraryDependencies += "org.typelevel" %% "cats-core"             % "2.7.0",
  libraryDependencies += "org.typelevel" %% "cats-free"             % "2.7.0",
  libraryDependencies += "org.typelevel" %% "cats-collections-core" % "0.9.3",
  libraryDependencies += "co.fs2"        %% "fs2-core"              % "3.2.5",
  libraryDependencies += "io.circe"      %% "circe-core"            % "0.14.2",
  libraryDependencies += "io.circe"      %% "circe-generic"         % "0.14.2",
  libraryDependencies += "io.circe"      %% "circe-parser"          % "0.14.2",
  libraryDependencies += "com.google.guava" % "guava"   % "32.0.0-jre",
  libraryDependencies += "com.oath.cyclops" % "cyclops" % "10.4.0",
  libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.10.2",
  libraryDependencies += "org.rocksdb"        % "rocksdbjni"   % "7.1.1",
  libraryDependencies += "org.apache.commons" % "commons-text" % "1.10.0",
  libraryDependencies += ("com.github.cb372" %% "scalacache-caffeine" % "0.28.0") cross CrossVersion.for3Use2_13,
  libraryDependencies += "com.lihaoyi"    %% "pprint"     % "0.8.1",
  libraryDependencies += "org.typelevel"  %% "cats-laws"  % "2.7.0"  % Test,
  libraryDependencies += "org.scalatest"  %% "scalatest"  % "3.2.9"  % Test,
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-core" % "4.2.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-junit-jupiter" % "4.2.0" % Test,
  libraryDependencies += "com.github.seregamorph" % "hamcrest-more-matchers" % "0.1" % Test,
  libraryDependencies += "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.10.2" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.10.2" % Test
  ),
  libraryDependencies += "org.hamcrest"          % "hamcrest" % "2.2"    % Test,
  libraryDependencies += "com.eed3si9n.expecty" %% "expecty"  % "0.16.0" % Test
)

lazy val americium = (project in file(".")).settings(settings: _*)
