package com.sageserpent.americium.junit5

import com.sageserpent.americium.Trials
import com.sageserpent.americium.junit5.java.FailingTrialsTestExample
import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.testkit.engine.EngineTestKit
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class JUnit5FailureAndReplaySpec
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks {
  "running a failing dynamicTest under JUnit5" should "produce com.sageserpent.americium.junit5.TrialException with recipe/recipeId diagnostics and support replay" in {
    forAll(Table("failingCase", 1, 2, 3, 4, 5)) { failingCase =>
      FailingDynamicTestExample.failingCase = failingCase

      val results = EngineTestKit
        .engine("junit-jupiter")
        .selectors(
          DiscoverySelectors.selectClass(classOf[FailingDynamicTestExample])
        )
        .configurationParameter(
          "junit.jupiter.conditions.deactivate",
          "org.junit.*DisabledCondition"
        )
        .execute()

      val testEvents  = results.testEvents()
      val failedTests = testEvents.failed().list()
      failedTests should not be empty

      val failure = failedTests
        .get(0)
        .getRequiredPayload(
          classOf[org.junit.platform.engine.TestExecutionResult]
        )
        .getThrowable
        .get()
      failure shouldBe a[com.sageserpent.americium.junit5.TrialException]
      val trialException =
        failure.asInstanceOf[com.sageserpent.americium.junit5.TrialException]
      val recipe     = trialException.recipe
      val recipeHash = trialException.recipeHash

      recipe should not be empty
      recipeHash should not be empty
      trialException.toString should include("Reproduce via Java property:")
      trialException.toString should include("trials.recipeHash=")

      // Now test replay with trials.recipe
      System.setProperty("trials.recipe", recipe)
      try {
        val replayResults = EngineTestKit
          .engine("junit-jupiter")
          .selectors(
            DiscoverySelectors.selectClass(classOf[FailingDynamicTestExample])
          )
          .configurationParameter(
            "junit.jupiter.conditions.deactivate",
            "org.junit.*DisabledCondition"
          )
          .execute()

        val replayEvents = replayResults.testEvents()
        replayEvents.started().count() shouldBe 1
        replayEvents.failed().count() shouldBe 1
      } finally {
        System.clearProperty("trials.recipe")
      }

      // Now test replay with trials.recipeHash
      System.setProperty("trials.recipeHash", recipeHash)
      try {
        val replayResults = EngineTestKit
          .engine("junit-jupiter")
          .selectors(
            DiscoverySelectors.selectClass(classOf[FailingDynamicTestExample])
          )
          .configurationParameter(
            "junit.jupiter.conditions.deactivate",
            "org.junit.*DisabledCondition"
          )
          .execute()

        val replayEvents = replayResults.testEvents()
        replayEvents.started().count() shouldBe 1
        replayEvents.failed().count() shouldBe 1
      } finally {
        System.clearProperty("trials.recipeHash")
      }
    }
  }

  "running a failing annotated TrialsTest under JUnit5" should "produce com.sageserpent.americium.junit5.TrialException with recipe/recipeId diagnostics and support replay" in {
    forAll(Table("failingCase", 1, 2, 3, 4, 5)) { failingCase =>
      FailingTrialsTestExample.failingCase = failingCase

      val results = EngineTestKit
        .engine("junit-jupiter")
        .selectors(
          DiscoverySelectors.selectClass(classOf[FailingTrialsTestExample])
        )
        .configurationParameter(
          "junit.jupiter.conditions.deactivate",
          "org.junit.*DisabledCondition"
        )
        .execute()

      val testEvents  = results.testEvents()
      val failedTests = testEvents.failed().list()
      failedTests should not be empty

      val failure = failedTests
        .get(0)
        .getRequiredPayload(
          classOf[org.junit.platform.engine.TestExecutionResult]
        )
        .getThrowable
        .get()
      failure shouldBe a[com.sageserpent.americium.junit5.TrialException]
      val trialException =
        failure.asInstanceOf[com.sageserpent.americium.junit5.TrialException]
      val recipe     = trialException.recipe
      val recipeHash = trialException.recipeHash

      recipe should not be empty
      recipeHash should not be empty
      trialException.toString should include("Reproduce via Java property:")
      trialException.toString should include("trials.recipeHash=")

      // Now test replay with trials.recipe
      System.setProperty("trials.recipe", recipe)
      try {
        val replayResults = EngineTestKit
          .engine("junit-jupiter")
          .selectors(
            DiscoverySelectors.selectClass(classOf[FailingTrialsTestExample])
          )
          .configurationParameter(
            "junit.jupiter.conditions.deactivate",
            "org.junit.*DisabledCondition"
          )
          .execute()

        val replayEvents = replayResults.testEvents()
        replayEvents.started().count() shouldBe 1
        replayEvents.failed().count() shouldBe 1
      } finally {
        System.clearProperty("trials.recipe")
      }

      // Now test replay with trials.recipeHash
      System.setProperty("trials.recipeHash", recipeHash)
      try {
        val replayResults = EngineTestKit
          .engine("junit-jupiter")
          .selectors(
            DiscoverySelectors.selectClass(classOf[FailingTrialsTestExample])
          )
          .configurationParameter(
            "junit.jupiter.conditions.deactivate",
            "org.junit.*DisabledCondition"
          )
          .execute()

        val replayEvents = replayResults.testEvents()
        replayEvents.started().count() shouldBe 1
        replayEvents.failed().count() shouldBe 1
      } finally {
        System.clearProperty("trials.recipeHash")
      }
    }
  }
}

@org.junit.jupiter.api.Disabled
class FailingDynamicTestExample {
  @TestFactory
  def failingTest(): DynamicTests = {
    // Choose 1, 2, 3, 4, 5 so we are guaranteed to include the failing case
    Trials.api.choose(1, 2, 3, 4, 5).withLimit(5).dynamicTests { caze =>
      if (caze == FailingDynamicTestExample.failingCase) {
        throw new RuntimeException(s"Deliberate failure for caze $caze")
      }
    }
  }
}

object FailingDynamicTestExample {
  var failingCase: Int = 1
}
