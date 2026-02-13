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

  def json(decisionStages: DecisionStages): String =
    decisionStages.asJson.spaces4
  def compressedJson(decisionStages: DecisionStages): String =
    decisionStages.asJson.noSpaces

  def jsonHashInHexadecimal(decisionStages: DecisionStages): String =
    GuavaHashing
      .murmur3_128()
      .hashUnencodedChars(json(decisionStages))
      .toString
}

case class ChoiceOf(index: Int) extends Decision

case class FactoryInputOf(input: BigInt) extends Decision
