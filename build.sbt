import sbt.Tests.{Group, SubProcess}
import sbtrelease.ReleaseStateTransformations.*

import java.io.OutputStream

lazy val javaVersion = "17"

lazy val scala2_13_Version = "2.13.15"

lazy val scala3_Version = "3.3.4"

ThisBuild / scalaVersion := scala2_13_Version

lazy val settings = Seq(
  crossScalaVersions   := Seq(scala2_13_Version, scala3_Version),
  pomIncludeRepository := { _ => false },
  publishMavenStyle    := true,
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization     := "com.sageserpent",
  organizationName := "sageserpent",
  description      := "Generation of test data for parameterised testing",
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
    // *DO NOT* run `+publishSigned`, `sonatypeBundleRelease` and `pushChanges`
    // - the equivalent is done on GitHub by
    // `gha-scala-library-release-workflow`.
    setNextVersion,
    commitNextVersion
  ),
  name := "americium",
  scalacOptions ++= (CrossVersion.partialVersion(
    scalaVersion.value
  ) match {
    case Some((2, _)) =>
      Seq("-Xsource:3", s"-java-output-version:$javaVersion")
    case Some((3, _)) =>
      Seq("-explain", s"-java-output-version:$javaVersion")

    case _ => Nil
  }),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies ++= (CrossVersion.partialVersion(
    scalaVersion.value
  ) match {
    case Some((2, _)) =>
      Seq(
        "com.softwaremill.magnolia1_2" %% "magnolia"      % "1.1.10",
        "org.scala-lang"                % "scala-reflect" % scalaVersion.value
      )
    case Some((3, _)) =>
      Seq("com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.8")

    case _ => Seq.empty
  }),
  Test / test / logLevel := Level.Error,
  Test / testOptions += Tests.Argument(jupiterTestFramework, "-q"),
  Test / testGrouping := {
    val tests = (Test / definedTests).value

    tests
      .groupBy(_.name)
      .map { case (groupName, group) =>
        new Group(
          groupName,
          group,
          SubProcess(
            (Test / forkOptions).value
              .withRunJVMOptions(
                Vector(
                  s"-Dtrials.runDatabase=trialsRunDatabaseForGroup$groupName"
                )
              )
              .withOutputStrategy(
                OutputStrategy.CustomOutput(OutputStream.nullOutputStream)
              )
          )
        )
      }
      .toSeq
  },
  Global / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 6)),
  Test / fork                     := true,
  Test / testForkedParallel       := false,
  libraryDependencies += "org.typelevel" %% "cats-core"             % "2.12.0",
  libraryDependencies += "org.typelevel" %% "cats-free"             % "2.12.0",
  libraryDependencies += "org.typelevel" %% "cats-collections-core" % "0.9.9",
  libraryDependencies += "co.fs2"        %% "fs2-core"              % "3.11.0",
  libraryDependencies += "io.circe"      %% "circe-core"            % "0.14.10",
  libraryDependencies += "io.circe"      %% "circe-generic"         % "0.14.10",
  libraryDependencies += "io.circe"      %% "circe-parser"          % "0.14.10",
  libraryDependencies += "com.google.guava" % "guava"   % "33.4.0-jre",
  libraryDependencies += "com.oath.cyclops" % "cyclops" % "10.4.1",
  libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.11.3",
  libraryDependencies += "org.junit.platform" % "junit-platform-launcher" % "1.11.3",
  libraryDependencies += "org.rocksdb"        % "rocksdbjni"   % "9.7.3",
  libraryDependencies += "org.apache.commons" % "commons-text" % "1.13.0",
  libraryDependencies += "com.lihaoyi"       %% "pprint"       % "0.9.0",
  libraryDependencies += "org.typelevel"  %% "cats-laws"  % "2.12.0" % Test,
  libraryDependencies += "org.scalatest"  %% "scalatest"  % "3.2.19" % Test,
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.18.1" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-core" % "5.14.2" % Test,
  libraryDependencies += "org.mockito" % "mockito-junit-jupiter" % "5.14.2" % Test,
  libraryDependencies += "com.github.seregamorph" % "hamcrest-more-matchers" % "0.1" % Test,
  libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.11.3" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.11.3" % Test
  ),
  libraryDependencies += "org.hamcrest"          % "hamcrest" % "3.0"    % Test,
  libraryDependencies += "com.eed3si9n.expecty" %% "expecty"  % "0.17.0" % Test
)

lazy val americium = (project in file("."))
  .settings(settings: _*)
  .disablePlugins(plugins.JUnitXmlReportPlugin)
