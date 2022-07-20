package com.sageserpent.americium

import com.sageserpent.americium.TrialsScaffolding.{ShrinkageStop, noStopping}
import com.sageserpent.americium.java.TrialsDefaults.{
  defaultComplexityLimit,
  defaultShrinkageAttemptsLimit
}
import com.sageserpent.americium.java.{
  CaseSupplyCycle,
  CasesLimitStrategy,
  TrialsFactoring
}

import _root_.java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.DurationConverters.ScalaDurationOps

object TrialsScaffolding {
  type ShrinkageStop[-Case] = () => Case => Boolean

  val noStopping: ShrinkageStop[Any] = () => _ => false

  val noShrinking: ShrinkageStop[Any] = () => _ => true

  def timer(timeLimit: Duration): ShrinkageStop[Any] = () => {
    val whenStarted = Instant.now()

    _ =>
      timeLimit match {
        case finiteDuration: FiniteDuration =>
          !Instant.now().isBefore(whenStarted.plus(finiteDuration.toJava))
        case _ => false
      }
  }

  trait SupplyToSyntax[+Case] {
    def supplyTo(consumer: Case => Unit): Unit
  }

  trait SupplyToSyntaxTuple2[+Case1, +Case2]
      extends SupplyToSyntax[(Case1, Case2)] {
    def supplyTo(consumer: (Case1, Case2) => Unit): Unit
  }

  trait SupplyToSyntaxTuple3[+Case1, +Case2, +Case3]
      extends SupplyToSyntax[(Case1, Case2, Case3)] {
    def supplyTo(consumer: (Case1, Case2, Case3) => Unit): Unit
  }

  trait SupplyToSyntaxTuple4[+Case1, +Case2, +Case3, +Case4]
      extends SupplyToSyntax[(Case1, Case2, Case3, Case4)] {
    def supplyTo(consumer: (Case1, Case2, Case3, Case4) => Unit): Unit
  }

  trait Tuple2Trials[+Case1, +Case2] extends TrialsScaffolding[(Case1, Case2)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple2[Case1, Case2]

    def and[Case3](
        thirdTrials: Trials[Case3]
    ): Tuple3Trials[Case1, Case2, Case3]
  }

  trait Tuple3Trials[+Case1, +Case2, +Case3]
      extends TrialsScaffolding[(Case1, Case2, Case3)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple3[Case1, Case2, Case3]

    def and[Case4](
        fourthTrials: Trials[Case4]
    ): Tuple4Trials[Case1, Case2, Case3, Case4]
  }

  trait Tuple4Trials[+Case1, +Case2, +Case3, +Case4]
      extends TrialsScaffolding[(Case1, Case2, Case3, Case4)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple4[
      Case1,
      Case2,
      Case3,
      Case4
    ]
  }
}

trait TrialsScaffolding[+Case] extends TrialsFactoring[Case] {
  type SupplySyntaxType <: TrialsScaffolding.SupplyToSyntax[Case]

  def trials: Trials[Case]

  def withLimit(limit: Int): SupplySyntaxType

  @deprecated(
    "Use `withLimits` instead."
  )
  def withLimit(limit: Int, complexityLimit: Int): SupplySyntaxType

  def withLimits(
      casesLimit: Int,
      complexityLimit: Int = defaultComplexityLimit,
      shrinkageAttemptsLimit: Int = defaultShrinkageAttemptsLimit,
      shrinkageStop: ShrinkageStop[Case] = noStopping
  ): SupplySyntaxType

  def withStrategy(
      casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy,
      complexityLimit: Int = defaultComplexityLimit,
      shrinkageAttemptsLimit: Int = defaultShrinkageAttemptsLimit,
      shrinkageStop: ShrinkageStop[Case] = noStopping
  ): SupplySyntaxType

  def withRecipe(recipe: String): SupplySyntaxType
}
