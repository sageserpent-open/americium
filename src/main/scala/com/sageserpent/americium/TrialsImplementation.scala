package com.sageserpent.americium

import cats.data.{State, WriterT}
import cats.free.Free
import cats.free.Free.{liftF, pure}
import cats.implicits._
import cats.~>
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import _root_.java.lang.{Iterable => JavaIterable, Long => JavaLong}
import _root_.java.util.Optional
import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import scala.collection.JavaConverters._
import scala.util.Random

object TrialsImplementation extends JavaTrialsApi with TrialsApi {
  sealed trait Decision;

  case class ChoiceOf(index: Int)        extends Decision
  case class AlternativeOf(index: Int)   extends Decision
  case class FactoryInputOf(input: Long) extends Decision

  type DecisionIndices = List[Decision]

  type Generation[Case] = Free[GenerationOperation, Case]

  // Java and Scala API ...

  override def only[Case](onlyCase: Case): TrialsImplementation[Case] =
    TrialsImplementation(pure[GenerationOperation, Case](onlyCase))

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

  override def stream[Case](
      factory: JavaFunction[JavaLong, Case]): TrialsImplementation[Case] =
    stream(Long.box _ andThen factory.apply)

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

  override def stream[Case](factory: Long => Case): TrialsImplementation[Case] =
    new TrialsImplementation(Factory(factory))

  sealed trait GenerationOperation[Case]

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

  case class Choice[Case](choices: Iterable[Case])
      extends GenerationOperation[Case]

  case class Alternation[Case](alternatives: Iterable[Trials[Case]])
      extends GenerationOperation[Case]

  case class Factory[Case](factory: Long => Case)
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
      liftF[TrialsImplementation.GenerationOperation, Case](
        generationOperation))
  }

  override private[americium] val scalaTrials = this

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    reproduce(parseDecisionIndices(recipe))

  override def withLimit(
      limit: Int): JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] =
    new JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] {

      // Java-only API ...
      override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
        supplyTo(consumer.accept _)

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit =
        cases.take(limit).foreach {
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
    }

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

  override def supplyTo(recipe: String, consumer: Case => Unit): Unit = {
    val decisionIndices = parseDecisionIndices(recipe)
    val reproducedCase  = reproduce(decisionIndices)

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

  private def reproduce(decisionIndices: DecisionIndices): Case = {

    type DecisionIndicesContext[Caze] = State[DecisionIndices, Caze]

    // NOTE: unlike the companion interpreter in `cases`,
    // this one has a relatively sane implementation.
    def interpreter: GenerationOperation ~> DecisionIndicesContext =
      new (GenerationOperation ~> DecisionIndicesContext) {
        override def apply[Case](generationOperation: GenerationOperation[Case])
          : DecisionIndicesContext[Case] = {
          generationOperation match {
            case Choice(choices) =>
              for {
                decisionIndices <- State.get[DecisionIndices]
                ChoiceOf(decisionIndex) :: remainingDecisionIndices = decisionIndices
                _ <- State.set(remainingDecisionIndices)
              } yield choices.drop(decisionIndex).head

            case Alternation(alternatives) =>
              for {
                decisionIndices <- State.get[DecisionIndices]
                AlternativeOf(decisionIndex) :: remainingDecisionIndices = decisionIndices
                _ <- State.set(remainingDecisionIndices)
                result <- {
                  val chosenAlternative = alternatives.drop(decisionIndex).head
                  chosenAlternative.generation.foldMap(interpreter) // Ahem. Could this be done without recursively interpreting?
                }
              } yield result

            case Factory(factory) =>
              for {
                decisionIndices <- State.get[DecisionIndices]
                FactoryInputOf(input) :: remainingDecisionIndices = decisionIndices
                _ <- State.set(remainingDecisionIndices)
              } yield factory(input)

            // NOTE: pattern-match only on `Some`, as we are reproducing a caze that
            // therefore must have passed filtration the first time around.
            case FiltrationResult(Some(caze)) =>
              caze.pure[DecisionIndicesContext]
          }
        }
      }

    generation
      .foldMap(interpreter)
      .runA(decisionIndices)
      .value
  }

  private def parseDecisionIndices(recipe: String): DecisionIndices = {
    decode[DecisionIndices](recipe).right.get // TODO: what could possibly go wrong?
  }

  private def cases: Stream[(DecisionIndices, Case)] = {
    val randomBehaviour = new Random(734874)

    type DecisionsWriter[Case] = WriterT[Stream, DecisionIndices, Case]

    // NASTY HACK: what follows is a hacked alternative to using the reader monad whereby the injected
    // context is *mutable*, but at least it's buried in the interpreter for `GenerationOperation`, expressed
    // as a closure over `randomBehaviour`. The reified `FiltrationResult` values are also handled by the
    // interpreter too. If it's any consolation, it means that flat-mapping is stack-safe - although I'm not
    // entirely sure about alternation. Read 'em and weep!

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
                    case (caze, decisionIndex) =>
                      List(ChoiceOf(decisionIndex)) -> caze
                  }
                  .toStream
              )

            case Alternation(alternatives) =>
              WriterT(randomBehaviour
                .pickAlternatelyFrom(alternatives.zipWithIndex
                  .map {
                    case (value, decisionIndex) =>
                      value.generation
                        .foldMap(interpreter)
                        .run // Ahem. Could this be done without recursively interpreting?
                        .map {
                          case (decisionIndices, caze) =>
                            (AlternativeOf(decisionIndex) :: decisionIndices) -> caze
                        }
                  }))

            case Factory(factory) =>
              WriterT(
                Stream.continually {
                  val input = randomBehaviour.nextLong()

                  List(FactoryInputOf(input)) -> factory(input)
                }
              )

            case FiltrationResult(result) => WriterT.liftF(result.toStream)
          }
        }
      }

    generation.foldMap(interpreter).run
  }
}
