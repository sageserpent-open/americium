package com.sageserpent.americium.generation
import com.google.common.hash.Hashing as GuavaHashing
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

sealed trait Decision

object Decision {
  type DecisionStages = List[Decision]

  def parseRecipe(recipe: String): DecisionStages = {
    decode[DecisionStages](
      recipe
    ).toTry.get // Just throw the exception, the callers are written in Java style.
  }

  def shorthandRecipe(decisionStages: DecisionStages): String =
    decisionStages.asJson.noSpaces

  def recipeHash(decisionStages: DecisionStages): String =
    GuavaHashing
      .murmur3_128()
      .hashUnencodedChars(longhandRecipe(decisionStages))
      .toString

  def longhandRecipe(decisionStages: DecisionStages): String =
    decisionStages.asJson.spaces4
}

case class ChoiceOf(index: Int) extends Decision

case class FactoryInputOf(input: BigInt) extends Decision
