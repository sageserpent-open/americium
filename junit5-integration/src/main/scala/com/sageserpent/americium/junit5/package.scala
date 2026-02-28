package com.sageserpent.americium
import com.sageserpent.americium.java.{
  CaseFailureReporting,
  InlinedCaseFiltration,
  TestIntegrationContext
}
import com.sageserpent.americium.junit5.java.{
  ConfiguredTrialsTest,
  LauncherDiscoveryListenerCapturingReplayedUniqueIds,
  TestExecutionListenerCapturingUniqueIds,
  TrialsTest
}
import com.sageserpent.americium.junit5.storage.JUnit5ReplayStorage
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.platform.engine.UniqueId
import org.opentest4j.TestAbortedException

import _root_.java.util.Iterator as JavaIterator
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IteratorHasAsJava, SetHasAsScala}
import scala.jdk.OptionConverters.RichOptional

package object junit5 {
  type DynamicTests = JavaIterator[DynamicTest]
  implicit class Syntax[Case](
      private val supplier: TrialsScaffolding.SupplyToSyntax[Case]
  ) extends AnyVal {

    /** Provide dynamic tests for JUnit5, allowing direct coupling between a
      * [[TrialsScaffolding.SupplyToSyntax]] and the parameterised test
      * consuming the supplied test cases. <p> In contrast to the 'classic'
      * approach taken by [[TrialsTest]] and [[ConfiguredTrialsTest]], this
      * affords strong type-checking between the test's arguments and the
      * supplier. <p> It does however sacrifice the potential use of
      * [[org.junit.jupiter.api.BeforeEach]] and
      * [[org.junit.jupiter.api.AfterEach]] to perform set-up and tear-down for
      * each trial - that has to be coded explicitly in the parameterised test
      * itself. <p> Example:
      * {{{
      *   import com.sageserpent.americium.junit5._
      * }}}
      * {{{
      *   @TestFactory
      *   def dynamicTestsExample(): DynamicTests = {
      *     val supplier: TrialsScaffolding.SupplyToSyntax[Int] =
      *       Trials.api.integers.withLimit(10)
      *
      *      supplier
      *         .dynamicTests(
      *            // The parameterised test: it just prints out the test case...
      *            println
      *        )
      *   }
      * }}}
      *
      * @param parameterisedTest
      *   Parameterised test that consumes a test case of type {@code Case} .
      * @return
      *   An iterator of [[DynamicTest]] instances, suitable for use with the
      *   [[org.junit.jupiter.api.TestFactory]] annotation provided by JUnit5.
      */
    def dynamicTests(
        parameterisedTest: Case => Unit
    ): DynamicTests =
      junit5.dynamicTests(
        supplier.testIntegrationContexts(),
        supplier.reproduce,
        parameterisedTest
      )
  }

  implicit class Tuple2Syntax[Case1, Case2](
      private val supplier: TrialsScaffolding.SupplyToSyntax[(Case1, Case2)]
  ) extends AnyVal {

    /** Overload for a parameterised test taking two arguments.
      * @see
      *   [[Syntax.dynamicTests]]
      */
    def dynamicTests(
        parameterisedTest: (Case1, Case2) => Unit
    ): DynamicTests = junit5.dynamicTests(
      supplier.testIntegrationContexts(),
      supplier.reproduce,
      parameterisedTest.tupled
    )
  }

  implicit class Tuple3Syntax[Case1, Case2, Case3](
      private val supplier: TrialsScaffolding.SupplyToSyntax[
        (Case1, Case2, Case3)
      ]
  ) extends AnyVal {

    /** Overload for a parameterised test taking three arguments.
      * @see
      *   [[Syntax.dynamicTests]]
      */
    def dynamicTests(
        parameterisedTest: (Case1, Case2, Case3) => Unit
    ): DynamicTests = junit5.dynamicTests(
      supplier.testIntegrationContexts(),
      supplier.reproduce,
      parameterisedTest.tupled
    )
  }

  /** Overload for a parameterised test taking four arguments.
    * @see
    *   [[Syntax.dynamicTests]]
    */
  implicit class Tuple4Syntax[Case1, Case2, Case3, Case4](
      private val supplier: TrialsScaffolding.SupplyToSyntax[
        (Case1, Case2, Case3, Case4)
      ]
  ) extends AnyVal {
    def dynamicTests(
        parameterisedTest: (Case1, Case2, Case3, Case4) => Unit
    ): DynamicTests = junit5.dynamicTests(
      supplier.testIntegrationContexts(),
      supplier.reproduce,
      parameterisedTest.tupled
    )
  }

  private[americium] def dynamicTests[Case](
      contexts: Iterator[TestIntegrationContext[Case]],
      reproduceFromRecipe: String => Case,
      parameterisedTest: Case => Unit
  ): DynamicTests = {
    val replayedUniqueIds =
      LauncherDiscoveryListenerCapturingReplayedUniqueIds
        .replayedUniqueIds()
        .asScala

    val casesAvailableForReplayByUniqueId: mutable.Map[UniqueId, Case] =
      mutable.Map.from(
        replayedUniqueIds
          .flatMap(uniqueId =>
            JUnit5ReplayStorage.jUnit5ReplayStorage
              .recipeFromUniqueId(uniqueId.toString)
              .map(uniqueId -> reproduceFromRecipe(_))
          )
      )

    val haveReproducedTestCaseForAllReplayedUniqueIds =
      replayedUniqueIds.nonEmpty && casesAvailableForReplayByUniqueId.keys == replayedUniqueIds

    if (haveReproducedTestCaseForAllReplayedUniqueIds) {
      new JavaIterator[DynamicTest] {
        private var oneRelativeInvocationIndex: Integer = 0

        private val inlinedCaseFiltration: InlinedCaseFiltration = (
            runnable: Runnable,
            additionalExceptionsToNoteAsFiltration: Array[
              Class[_ <: Throwable]
            ]
        ) => {
          val inlineFilterRejection = new RuntimeException

          try {
            Trials.throwInlineFilterRejection.withValue(() =>
              throw inlineFilterRejection
            ) { runnable.run() }

            true
          } catch {
            case exception: RuntimeException
                if inlineFilterRejection == exception =>
              false
            case throwable: Throwable
                if additionalExceptionsToNoteAsFiltration.exists(
                  _.isInstance(throwable)
                ) =>
              throw throwable
          }
        }

        private val caseFailureReporting: CaseFailureReporting = throwable =>
          throw throwable

        override def hasNext: Boolean =
          casesAvailableForReplayByUniqueId.nonEmpty

        override def next(): DynamicTest = {
          oneRelativeInvocationIndex += 1

          val details =
            if (1 == casesAvailableForReplayByUniqueId.size)
              casesAvailableForReplayByUniqueId
                .get(casesAvailableForReplayByUniqueId.keys.head)
                .getOrElse("")
            else ""

          dynamicTest(
            s"[$oneRelativeInvocationIndex] ${pprint.PPrinter.BlackWhite(details)}",
            { () =>
              val uniqueId =
                TestExecutionListenerCapturingUniqueIds.uniqueId.toScala

              val potentialReplayedTestCase =
                uniqueId.flatMap(casesAvailableForReplayByUniqueId.get)

              uniqueId.foreach(casesAvailableForReplayByUniqueId.remove)

              potentialReplayedTestCase.foreach(
                invoke(
                  parameterisedTest,
                  _,
                  inlinedCaseFiltration,
                  caseFailureReporting
                )
              )
            }
          )
        }
      }
    } else {
      contexts.zipWithIndex.map { case (context, invocationIndex) =>
        val shrinkagePrefix =
          if (context.isPartOfShrinkage) "Shrinking ... "
          else ""

        val caze                  = context.caze
        val inlinedCaseFiltration = context.inlinedCaseFiltration
        val caseFailureReporting  = context.caseFailureReporting
        val recipe                = context.recipe

        dynamicTest(
          s"[${1 + invocationIndex}] $shrinkagePrefix${pprint.PPrinter.BlackWhite(caze)}",
          { () =>
            TestExecutionListenerCapturingUniqueIds.uniqueId.ifPresent(
              uniqueId =>
                JUnit5ReplayStorage.jUnit5ReplayStorage.recordUniqueId(
                  uniqueId.toString,
                  recipe
                )
            )

            invoke(
              parameterisedTest,
              caze,
              inlinedCaseFiltration,
              caseFailureReporting
            )
          }
        )
      }.asJava
    }
  }
  private def invoke[Case](
      parameterisedTest: Case => Unit,
      caze: Case,
      inlinedCaseFiltration: InlinedCaseFiltration,
      caseFailureReporting: CaseFailureReporting
  ): Unit = {
    val eligible: Boolean =
      try {
        inlinedCaseFiltration
          .executeInFiltrationContext(
            () => parameterisedTest(caze),
            Array(classOf[TestAbortedException])
          )
      } catch {
        case throwable: Throwable =>
          caseFailureReporting.report(throwable)
          throw throwable
      }

    if (!eligible) throw new TestAbortedException
  }
}
