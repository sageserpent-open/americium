package com.sageserpent.infrastructure

import org.junit.runner.Request
import org.junit.runner.JUnitCore

import org.junit.runner.RunWith
import org.junit.Test
import org.junit.runners.Suite



@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(classOf[BargainBasementTests], classOf[RichRandomTests], classOf[UnboundedTests]))
class AllTests{
  
}

object RunAllTests extends App {
  System.out.println("Starting tests....")
  new JUnitCore().run(classOf[AllTests])
  System.out.println(".... finished tests.")
}
