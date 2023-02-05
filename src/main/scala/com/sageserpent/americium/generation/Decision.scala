package com.sageserpent.americium.generation

sealed trait Decision
object Decision {
  type DecisionStages = List[Decision]
}

case class ChoiceOf(index: Int) extends Decision

case class FactoryInputOf(input: Long) extends Decision
