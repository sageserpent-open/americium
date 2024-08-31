import com.jsuereth.sbtpgp.GetSignaturesModule
import com.jsuereth.sbtpgp.PgpKeys.{signaturesModule, updatePgpSignatures}
import sbt.Tests.{Group, SubProcess}

import java.io.OutputStream

lazy val javaVersion = "19"

lazy val scala2_13_Version = "2.13.14"

lazy val scala3_Version = "3.3.3"

ThisBuild / scalaVersion := scala2_13_Version

lazy val settings = Seq(
  (updatePgpSignatures / signaturesModule) :=
    GetSignaturesModule(
      projectID.value,
      libraryDependencies.value,
      Configurations.Default :: Configurations.Pom :: Configurations.Compile :: Nil
    ),
  crossScalaVersions := Seq(scala2_13_Version, scala3_Version),
  name               := "americium",
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
      Seq("com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.7")

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
  libraryDependencies += "com.sageserpent" %% "americium"  % "1.19.5",
  libraryDependencies += "org.typelevel"   %% "cats-laws"  % "2.12.0" % Test,
  libraryDependencies += "org.scalatest"   %% "scalatest"  % "3.2.19" % Test,
  libraryDependencies += "org.scalacheck"  %% "scalacheck" % "1.18.0" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-core" % "5.13.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-junit-jupiter" % "5.13.0" % Test,
  libraryDependencies += "com.github.seregamorph" % "hamcrest-more-matchers" % "0.1" % Test,
  libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.11.0" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.11.0" % Test
  ),
  libraryDependencies += "org.hamcrest"          % "hamcrest" % "3.0"    % Test,
  libraryDependencies += "com.eed3si9n.expecty" %% "expecty"  % "0.16.0" % Test
)

lazy val americium = (project in file("."))
  .settings(settings: _*)
  .disablePlugins(plugins.JUnitXmlReportPlugin)
