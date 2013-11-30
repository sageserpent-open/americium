import AssemblyKeys._

name := "SageSerpent"

version := "0.5"

seq(assemblySettings: _*) ++ inConfig(Test)(seq(assemblySettings: _*))

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

mainClass in assembly := Some("com.sageserpent.infrastructure.RunAllTests")

jarName in assembly := "sageserpent-infrastructure.jar"

jarName in (Test, assembly) := "sageserpent-infrastructure-with-tests.jar"




