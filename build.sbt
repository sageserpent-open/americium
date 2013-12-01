import AssemblyKeys._

name := "SageSerpent"

version := "0.5"

seq(assemblySettings: _*) ++ inConfig(Test)(seq(assemblySettings: _*))

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

mainClass in assembly := Some("com.sageserpent.infrastructure.RunAllTests")

val unacceptableStrategies = Set(MergeStrategy.singleOrError)

mergeStrategy in assembly ~= { existingStrategy =>
	input => {
		val decisionFromExistingStrategy = existingStrategy(input)
		if (unacceptableStrategies.contains(decisionFromExistingStrategy))
			MergeStrategy.first
		else
			decisionFromExistingStrategy
	}
}

assemblyOption in assembly ~= { _.copy(cacheUnzip = false) }

assemblyOption in assembly ~= { _.copy(cacheOutput = false) }

jarName in assembly := "sageserpent-infrastructure.jar"

jarName in (Test, assembly) := "sageserpent-infrastructure-with-tests.jar"

proguardSettings

ProguardKeys.options in Proguard ++= Seq("-keep public class com.sageserpent.infrastructure.**")

ProguardKeys.options in Proguard ++= Seq("-keepclasseswithmembers public class * {public static void main(java.lang.String[]);}")

ProguardKeys.options in Proguard ++= Seq("-dontobfuscate", "-dontusemixedcaseclassnames", "-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.inputs in Proguard := {
	val annoyingTaskDependencyWorkaround = (assembly in (Test, assembly)).value
	Seq((outputPath in (Test, assembly)).value)
}





