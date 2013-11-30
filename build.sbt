proguardSettings

name := "SageSerpent"

version := "0.5"

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.novocode" % "junit-interface" % "0.10" % "test"

resolvers += "http://maven.xwiki.org" at "http://maven.xwiki.org/externals"

ProguardKeys.options in Proguard ++= Seq("-keep public class com.sageserpent.infrastructure.*")

ProguardKeys.options in Proguard ++= Seq("-dontobfuscate", "-dontusemixedcaseclassnames", "-dontnote", "-dontwarn", "-ignorewarnings")

//ProguardKeys.inputs in Proguard <<= fullClasspath in Test map { _.files }

