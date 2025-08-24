package com.sageserpent.americium.generation
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

sealed trait Decision

object Decision {
  type DecisionStages = List[Decision]

  def parseDecisionIndices(recipe: String): DecisionStages = {
    decode[DecisionStages](
      recipe
    ).toTry.get // Just throw the exception, the callers are written in Java style.
  }

  def json(decisionStages: DecisionStages) = decisionStages.asJson.spaces4
  def compressedJson(decisionStages: DecisionStages) =
    decisionStages.asJson.noSpaces
}

case class ChoiceOf(index: Int) extends Decision

case class FactoryInputOf(input: BigInt) extends Decision
