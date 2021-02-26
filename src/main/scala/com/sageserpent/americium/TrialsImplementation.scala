package com.sageserpent.americium

import cats.data.WriterT
import cats.free.FreeT
import cats.free.FreeT.{liftF, pure}
import cats.implicits._
import cats.~>
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom

import _root_.java.lang.{Iterable => JavaIterable}
import _root_.java.util.Optional
import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import scala.collection.JavaConverters._
import scala.util.Random

object TrialsImplementation extends JavaTrialsApi with TrialsApi {
  type DecisionIndices = List[Int]

  type DecisionsWriter[Case] = WriterT[Stream, DecisionIndices, Case]

  type Generation[Case] = FreeT[GenerationOperation, DecisionsWriter, Case]

  // Java and Scala API ...

  override def only[Case](onlyCase: Case): TrialsImplementation[Case] =
    TrialsImplementation(
      pure[GenerationOperation, DecisionsWriter, Case](onlyCase))

  override def choose[Case](firstChoice: Case,
                            secondChoice: Case,
                            otherChoices: Case*): TrialsImplementation[Case] =
    choose(firstChoice +: secondChoice +: otherChoices)

  // Java-only API ...

  override def choose[Case](
      choices: JavaIterable[Case]): TrialsImplementation[Case] =
    choose(choices.asScala)

  override def choose[Case](
      choices: Array[Case with AnyRef]): TrialsImplementation[Case] =
    choose(choices.toSeq)

  override def alternate[Case](
      firstAlternative: JavaTrials[_ <: Case],
      secondAlternative: JavaTrials[_ <: Case],
      otherAlternatives: JavaTrials[_ <: Case]*): TrialsImplementation[Case] =
    alternate(
      (firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))
        .map(_.scalaTrials))

  override def alternate[Case](alternatives: JavaIterable[JavaTrials[Case]])
    : TrialsImplementation[Case] =
    alternate(alternatives.asScala.map(_.scalaTrials))

  override def alternate[Case](
      alternatives: Array[JavaTrials[Case]]): TrialsImplementation[Case] =
    alternate(alternatives.toSeq.map(_.scalaTrials))

  // Scala-only API ...

  override def choose[Case](
      choices: Iterable[Case]): TrialsImplementation[Case] =
    TrialsImplementation(
      liftF[GenerationOperation, DecisionsWriter, Case](Choice(choices)))

  override def alternate[Case](firstAlternative: Trials[Case],
                               secondAlternative: Trials[Case],
                               otherAlternatives: Trials[Case]*): Trials[Case] =
    alternate(
      firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))

  override def alternate[Case](
      alternatives: Iterable[Trials[Case]]): TrialsImplementation[Case] =
    TrialsImplementation(
      liftF[GenerationOperation, DecisionsWriter, Case](
        Alternation(alternatives)))

  sealed trait GenerationOperation[Case]

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

  case class Choice[Case](choices: Iterable[Case])
      extends GenerationOperation[Case]

  case class Alternation[Case](alternatives: Iterable[Trials[Case]])
      extends GenerationOperation[Case]

  // NASTY HACK: as `Free` does not support `filter/withFilter`, reify
  // the optional results of a flat-mapped filtration; the interpreter
  // will deal with these.
  case class FiltrationResult[Case](result: Option[Case])
      extends GenerationOperation[Case]
}

case class TrialsImplementation[+Case](
    override val generation: TrialsImplementation.Generation[_ <: Case])
    extends JavaTrials[Case]
    with Trials[Case] {
  import TrialsImplementation._

  override private[americium] val scalaTrials = this

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    hokeyReproductionOfCase(recipe)._1

  // Java-only API ...
  override def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase])
    : TrialsImplementation[TransformedCase] = map(transform.apply _)

  override def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, JavaTrials[TransformedCase]])
    : TrialsImplementation[TransformedCase] =
    flatMap(step.apply _ andThen (_.scalaTrials))

  override def filter(
      predicate: Predicate[_ >: Case]): TrialsImplementation[Case] =
    filter(predicate.test _)

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[_ >: Case, Optional[TransformedCase]])
    : TrialsImplementation[TransformedCase] =
    mapFilter(filteringTransform.apply _ andThen {
      case withPayload if withPayload.isPresent => Some(withPayload.get())
      case _                                    => None
    })

  override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
    supplyTo(consumer.accept _)

  override def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
    supplyTo(recipe, consumer.accept _)

  // Scala-only API ...
  override def map[TransformedCase](transform: Case => TransformedCase)
    : TrialsImplementation[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase])
    : TrialsImplementation[TransformedCase] =
    flatMap(
      (caze: Case) =>
        TrialsImplementation(
          liftF[GenerationOperation, DecisionsWriter, TransformedCase](
            FiltrationResult(filteringTransform(caze)))))

  override def filter(predicate: Case => Boolean): TrialsImplementation[Case] =
    flatMap(
      (caze: Case) =>
        TrialsImplementation(
          liftF[GenerationOperation, DecisionsWriter, Case](
            FiltrationResult(Some(caze).filter(predicate)))))

  override def flatMap[TransformedCase](step: Case => Trials[TransformedCase])
    : TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def supplyTo(consumer: Case => Unit): Unit =
    cases.zipWithIndex.foreach {
      case (testCase, index) =>
        try {
          consumer(testCase)
        } catch {
          case exception: Throwable =>
            throw new TrialException(exception) {
              override def provokingCase: Case = testCase

              override def recipe: String = index.toString
            }
        }
    }

  override def supplyTo(recipe: String, consumer: Case => Unit): Unit = {
    val (reproducedCase, index) = hokeyReproductionOfCase(recipe)
    try {
      consumer(reproducedCase)
    } catch {
      case exception: Throwable =>
        throw new TrialException(exception) {
          override def provokingCase: Case = reproducedCase

          override def recipe: String = index.toString
        }
    }
  }

  // TODO - its name is accurate. Find a way of a) not having to slog through
  // all the generation of dropped test cases and b) make the recipe something
  // that can deal with changes in representation, maybe?
  private def hokeyReproductionOfCase(recipe: String) = {
    cases.zipWithIndex.drop(recipe.toInt).head
  }

  private def cases: Stream[Case] = {
    val randomBehaviour = new Random(734874)

    // NASTY HACK: what follows is an abuse of the reader monad whereby the injected context is *mutable*,
    // but at least it's buried in the interpreter for `GenerationOperation`. The reified `FiltrationResult`
    // values are also handled by the interpreter too. If it's any consolation, it means that flat-mapping is
    // stack-safe. Read 'em and weep!

    def interpreter: GenerationOperation ~> DecisionsWriter =
      new (GenerationOperation ~> DecisionsWriter) {
        override def apply[Case](
            generationOperation: GenerationOperation[Case]) = {
          generationOperation match {
            case Choice(choices) =>
              WriterT.liftF(randomBehaviour.shuffle(choices).toStream)
            case Alternation(alternatives) =>
              WriterT(
                randomBehaviour.pickAlternatelyFrom(alternatives map (value => {
                  value.generation
                    .foldMap(interpreter)
                    .run // Ahem. Could this be done without recursively interpreting?
                })))
            case FiltrationResult(result) => WriterT.liftF(result.toStream)
          }
        }
      }

    val stuff: Stream[(DecisionIndices, Case)] =
      generation.foldMap(interpreter).run

    stuff.map(_._2)
  }
}
