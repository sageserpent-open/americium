package com.sageserpent.americium.java

import com.sageserpent.americium.{
  CaseSupplyCycle as ScalaCaseSupplyCycle,
  CasesLimitStrategy as ScalaCasesLimitStrategy,
  TestIntegrationContext as ScalaTestIntegrationContext,
  TrialsScaffolding as ScalaTrialsScaffolding
}
import com.sageserpent.americium.java.{
  CaseSupplyCycle as JavaCaseSupplyCycle,
  CasesLimitStrategy as JavaCasesLimitStrategy,
  TrialsScaffolding as JavaTrialsScaffolding
}

import java.util.Iterator as JavaIterator
import java.util.function.Consumer
import scala.jdk.CollectionConverters.*

object TrialsScaffoldingImplementation {
  case class SupplyToSyntaxAdapter[Case](
      scalaSupplyToSyntax: ScalaTrialsScaffolding.SupplyToSyntax[Case]
  ) extends JavaTrialsScaffolding.SupplyToSyntax[Case] {
    override def withSeed(
        seed: Long
    ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
      SupplyToSyntaxAdapter(scalaSupplyToSyntax.withSeed(seed))

    override def withComplexityLimit(
        complexityLimit: Int
    ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
      SupplyToSyntaxAdapter(
        scalaSupplyToSyntax.withComplexityLimit(complexityLimit)
      )

    override def withShrinkageAttemptsLimit(
        shrinkageAttemptsLimit: Int
    ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
      SupplyToSyntaxAdapter(
        scalaSupplyToSyntax.withShrinkageAttemptsLimit(shrinkageAttemptsLimit)
      )

    override def withShrinkageStop(
        shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[_ >: Case]
    ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
      SupplyToSyntaxAdapter(
        scalaSupplyToSyntax.withShrinkageStop({ () =>
          val predicate = shrinkageStop.build()
          predicate.test _
        })
      )

    override def withValidTrialsCheck(
        enabled: Boolean
    ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
      SupplyToSyntaxAdapter(
        scalaSupplyToSyntax.withValidTrialsCheck(enabled)
      )

    override def supplyTo(consumer: Consumer[Case]): Unit =
      scalaSupplyToSyntax.supplyTo(consumer.accept)

    override def asIterator(): JavaIterator[Case] =
      scalaSupplyToSyntax.asIterator().asJava

    override def testIntegrationContexts(): JavaIterator[
      ScalaTestIntegrationContext[Case]
    ] =
      scalaSupplyToSyntax.testIntegrationContexts().asJava

    override def reproduce(recipe: String): Case =
      scalaSupplyToSyntax.reproduce(recipe)
  }
}
