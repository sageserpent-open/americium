package com.sageserpent.americium

import com.sageserpent.americium.Trials.MutableState

import scala.util.Random

case class TrialsImplementation[+Case](
    override val generate: Trials.MutableState => Stream[Case])
    extends Trials[Case] {
  override def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase] =
    TrialsImplementation(generate andThen (_ map transform))

  override def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] =
    TrialsImplementation { state =>
      // NASTY HACK: essentially this is the reader monad, only using a mutable context. Read 'em and weep!
      for {
        stepInput <- generate(state)
        stepOutput = step(stepInput)
        overallOutput <- stepOutput.generate(state)
      } yield overallOutput
    }

  override def filter(predicate: Case => Boolean): Trials[Case] =
    TrialsImplementation(generate andThen (_ filter predicate))

  override def supplyTo(consumer: Case => Unit): Unit = {
    val randomBehaviour = new Random(734874)

    generate(MutableState(randomBehaviour)).foreach { testCase =>
      try {
        consumer(testCase)
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
