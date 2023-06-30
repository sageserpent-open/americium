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
