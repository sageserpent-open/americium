package com.sageserpent.americium

import cats.Traverse

import _root_.java.time.Instant
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

  def chooseWithWeights[Case](
      firstChoice: (Int, Case),
      secondChoice: (Int, Case),
      otherChoices: (Int, Case)*
  ): Trials[Case]

  def chooseWithWeights[Case](choices: Iterable[(Int, Case)]): Trials[Case]

  def alternate[Case](
      firstAlternative: Trials[Case],
      secondAlternative: Trials[Case],
      otherAlternatives: Trials[Case]*
  ): Trials[Case]

  def alternate[Case](alternatives: Iterable[Trials[Case]]): Trials[Case]

  def sequences[Case, Sequence[_]: Traverse](
      sequenceOfTrials: Sequence[Trials[Case]]
  )(implicit
      factory: Factory[Case, Sequence[Case]]
  ): Trials[Sequence[Case]]

  def stream[Case](factory: Long => Case): Trials[Case]

  def integers: Trials[Int]

  def longs: Trials[Long]

  def doubles: Trials[Double]

  def booleans: Trials[Boolean]

  def characters: Trials[Char]

  def instants: Trials[Instant]

  def strings: Trials[String]
}
