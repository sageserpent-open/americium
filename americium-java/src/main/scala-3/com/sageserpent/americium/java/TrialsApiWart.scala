package com.sageserpent.americium.java
import com.sageserpent.americium.java.Trials as JavaTrials
import com.sageserpent.americium.java.TrialsApi as JavaTrialsApi
import com.sageserpent.americium.TrialsApiImplementation as ScalaTrialsApiImplementation

import scala.annotation.varargs

trait TrialsApiWart extends JavaTrialsApi {
  def scalaApi: ScalaTrialsApiImplementation

  @varargs
  def choose[Case <: AnyRef](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): JavaTrials[Case] =
    TrialsImplementation(scalaApi.choose(firstChoice +: secondChoice +: otherChoices))

  def choose[Case <: AnyRef](
      choices: Array[Case]
  ): JavaTrials[Case] = TrialsImplementation(scalaApi.choose(choices.toSeq))
}
