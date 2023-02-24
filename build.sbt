import sbt.Tests.{Group, SubProcess}

lazy val scala2_13_Version = "2.13.8"
lazy val scala3_Version    = "3.1.2"
lazy val settings = Seq(
  crossScalaVersions := Seq(scala2_13_Version, scala3_Version),
  name               := "americium",
  scalacOptions ++= (CrossVersion.partialVersion(
    scalaVersion.value
  ) match {
    case Some((2, _)) =>
      Seq("-Xsource:3")
    case Some((3, _)) =>
      Seq.empty

    case _ => Nil
  }),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies += "com.sageserpent" %% "americium"  % "1.10.2",
  libraryDependencies += "org.typelevel"   %% "cats-laws"  % "2.7.0"  % Test,
  libraryDependencies += "org.scalatest"   %% "scalatest"  % "3.2.9"  % Test,
  libraryDependencies += "org.scalacheck"  %% "scalacheck" % "1.15.4" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-core" % "4.2.0" % Test,
  libraryDependencies += "org.mockito" % "mockito-junit-jupiter" % "4.2.0" % Test,
  libraryDependencies += "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
  libraryDependencies ++= Seq(
    "org.junit.platform" % "junit-platform-runner" % "1.8.2" % Test,
    "org.junit.jupiter"  % "junit-jupiter-engine"  % "5.8.2" % Test
  ),
  libraryDependencies += "org.hamcrest" % "hamcrest" % "2.2" % Test,
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
  Test / testForkedParallel       := false
)
lazy val americium = (project in file(".")).settings(settings: _*)

ThisBuild / scalaVersion := scala2_13_Version
val jUnitVersion = "5.7.0"
val javaVersion  = "1.9"
