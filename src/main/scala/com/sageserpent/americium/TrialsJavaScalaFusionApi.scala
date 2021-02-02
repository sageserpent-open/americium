package com.sageserpent.americium

import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom

import _root_.java.lang.{Iterable => JavaIterable}
import scala.collection.JavaConverters._

trait TrialsJavaScalaFusionApi extends JavaTrialsApi {
  // Java API ...

  override def only[SomeCase](onlyCase: SomeCase): Trials[SomeCase] =
    TrialsImplementation(_ => Stream(onlyCase))

  override def choose[SomeCase](firstChoice: SomeCase,
                                secondChoice: SomeCase,
                                otherChoices: SomeCase*): Trials[SomeCase] =
    choose(firstChoice +: secondChoice +: otherChoices)

  override def choose[SomeCase](
      choices: JavaIterable[SomeCase]): Trials[SomeCase] =
    choose(choices.asScala)

  override def choose[SomeCase](
      choices: Array[SomeCase with AnyRef]): Trials[SomeCase] =
    choose(choices.toSeq)

  override def alternate[SomeCase](
      firstAlternative: JavaTrials[_ <: SomeCase],
      secondAlternative: JavaTrials[_ <: SomeCase],
      otherAlternatives: JavaTrials[_ <: SomeCase]*): Trials[SomeCase] =
    alternate(
      firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))

  override def alternate[SomeCase](
      alternatives: JavaIterable[JavaTrials[SomeCase]]): Trials[SomeCase] =
    alternate(alternatives.asScala)

  override def alternate[SomeCase](
      alternatives: Array[JavaTrials[SomeCase]]): Trials[SomeCase] =
    alternate(alternatives.toSeq)

  // Scala-only API ...
  def choose[SomeCase](choices: Iterable[SomeCase]): Trials[SomeCase] =
    TrialsImplementation(_.randomBehaviour.shuffle(choices).toStream)

  def alternate[SomeCase](
      alternatives: Iterable[JavaTrials[SomeCase]]): Trials[SomeCase] =
    TrialsImplementation { mutableState =>
      mutableState.randomBehaviour.pickAlternatelyFrom(
        alternatives map (_.generate(mutableState)))
    }
}
