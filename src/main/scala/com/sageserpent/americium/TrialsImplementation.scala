package com.sageserpent.americium

import cats.data.{State, WriterT}
import cats.free.FreeT
import cats.free.FreeT.{liftF, pure}
import cats.implicits._
import cats.~>
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import io.circe.parser._
import io.circe.syntax._

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
    new TrialsImplementation(Choice(choices))

  override def alternate[Case](firstAlternative: Trials[Case],
                               secondAlternative: Trials[Case],
                               otherAlternatives: Trials[Case]*): Trials[Case] =
    alternate(
      firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))

  override def alternate[Case](
      alternatives: Iterable[Trials[Case]]): TrialsImplementation[Case] =
    new TrialsImplementation(Alternation(alternatives))

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

  def this(
      generationOperation: TrialsImplementation.GenerationOperation[Case]) = {
    this(
      liftF[TrialsImplementation.GenerationOperation,
            TrialsImplementation.DecisionsWriter,
            Case](generationOperation))
  }

  override private[americium] val scalaTrials = this

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    incomprehensibleVoodooReproductionOfCase(recipe)._2

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
    flatMap((caze: Case) =>
      new TrialsImplementation(FiltrationResult(filteringTransform(caze))))

  override def filter(predicate: Case => Boolean): TrialsImplementation[Case] =
    flatMap((caze: Case) =>
      new TrialsImplementation(FiltrationResult(Some(caze).filter(predicate))))

  override def flatMap[TransformedCase](step: Case => Trials[TransformedCase])
    : TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def supplyTo(consumer: Case => Unit): Unit =
    cases.foreach {
      case (decisionIndices, testCase) =>
        try {
          consumer(testCase)
        } catch {
          case exception: Throwable =>
            throw new TrialException(exception) {
              override def provokingCase: Case = testCase

              override def recipe: String = decisionIndices.asJson.spaces4
            }
        }
    }

  override def supplyTo(recipe: String, consumer: Case => Unit): Unit = {
    val (decisionIndices, reproducedCase) =
      incomprehensibleVoodooReproductionOfCase(recipe)

    try {
      consumer(reproducedCase)
    } catch {
      case exception: Throwable =>
        throw new TrialException(exception) {
          override def provokingCase: Case = reproducedCase

          override def recipe: String = decisionIndices.asJson.spaces4
        }
    }
  }

  // TODO - its name is accurate.
  private def incomprehensibleVoodooReproductionOfCase(
      recipe: String): (DecisionIndices, Case) = {
    val decisionIndices: DecisionIndices =
      decode[DecisionIndices](recipe).right.get // TODO: what could possibly go wrong?

    println(decisionIndices) // TODO - remove this debugging cruft...

    type Frankenstein[Caze] = State[DecisionIndices, Caze] // TODO - name, please...

    def jolt: DecisionsWriter ~> Frankenstein =
      new (DecisionsWriter ~> Frankenstein) {
        override def apply[A](
            decisionsWriter: DecisionsWriter[A]): Frankenstein[A] =
          decisionsWriter.run.head._2.pure[Frankenstein]
      }

    def interpreter: GenerationOperation ~> Frankenstein =
      new (GenerationOperation ~> Frankenstein) {
        override def apply[Case](generationOperation: GenerationOperation[Case])
          : Frankenstein[Case] = {
          generationOperation match {
            case Choice(choices) =>
              for {
                decisionIndices <- State.get[DecisionIndices]
                decisionIndex :: remainingDecisionIndices = decisionIndices
                _ <- State.set(remainingDecisionIndices)
              } yield choices.drop(decisionIndex).head

            case Alternation(alternatives) =>
              for {
                decisionIndices <- State.get[DecisionIndices]
                decisionIndex :: remainingDecisionIndices = decisionIndices
                _ <- State.set(remainingDecisionIndices)
                result <- {
                  val chosenAlternative = alternatives.drop(decisionIndex).head
                  chosenAlternative.generation.mapK(jolt).foldMap(interpreter)
                }
              } yield result

            case FiltrationResult(result) => result.get.pure[Frankenstein]
          }
        }
      }

    decisionIndices -> generation
      .mapK(jolt)
      .foldMap(interpreter)
      .runA(decisionIndices)
      .value
  }

  private def cases: Stream[(DecisionIndices, Case)] = {
    val randomBehaviour = new Random(734874)

    // NASTY HACK: what follows is an abuse of the reader monad whereby the injected context is *mutable*,
    // but at least it's buried in the interpreter for `GenerationOperation`. The reified `FiltrationResult`
    // values are also handled by the interpreter too. If it's any consolation, it means that flat-mapping is
    // stack-safe. Read 'em and weep!

    def interpreter: GenerationOperation ~> DecisionsWriter =
      new (GenerationOperation ~> DecisionsWriter) {
        override def apply[Case](generationOperation: GenerationOperation[Case])
          : WriterT[Stream, DecisionIndices, Case] = {
          generationOperation match {
            case Choice(choices) =>
              WriterT(
                randomBehaviour
                  .shuffle(choices.zipWithIndex)
                  .map {
                    case (caze, decisionIndex) => List(decisionIndex) -> caze
                  }
                  .toStream
              )
            case Alternation(alternatives) =>
              WriterT(
                randomBehaviour
                  .pickAlternatelyFrom(alternatives.zipWithIndex
                    .map {
                      case (value, decisionIndex) =>
                        value.generation
                          .foldMap(interpreter)
                          .run // Ahem. Could this be done without recursively interpreting?
                          .map {
                            case (decisionIndices, caze) =>
                              (decisionIndex :: decisionIndices) -> caze
                          }
                    }))
            case FiltrationResult(result) => WriterT.liftF(result.toStream)
          }
        }
      }

    generation.foldMap(interpreter).run
  }
}
