package com.sageserpent.americium.java
import com.sageserpent.americium.java.TrialsApi as JavaTrialsApi
import com.sageserpent.americium.{
  TrialsImplementation,
  TrialsApiImplementation as ScalaTrialsApiImplementation
}

trait TrialsApiWart extends JavaTrialsApi {
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
