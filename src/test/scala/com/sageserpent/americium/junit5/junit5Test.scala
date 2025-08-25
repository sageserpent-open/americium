package com.sageserpent.americium.junit5
import com.sageserpent.americium.MockitoSessionSupport
import com.sageserpent.americium.Trials.api
import org.junit.jupiter.api.DynamicTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.function.Consumer

class junit5Test extends AnyFlatSpec with Matchers with MockitoSessionSupport {
  "junit5" should "shrink test cases when a failure occurs" in {
    val problem = new RuntimeException("Not divisible by 5.")

    def test(caze: Int): Unit = {
      println(caze)
      // Every multiple of 5 throws an exception...
      if (0 == caze % 5) throw problem
    }

    val integers = api.integers

    val dynamicTests = integers.withLimit(20).dynamicTests(test)

    val action: Consumer[DynamicTest] = test =>
      try {
        test.getExecutable.execute()
      } catch {
        case _: RuntimeException =>
      }

    val exception =
      intercept[integers.TrialException](dynamicTests.forEachRemaining(action))

    exception.getCause should be theSameInstanceAs problem

    // we expect the maximally shrunk value of zero to have been submitted as it
    // is also a multiple of 5...
    exception.provokingCase should be(0)
  }
}
