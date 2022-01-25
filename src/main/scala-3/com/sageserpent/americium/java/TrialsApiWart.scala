package com.sageserpent.americium.java
import com.sageserpent.americium.java.{
  Trials as JavaTrials,
  TrialsApi as JavaTrialsApi
}
import com.sageserpent.americium.{
  TrialsImplementation,
  TrialsApiImplementation as ScalaTrialsApiImplementation
}

import scala.annotation.varargs

trait TrialsApiWart extends JavaTrialsApi {
  def scalaApi: ScalaTrialsApiImplementation

  @varargs
  def choose[Case <: AnyRef](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): TrialsImplementation[Case] =
    scalaApi.choose(firstChoice +: secondChoice +: otherChoices)

  def choose[Case <: AnyRef](
      choices: Array[Case]
  ): TrialsImplementation[Case] = scalaApi.choose(choices.toSeq)
}
