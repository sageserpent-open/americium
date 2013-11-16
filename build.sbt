name := "SageSerpent"

version := "0.5"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

libraryDependencies += "cpsuite" % "cpsuite" % "1.2.5"

