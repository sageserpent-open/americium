package com.sageserpent.americium
import com.sageserpent.americium.java.TestIntegrationContext
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.opentest4j.TestAbortedException

import _root_.java.util.Iterator as JavaIterator
import scala.jdk.CollectionConverters.IteratorHasAsJava

package object junit5 {
  implicit class Syntax[Case](
      private val supplier: TrialsScaffolding.SupplyToSyntax[Case]
  ) extends AnyVal {

    /** Provide dynamic tests for JUnit5, allowing direct coupling between a
      * [[TrialsScaffolding.SupplyToSyntax]] and the parameterised test
      * consuming the supplied test cases. <p> In contrast to the 'classic'
      * approach taken by [[com.sageserpent.americium.java.junit5.TrialsTest]]
      * and [[com.sageserpent.americium.java.junit5.ConfiguredTrialsTest]], this
      * affords strong type-checking between the test's arguments and the
      * supplier. <p> It does however sacrifice the potential use of
      * [[org.junit.jupiter.api.BeforeEach]] and
      * [[org.junit.jupiter.api.AfterEach]] to perform set-up and tear-down for
      * each trial - that has to be coded explicitly in the parameterised test
      * itself. <p> Example:
      * {{{
      *   import java.util.{Iterator => JavaIterator}
      *   import com.sageserpent.americium.junit5.Syntax
      * }}}
      * {{{
      *   @TestFactory
      *   def dynamicTestsExample(): JavaIterator[DynamicTest] = {
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
      *   Parameterised test that consumes a test case of type {@code Case}.
      * @return
      *   An iterator of [[DynamicTest]] instances, suitable for use with the
      *   [[org.junit.jupiter.api.TestFactory]] annotation provided by JUnit5.
      */
    def dynamicTests(
        parameterisedTest: Case => Unit
    ): JavaIterator[DynamicTest] =
      junit5.dynamicTests(supplier.testIntegrationContexts(), parameterisedTest)
  }
  private[americium] def dynamicTests[Case](
      contexts: Iterator[TestIntegrationContext[Case]],
      parameterisedTest: Case => Unit
  ): JavaIterator[DynamicTest] = {
    contexts.zipWithIndex.map { case (context, invocationIndex) =>
      val shrinkagePrefix =
        if (context.isPartOfShrinkage) "Shrinking ... "
        else ""
      dynamicTest(
        s"[${1 + invocationIndex}] $shrinkagePrefix${pprint.PPrinter.BlackWhite(context.caze)}",
        { () =>
          val eligible =
            try {
              context.inlinedCaseFiltration
                .executeInFiltrationContext(
                  () => parameterisedTest(context.caze),
                  Array(classOf[TestAbortedException])
                )
            } catch {
              case throwable: Throwable =>
                context.caseFailureReporting.report(throwable)
                throw throwable
            }

          if (!eligible) throw new TestAbortedException
        }
      )
    }.asJava
  }
}
