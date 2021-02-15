package com.sageserpent.americium

import cats.free.Free.{liftF, pure}
import com.sageserpent.americium.java.Trials.{Alternation, Choice}
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}

import _root_.java.lang.{Iterable => JavaIterable}
import scala.collection.JavaConverters._

trait TrialsJavaScalaFusionApi extends JavaTrialsApi {
  // Java API ...

  override def only[Case](onlyCase: Case): Trials[Case] =
    TrialsImplementation(pure(onlyCase))

  override def choose[Case](firstChoice: Case,
                            secondChoice: Case,
                            otherChoices: Case*): Trials[Case] =
    choose(firstChoice +: secondChoice +: otherChoices)

  override def choose[Case](choices: JavaIterable[Case]): Trials[Case] =
    choose(choices.asScala)

  override def choose[Case](choices: Array[Case with AnyRef]): Trials[Case] =
    choose(choices.toSeq)

  override def alternate[Case](
      firstAlternative: JavaTrials[_ <: Case],
      secondAlternative: JavaTrials[_ <: Case],
      otherAlternatives: JavaTrials[_ <: Case]*): Trials[Case] =
    alternate(
      firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))

  override def alternate[Case](
      alternatives: JavaIterable[JavaTrials[Case]]): Trials[Case] =
    alternate(alternatives.asScala)

  override def alternate[Case](
      alternatives: Array[JavaTrials[Case]]): Trials[Case] =
    alternate(alternatives.toSeq)

  // Scala-only API ...
  def choose[Case](choices: Iterable[Case]): Trials[Case] =
    TrialsImplementation(liftF(Choice(choices)))

  def alternate[Case](alternatives: Iterable[JavaTrials[Case]]): Trials[Case] =
    TrialsImplementation(liftF(Alternation(alternatives)))
}
