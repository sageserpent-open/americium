import sbt.Tests.{Group, SubProcess}
import sbtrelease.ReleaseStateTransformations.*

import java.io.OutputStream

lazy val javaVersion = "17"

lazy val scala2_13_Version = "2.13.18"

lazy val scala3_Version = "3.3.7"

ThisBuild / scalaVersion     := scala2_13_Version
ThisBuild / organization     := "com.sageserpent"
ThisBuild / organizationName := "sageserpent"

// Common settings for all modules
lazy val commonSettings = Seq(
  crossScalaVersions   := Seq(scala2_13_Version, scala3_Version),
  pomIncludeRepository := { _ => false },
  publishMavenStyle    := true,
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization     := "com.sageserpent",
  organizationName := "sageserpent",
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
  Test / fork               := true,
  Test / testForkedParallel := false,
  Test / test / logLevel    := Level.Error,
  Test / testOptions += Tests.Argument(jupiterTestFramework, "-q"),
  // Test grouping for isolated databases
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
  Global / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 6))
)

lazy val coreDependencies = Def.setting {
  Seq(
    "com.typesafe.scala-logging"   %% "scala-logging"         % "3.9.6",
    "ch.qos.logback"                % "logback-core"          % "1.5.29",
    "org.typelevel"                %% "cats-core"             % "2.13.0",
    "org.typelevel"                %% "cats-free"             % "2.13.0",
    "org.typelevel"                %% "cats-collections-core" % "0.9.10",
    "co.fs2"                       %% "fs2-core"              % "3.12.2",
    "io.circe"                     %% "circe-core"            % "0.14.15",
    "io.circe"                     %% "circe-generic"         % "0.14.15",
    "io.circe"                     %% "circe-parser"          % "0.14.15",
    "com.google.guava"              % "guava"                 % "33.5.0-jre",
    "com.github.ben-manes.caffeine" % "caffeine"              % "3.2.3",
    "com.oath.cyclops"              % "cyclops"               % "10.4.1",
    "org.rocksdb"                   % "rocksdbjni"            % "10.2.1",
    "org.apache.commons"            % "commons-text"          % "1.15.0",
    "com.lihaoyi"                  %% "pprint"                % "0.9.6",
    "uk.org.webcompere"             % "system-stubs-core"     % "2.1.8",
    "net.bytebuddy"                 % "byte-buddy"            % "1.18.5"
  )
}

lazy val scalaVersionDependencies = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      Seq(
        "com.softwaremill.magnolia1_2" %% "magnolia"      % "1.1.10",
        "org.scala-lang"                % "scala-reflect" % scalaVersion.value
      )
    case Some((3, _)) =>
      Seq("com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.18")
    case _ => Seq.empty
  }
}

lazy val coreTestDependencies = Def.setting {
  Seq(
    "com.github.valfirst"    % "slf4j-test"             % "3.0.3"    % Test,
    "org.typelevel"         %% "cats-laws"              % "2.13.0"   % Test,
    "org.scalatest"         %% "scalatest"              % "3.2.19"   % Test,
    "org.scalacheck"        %% "scalacheck"             % "1.19.0"   % Test,
    "org.scalatestplus"     %% "scalacheck-1-16"        % "3.2.14.0" % Test,
    "org.mockito"            % "mockito-core"           % "5.21.0"   % Test,
    "com.github.seregamorph" % "hamcrest-more-matchers" % "1.0"      % Test,
    "org.hamcrest"           % "hamcrest"               % "3.0"      % Test,
    "com.eed3si9n.expecty"  %% "expecty"                % "0.17.1"   % Test,
    "org.mockito"            % "mockito-junit-jupiter"  % "5.21.0"   % Test,
    "org.junit.jupiter" % "junit-jupiter-params" % JupiterKeys.junitJupiterVersion.value,
    "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
    "org.junit.jupiter" % "junit-jupiter-engine" % JupiterKeys.junitJupiterVersion.value % Test,
    "org.junit.platform" % "junit-platform-testkit" % JupiterKeys.junitPlatformVersion.value % Test,
    "org.junit.platform" % "junit-platform-suite-api" % JupiterKeys.junitPlatformVersion.value % Test
  )
}

lazy val junit5Dependencies = Def.setting {
  Seq(
    "org.junit.platform" % "junit-platform-launcher" % JupiterKeys.junitPlatformVersion.value,
    "uk.org.webcompere" % "system-stubs-jupiter" % "2.1.8"
  )
}

lazy val americium: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name        := "americium",
    description := "Generation of test data for parameterised testing",
    libraryDependencies ++= coreDependencies.value,
    libraryDependencies ++= scalaVersionDependencies.value,
    libraryDependencies ++= coreTestDependencies.value,
  )
  .disablePlugins(plugins.JUnitXmlReportPlugin)

lazy val `americium-junit5`: Project = (project in file("junit5"))
  .dependsOn(americium % "test->test;compile->compile")
  .settings(commonSettings)
  .settings(
    name        := "americium-junit5",
    description := "JUnit5 integration for Americium property-based testing",
    libraryDependencies ++= junit5Dependencies.value
  )
  .disablePlugins(plugins.JUnitXmlReportPlugin)

lazy val root: Project = (project in file("."))
  .aggregate(americium, `americium-junit5`)
  .settings(
    name              := "americium-root",
    publish / skip    := true,
    releaseCrossBuild := false,
    releaseProcess    := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+clean"),
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion
    )
  )
