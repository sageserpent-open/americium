import AssemblyKeys._

name := "SageSerpent"

version := "0.5"

//seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

seq(assemblySettings: _*)

//mainClass in oneJar := Some("com.sageserpent.infrastructure.RunAllTests")

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

//products in (Test, packageBin) ++= (products in (Compile, packageBin)).value

//packageBin in Compile := (packageBin in Test).value

test in assembly := {}

mainClass in assembly := Some("com.sageserpent.infrastructure.RunAllTests")

jarName in assembly := "giantRabbit.jar"

fullClasspath in assembly := (fullClasspath in (Test, assembly)).value