package com.sageserpent.infrastructure

import org.junit.extensions.cpsuite.ClasspathSuite
import org.junit.runner.RunWith
import org.junit.Test


@RunWith(classOf[ClasspathSuite])
class AllTests {
}

class ThisShouldWork{
  @Test
  def aTestThatIsnt(){
    System.out.println("At least this test gets run!")
  }  
}
