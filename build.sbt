val jUnitVersion = "5.7.0"

val javaVersion = "1.8"

lazy val settings = Seq(
  name         := "americium",
  scalaVersion := "2.13.7",
  scalacOptions += s"-target:jvm-$javaVersion",
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  libraryDependencies += "com.sageserpent" %% "americium"  % "1.2.1",
  libraryDependencies += "org.typelevel"   %% "cats-laws"  % "2.4.2"  % Test,
  libraryDependencies += "org.scalatest"   %% "scalatest"  % "3.2.5"  % Test,
  libraryDependencies += "org.scalacheck"  %% "scalacheck" % "1.15.3" % Test,
  libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.6.0" % Test,
  libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.5" % Test,
  libraryDependencies += "org.scalamock"  %% "scalamock"  % "5.0.0"  % Test,
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
  libraryDependencies += "org.hamcrest" % "hamcrest" % "2.2" % Test
)

lazy val americium = (project in file(".")).settings(settings: _*)
