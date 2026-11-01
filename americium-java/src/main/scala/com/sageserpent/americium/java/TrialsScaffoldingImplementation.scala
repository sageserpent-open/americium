package com.sageserpent.americium.java

import com.sageserpent.americium.java.TrialsScaffolding as JavaTrialsScaffolding
import com.sageserpent.americium.{
  TestIntegrationContext as ScalaTestIntegrationContext,
  TrialsScaffolding as ScalaTrialsScaffolding
}

import java.util.Iterator as JavaIterator
import java.util.function.Consumer

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
      try {
        scalaSupplyToSyntax.supplyTo(consumer.accept)
      } catch {
        case _: com.sageserpent.americium.NoValidTrialsException =>
          throw new com.sageserpent.americium.java.NoValidTrialsException
      }

    override def asIterator(): JavaIterator[Case] = {
      val underlying = scalaSupplyToSyntax.asIterator()
      new JavaIterator[Case] {
        override def hasNext: Boolean =
          try {
            underlying.hasNext
          } catch {
            case _: com.sageserpent.americium.NoValidTrialsException =>
              throw new com.sageserpent.americium.java.NoValidTrialsException
          }

        override def next(): Case =
          try {
            underlying.next()
          } catch {
            case _: com.sageserpent.americium.NoValidTrialsException =>
              throw new com.sageserpent.americium.java.NoValidTrialsException
          }
      }
    }

    override def testIntegrationContexts(): JavaIterator[
      ScalaTestIntegrationContext[Case]
    ] = {
      val underlying = scalaSupplyToSyntax.testIntegrationContexts()
      new JavaIterator[ScalaTestIntegrationContext[Case]] {
        override def hasNext: Boolean =
          try {
            underlying.hasNext
          } catch {
            case _: com.sageserpent.americium.NoValidTrialsException =>
              throw new com.sageserpent.americium.java.NoValidTrialsException
          }

        override def next(): ScalaTestIntegrationContext[Case] =
          try {
            underlying.next()
          } catch {
            case _: com.sageserpent.americium.NoValidTrialsException =>
              throw new com.sageserpent.americium.java.NoValidTrialsException
          }
      }
    }

    override def reproduce(recipe: String): Case =
      scalaSupplyToSyntax.reproduce(recipe)
  }
}
