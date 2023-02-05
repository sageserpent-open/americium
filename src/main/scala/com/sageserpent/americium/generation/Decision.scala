package com.sageserpent.americium.generation
import io.circe.generic.auto.*
import io.circe.parser.decode

sealed trait Decision

object Decision {
  type DecisionStages = List[Decision]
  def parseDecisionIndices(recipe: String): DecisionStages = {
    decode[DecisionStages](
      recipe
    ).toTry.get // Just throw the exception, the callers are written in Java style.
  }
}

case class ChoiceOf(index: Int) extends Decision

case class FactoryInputOf(input: Long) extends Decision
