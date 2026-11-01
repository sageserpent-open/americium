package com.sageserpent.americium.java

import com.sageserpent.americium.Trials as ScalaTrials
import com.sageserpent.americium.java.{
  Trials as JavaTrials,
  TrialsScaffolding as JavaTrialsScaffolding
}

case class TrialsImplementation[Case](
    override val scalaTrials: ScalaTrials[Case]
) extends TrialsSkeletalImplementation[Case] {

  override def trials: JavaTrials[Case] = this

  override def withLimit(
      limit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
    TrialsScaffoldingImplementation.SupplyToSyntaxAdapter(
      scalaTrials.withLimit(limit)
    )

  override def withStrategy(
      casesLimitStrategyFactory: java.util.function.Function[
        CaseSupplyCycle,
        CasesLimitStrategy
      ]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
    TrialsScaffoldingImplementation.SupplyToSyntaxAdapter(
      scalaTrials.withStrategy(cycle =>
        casesLimitStrategyFactory.apply(new CaseSupplyCycle {
          override def numberOfPreviousCycles(): Int =
            cycle.numberOfPreviousCycles()
          override def numberOfPreviousFailures(): Int =
            cycle.numberOfPreviousFailures()
        })
      )
    )

  override def withRecipe(
      recipe: String
  ): JavaTrialsScaffolding.SupplyToSyntax[Case] =
    TrialsScaffoldingImplementation.SupplyToSyntaxAdapter(
      scalaTrials.withRecipe(recipe)
    )

  override def reproduce(recipe: String): Case = scalaTrials.reproduce(recipe)
}
