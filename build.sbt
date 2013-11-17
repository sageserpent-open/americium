name := "SageSerpent"

version := "0.5"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

mainClass in oneJar := Some("com.sageserpent.infrastructure.RunAllTests")

libraryDependencies += "junit" % "junit" % "4.11"

libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

libraryDependencies += "cpsuite" % "cpsuite" % "1.2.5"

products in (Test, packageBin) ++= (products in (Compile, packageBin)).value

packageBin in Compile := (packageBin in Test).value