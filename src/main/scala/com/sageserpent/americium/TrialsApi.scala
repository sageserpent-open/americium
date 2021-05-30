package com.sageserpent.americium

import scala.collection.Factory

trait TrialsApi {
  def delay[Case](delayed: => Trials[Case]): Trials[Case]

  def only[Case](onlyCase: Case): Trials[Case]

  def choose[Case](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): Trials[Case]

  def choose[Case](choices: Iterable[Case]): Trials[Case]

  def alternate[Case](
      firstAlternative: Trials[Case],
      secondAlternative: Trials[Case],
      otherAlternatives: Trials[Case]*
  ): Trials[Case]

  def alternate[Case](alternatives: Iterable[Trials[Case]]): Trials[Case]

  def stream[Case](factory: Long => Case): Trials[Case]

  def integers: Trials[Int]

  def longs: Trials[Long]

  def doubles: Trials[Double]

  def booleans: Trials[Boolean]

  def several[Container, ItemCase](
      items: Trials[ItemCase]
  )(implicit
      factory: Factory[ItemCase, Container]
  ): Trials[Container]
}
