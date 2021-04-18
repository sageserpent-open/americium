package com.sageserpent.americium

import _root_.java.lang.{Double => JavaDouble}
import scala.util.Random

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

  def integers: Trials[Int] = stream(_.hashCode)

  def longs: Trials[Long] = stream(identity)

  def doubles: Trials[Double] =
    for {
      zeroOrPositive <- stream { input =>
        val betweenZeroAndOne = new Random(input).nextDouble()
        Math.scalb(
          betweenZeroAndOne,
          (input.toDouble * JavaDouble.MAX_EXPONENT / Long.MaxValue).toInt
        )
      }
      negative <- trueOrFalse
    } yield if (negative) -zeroOrPositive else zeroOrPositive

  def trueOrFalse: Trials[Boolean] = choose(true, false)

  def coinFlip: Trials[Boolean] = stream(0 == _ % 2)
}
