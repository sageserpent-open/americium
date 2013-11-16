package com.sageserpent.infrastructure

import org.junit.extensions.cpsuite.ClasspathSuite
import org.junit.runner.RunWith
import org.junit.runner.JUnitCore

@RunWith(classOf[ClasspathSuite])
object AllTestsInTheJar extends App {
  new JUnitCore().run(AllTestsInTheJar.getClass());
}
