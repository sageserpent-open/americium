package com.sageserpent.americium

import com.sageserpent.americium.Trials.MutableState

import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import scala.util.Random

case class TrialsImplementation[+Case](
    override val generate: Trials.MutableState => Stream[Case])
    extends Trials[Case] {
  override def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase])
    : Trials[TransformedCase] =
    TrialsImplementation(generate andThen (_ map transform.apply))

  override def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, Trials[TransformedCase]])
    : Trials[TransformedCase] =
    TrialsImplementation { state =>
      // NASTY HACK: essentially this is the reader monad, only using a mutable context. Read 'em and weep!
      for {
        stepInput <- generate(state)
        stepOutput = step(stepInput)
        overallOutput <- stepOutput.generate(state)
      } yield overallOutput
    }

  override def filter(predicate: Predicate[_ >: Case]): Trials[Case] =
    TrialsImplementation(generate andThen (_ filter predicate.test))

  override def supplyTo(consumer: Consumer[_ >: Case]): Unit = {
    val randomBehaviour = new Random(734874)

    generate(MutableState(randomBehaviour)).foreach { testCase =>
      try {
        consumer.accept(testCase)
      } catch {
        case exception: Throwable =>
          throw new TrialException(exception) {
            override def provokingCase: Case = testCase

            override def recipe: String = ???
          }
      }
    }
  }

  override def reproduce(recipe: String): Case = ???
}
