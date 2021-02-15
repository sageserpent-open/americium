package com.sageserpent.americium

import cats.free.Free.liftF
import cats.~>
import com.sageserpent.americium.java.Trials._
import com.sageserpent.americium.randomEnrichment.RichRandom

import scala.util.Random

case class TrialsImplementation[+Case](
    override val generation: Generation[_ <: Case])
    extends Trials[Case] {
  override def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def filter(predicate: Case => Boolean): Trials[Case] =
    flatMap(
      (caze: Case) =>
        TrialsImplementation(
          liftF(FiltrationResult(Some(caze).filter(predicate)))))

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase])
    : Trials[TransformedCase] =
    flatMap((caze: Case) =>
      TrialsImplementation(liftF(FiltrationResult(filteringTransform(caze)))))

  override def supplyTo(consumer: Case => Unit): Unit = {
    val randomBehaviour = new Random(734874)

    // NASTY HACK: what follows is an abuse of the reader monad whereby the injected context is *mutable*,
    // but at least it's buried in the interpreter for `Trials.FiltrationResult`. The reified `Filtration` values
    // are also handled by the interpreter too. If it's any consolation, it means that flat-mapping is
    // stack-safe. Read 'em and weep!

    def interpreter: GenerationOperation ~> Stream =
      new (GenerationOperation ~> Stream) {
        override def apply[Case](
            generationOperation: GenerationOperation[Case]): Stream[Case] = {
          generationOperation match {
            case Choice(choices) => randomBehaviour.shuffle(choices).toStream
            case Alternation(alternatives) =>
              randomBehaviour.pickAlternatelyFrom(
                alternatives map (_.generation.foldMap(interpreter)))
            case FiltrationResult(result) => result.toStream
          }
        }
      }

    generation.foldMap(interpreter).foreach { testCase =>
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
