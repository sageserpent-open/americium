package com.sageserpent.americium.junit5.bugReports

import com.eed3si9n.expecty.Expecty.assert
import com.sageserpent.americium.Trials
import com.sageserpent.americium.Trials.api as trialsApi
import com.sageserpent.americium.generation.JavaPropertyNames.nondeterministicJavaProperty
import com.sageserpent.americium.junit5.*
import com.sageserpent.americium.junit5.bugReports.Issue255BugReproduction.HiddenTestSuite
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.{Disabled, Test, TestFactory}
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.testkit.engine.{
  EngineExecutionResults,
  EngineTestKit,
  EventType
}
import uk.org.webcompere.systemstubs.jupiter.{SystemStub, SystemStubsExtension}
import uk.org.webcompere.systemstubs.properties.SystemProperties

import scala.jdk.OptionConverters.*
import scala.jdk.StreamConverters.*

object Issue255BugReproduction {

  private val sharedSyntaxSupply: Trials[Int]#SupplySyntaxType =
    trialsApi.integers(1, 100).withLimit(50)

  @Disabled
  class HiddenTestSuite {
    @TestFactory
    def firstTestUsingTheSharedSyntaxSupply(): DynamicTests =
      sharedSyntaxSupply.dynamicTests { value =>
        println(s"firstTestUsingTheSharedSyntaxSupply: $value")
      }

    @TestFactory
    def secondTestUsingTheSharedSyntaxSupply(): DynamicTests =
      sharedSyntaxSupply.dynamicTests { value =>
        println(s"secondTestUsingTheSharedSyntaxSupply: $value")
      }
  }
}

//Bug report from: https://github.com/sageserpent-open/americium/issues/255
@ExtendWith(Array(classOf[SystemStubsExtension]))
class Issue255BugReproduction {
  // Make sure that the trials are generated repeatably between tests.
  @SystemStub
  private val systemProperties =
    new SystemProperties(nondeterministicJavaProperty, "false")

  @Test
  def allTestsUsingASharedSyntaxSupplyShouldRun(): Unit = {
    val results: EngineExecutionResults = EngineTestKit
      .engine("junit-jupiter")
      .selectors(DiscoverySelectors.selectClass(classOf[HiddenTestSuite]))
      .configurationParameter(
        "junit.jupiter.conditions.deactivate",
        "org.junit.*DisabledCondition"
      )
      .execute

    val overallTestOutcomes = results
      .containerEvents()
      .stream()
      .toScala(List)
      .filter(event =>
        EventType.FINISHED == event.getType && event.getTestDescriptor
          .isInstanceOf[TestFactoryTestDescriptor]
      )
      .flatMap(_.getPayload().toScala)
      .collect { case testExecutionResult: TestExecutionResult =>
        testExecutionResult.getStatus
      }

    assert(
      overallTestOutcomes.forall(TestExecutionResult.Status.SUCCESSFUL == _)
    )

    val fineGrainedTestDisplayNames = results
      .testEvents()
      .stream()
      .toScala(List)
      .filter(EventType.FINISHED == _.getType)
      .map(_.getTestDescriptor)
      .map(_.getDisplayName)

    assert(
      2 * fineGrainedTestDisplayNames.toSet.size == fineGrainedTestDisplayNames.size,
      "Each test case from the shared supply syntax should be run twice, once in the first hidden test, then again in the second hidden test."
    )
  }
}
