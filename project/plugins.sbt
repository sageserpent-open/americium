addDependencyTreePlugin

addSbtPlugin("com.github.sbt.junit" % "sbt-jupiter-interface" % "0.19.0")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"          % "2.6.1")
// TODO: reinstate this plugin once a version is published that
// supports SBT 2. For now, the release process might be broken.
//addSbtPlugin("ch.epfl.scala"        % "sbt-version-policy"    % "3.2.1")
addSbtPlugin("com.github.sbt"       % "sbt-release"           % "1.5.0")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"         % "2.4.4")
