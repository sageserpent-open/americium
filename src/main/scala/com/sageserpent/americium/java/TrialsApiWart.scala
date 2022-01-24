package com.sageserpent.americium.java
import com.sageserpent.americium.{
  TrialsImplementation,
  TrialsApiImplementation as ScalaTrialsApiImplementation
}

trait TrialsApiWart {
  def scalaApi: ScalaTrialsApiImplementation

  def choose[Case](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): TrialsImplementation[Case] =
    scalaApi.choose(firstChoice +: secondChoice +: otherChoices)

  def choose[Case](
      choices: Array[Case with AnyRef]
  ): TrialsImplementation[Case] = scalaApi.choose(choices.toSeq)
}
