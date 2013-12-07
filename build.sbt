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

test in assembly := {}

assemblyOption in assembly ~= { _.copy(cacheUnzip = true) }

assemblyOption in assembly ~= { _.copy(cacheOutput = true) }

jarName in assembly := "sageserpent-infrastructure.jar"

jarName in (Test, assembly) := "sageserpent-infrastructure-with-tests.jar"

val ikvmAssembly = TaskKey[Unit]("ikvmAssembly", "Converts jar from the 'assembly' task into a .NET library via IKVM.")

ikvmAssembly <<= (assembly in assembly, outputPath in assembly) map { (_, outputPath) => {
		val ikvmCommand = String.format("""ikvm\bin\ikvmc.exe -target:library %s""", outputPath)
		ikvmCommand !
	}
}

val ikvmTest = TaskKey[Unit]("ikvmTest", "Runs the test jar from the 'Test:assembly' task via IKVM.")

ikvmTest <<= (assembly in (Test, assembly), outputPath in (Test, assembly)) map { (_, outputPath) => {
		val ikvmCommand = String.format("""ikvm\bin\ikvm.exe -jar %s""", outputPath)
		ikvmCommand !
	}
}




