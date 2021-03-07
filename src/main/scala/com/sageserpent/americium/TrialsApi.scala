package com.sageserpent.americium

import _root_.java.lang.{Double => JavaDouble}

trait TrialsApi {
  def only[Case](onlyCase: Case): Trials[Case]

  def choose[Case](firstChoice: Case,
                   secondChoice: Case,
                   otherChoices: Case*): Trials[Case]

  def choose[Case](choices: Iterable[Case]): Trials[Case]

  def alternate[Case](firstAlternative: Trials[Case],
                      secondAlternative: Trials[Case],
                      otherAlternatives: Trials[Case]*): Trials[Case]

  def alternate[Case](alternatives: Iterable[Trials[Case]]): Trials[Case]

  def stream[Case](factory: Long => Case): Trials[Case]

  def scalaIntegers: Trials[Integer] = stream(_.hashCode)

  def scalaLongs: Trials[Long] = stream(identity)

  def scalaDoubles: Trials[Double] =
    stream(JavaDouble.longBitsToDouble)

  def scalaTrueOrFalse: Trials[Boolean] = choose(true, false)

  def scalaCoinFlip: Trials[Boolean] = stream(0 == _ % 2)
}
