import sbt.Tests.{Group, SubProcess}
import sbtrelease.ReleaseStateTransformations.*

import java.io.OutputStream

lazy val javaVersion = "17"

lazy val scala2_13_Version = "2.13.18"

lazy val scala3_Version = "3.3.7"

ThisBuild / scalaVersion := scala2_13_Version

// Common settings for all modules: not all of these work when
// scoped to `ThisBuild`, so go with this brute-force approach.
lazy val commonSettings = Seq(
  crossScalaVersions   := Seq(scala2_13_Version, scala3_Version),
  pomIncludeRepository := { _ => false },
  publishMavenStyle    := true,
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization     := "com.sageserpent",
  organizationName := "sageserpent",
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
        val trialsRunDatabaseName =
          if (
            groupName.contains(
              "TrialsSpecInQuarantineDueToUseOfRecipeHashSystemProperty"
            )
          ) s"trialsRunDatabaseIsolatedForTestGroup$groupName"
          else "trialsRunDatabaseSharedAcrossTestGroups"

        new Group(
          groupName,
          group,
          SubProcess(
            (Test / forkOptions).value
              .withRunJVMOptions(
                Vector(
                  s"-Dtrials.runDatabase=$trialsRunDatabaseName"
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
    "ch.qos.logback"                % "logback-core"          % "1.5.32",
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
    "com.lihaoyi"                  %% "os-lib"                % "0.11.3",
    "org.apache.commons"            % "commons-text"          % "1.15.0",
    "com.lihaoyi"                  %% "pprint"                % "0.9.6",
    "net.bytebuddy" % "byte-buddy" % "1.18.6-jdk6-jdk5"
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

lazy val `americium-utilities`: Project =
  (project in file("utilities"))
    .settings(commonSettings)
    .settings(
      name := "americium-utilities",
      description := "Utilities used internally by Americium that are of use to other projects",
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )

lazy val `external-tests`: Project = (project in file("external-tests"))
  .dependsOn(
    `americium-utilities` % "test->compile;test->test",
    americium             % "test->compile"
  )
  .settings(commonSettings)
  .settings(
    name := "external-tests",
    description := "Tests in their own project to break pseudo-cyclic dependencies",
    publish / skip                         := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )

lazy val americium: Project = (project in file("core-library"))
  .dependsOn(`americium-utilities` % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name        := "americium",
    description := "Generation of test data for parameterised testing",
    libraryDependencies ++= coreDependencies.value,
    libraryDependencies ++= scalaVersionDependencies.value,
    libraryDependencies += "com.github.valfirst" % "slf4j-test" % "3.0.3" % Test,
    libraryDependencies += "org.typelevel"  %% "cats-laws"  % "2.13.0" % Test,
    libraryDependencies += "org.scalatest"  %% "scalatest"  % "3.2.19" % Test,
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.19.0" % Test,
    libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % Test,
    libraryDependencies += "org.mockito" % "mockito-core" % "5.21.0" % Test,
    libraryDependencies += "com.github.seregamorph" % "hamcrest-more-matchers" % "1.0" % Test,
    libraryDependencies += "org.hamcrest" % "hamcrest" % "3.0" % Test,
    libraryDependencies += "org.mockito" % "mockito-junit-jupiter" % "5.21.0" % Test,
    libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % JupiterKeys.junitJupiterVersion.value % Test,
    libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test
  )
  .disablePlugins(plugins.JUnitXmlReportPlugin)

lazy val `americium-junit5`: Project = (project in file("junit5-integration"))
  .dependsOn(americium)
  .settings(commonSettings)
  .settings(
    name        := "americium-junit5",
    description := "JUnit5 integration for Americium property-based testing",
    // Pin the *non-test* JUnit dependencies to align with JUnit5 rather than
    // picking up the version from `JupiterKeys`.
    libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.14.3",
    libraryDependencies += "org.junit.platform" % "junit-platform-launcher" % "1.14.3",

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "uk.org.webcompere" % "system-stubs-jupiter" % "2.1.8" % Test,
    libraryDependencies += "com.github.valfirst" % "slf4j-test" % "3.0.3" % Test,
    libraryDependencies += "org.hamcrest" % "hamcrest" % "3.0" % Test,
    libraryDependencies += "com.eed3si9n.expecty" %% "expecty" % "0.17.1" % Test,
    libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % JupiterKeys.junitJupiterVersion.value % Test,
    libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
    libraryDependencies += "org.junit.jupiter" % "junit-jupiter-engine" % JupiterKeys.junitJupiterVersion.value % Test,
    libraryDependencies += "org.junit.platform" % "junit-platform-testkit" % JupiterKeys.junitPlatformVersion.value % Test
  )
  .disablePlugins(plugins.JUnitXmlReportPlugin)

lazy val root: Project = (project in file("."))
  .aggregate(
    `americium-utilities`,
    americium,
    `americium-junit5`,
    `external-tests`
  )
  .settings(
    name           := "americium-root",
    publish / skip := true,
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
      // *DO NOT* run `+publishSigned`, `sonatypeBundleRelease` and
      // `pushChanges`
      // - the equivalent is done on GitHub by
      // `gha-scala-library-release-workflow`.
      setNextVersion,
      commitNextVersion
    )
  )
