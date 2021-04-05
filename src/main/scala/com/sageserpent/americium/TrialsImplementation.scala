package com.sageserpent.americium

import cats.collections.Dequeue
import cats.data.{OptionT, State, StateT}
import cats.free.Free
import cats.free.Free.{liftF, pure}
import cats.syntax.applicative._
import cats.{Eval, ~>}
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import _root_.java.lang.{Iterable => JavaIterable, Long => JavaLong}
import _root_.java.util.Optional
import _root_.java.util.function.{
  Consumer,
  Predicate,
  Supplier,
  Function => JavaFunction
}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Random

object TrialsImplementation {
  sealed trait Decision

  case class ChoiceOf(index: Int)        extends Decision
  case class FactoryInputOf(input: Long) extends Decision

  type DecisionStages = Dequeue[Decision]

  implicit val decisionStagesEncoder: Encoder[DecisionStages] =
    implicitly[Encoder[List[Decision]]].contramap(_.toList)
  implicit val decisionStagesDecoder: Decoder[DecisionStages] =
    implicitly[Decoder[List[Decision]]].emap(list =>
      Right(Dequeue.apply(list: _*))
    )

  type Generation[Case] = Free[GenerationOperation, Case]

  // Java and Scala API ...

  abstract class CommonApi {

    def only[Case](onlyCase: Case): TrialsImplementation[Case] =
      TrialsImplementation(pure[GenerationOperation, Case](onlyCase))

    def choose[Case](
        firstChoice: Case,
        secondChoice: Case,
        otherChoices: Case*
    ): TrialsImplementation[Case] =
      scalaApi.choose(firstChoice +: secondChoice +: otherChoices)
  }

  // Java-only API ...

  val javaApi = new CommonApi with JavaTrialsApi {
    override def delay[Case](
        delayed: Supplier[JavaTrials[Case]]
    ): JavaTrials[Case] = scalaApi.delay(delayed.get().scalaTrials)

    override def choose[Case](
        choices: JavaIterable[Case]
    ): TrialsImplementation[Case] =
      scalaApi.choose(choices.asScala)

    override def choose[Case](
        choices: Array[Case with AnyRef]
    ): TrialsImplementation[Case] =
      scalaApi.choose(choices.toSeq)

    override def alternate[Case](
        firstAlternative: JavaTrials[_ <: Case],
        secondAlternative: JavaTrials[_ <: Case],
        otherAlternatives: JavaTrials[_ <: Case]*
    ): TrialsImplementation[Case] =
      scalaApi.alternate(
        (firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))
          .map(_.scalaTrials)
      )

    override def alternate[Case](
        alternatives: JavaIterable[JavaTrials[Case]]
    ): TrialsImplementation[Case] =
      scalaApi.alternate(alternatives.asScala.map(_.scalaTrials))

    override def alternate[Case](
        alternatives: Array[JavaTrials[Case]]
    ): TrialsImplementation[Case] =
      scalaApi.alternate(alternatives.toSeq.map(_.scalaTrials))

    override def stream[Case](
        factory: JavaFunction[JavaLong, Case]
    ): TrialsImplementation[Case] =
      scalaApi.stream(Long.box _ andThen factory.apply)
  }

  // Scala-only API ...

  val scalaApi = new CommonApi with TrialsApi {
    override def delay[Case](
        delayed: => Trials[Case]
    ): TrialsImplementation[Case] =
      TrialsImplementation(Free.defer(delayed.generation))

    override def choose[Case](
        choices: Iterable[Case]
    ): TrialsImplementation[Case] =
      new TrialsImplementation(Choice(choices.toVector))

    override def alternate[Case](
        firstAlternative: Trials[Case],
        secondAlternative: Trials[Case],
        otherAlternatives: Trials[Case]*
    ): TrialsImplementation[Case] =
      alternate(
        firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*)
      )

    override def alternate[Case](
        alternatives: Iterable[Trials[Case]]
    ): TrialsImplementation[Case] =
      choose(alternatives).flatMap(identity[Trials[Case]] _)

    override def stream[Case](
        factory: Long => Case
    ): TrialsImplementation[Case] =
      new TrialsImplementation(Factory(factory))
  }

  sealed trait GenerationOperation[Case]

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

  case class Choice[Case](choices: Vector[Case])
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
    override val generation: TrialsImplementation.Generation[_ <: Case]
) extends JavaTrials[Case]
    with Trials[Case] {

  import TrialsImplementation._

  def this(
      generationOperation: TrialsImplementation.GenerationOperation[Case]
  ) = {
    this(liftF(generationOperation))
  }

  override private[americium] val scalaTrials = this

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    reproduce(parseDecisionIndices(recipe))

  override def withLimit(
      limit: Int
  ): JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] =
    new JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] {

      // Java-only API ...
      override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
        supplyTo(consumer.accept _)

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit = {
        def shrink(
            caze: Case,
            decisionStages: DecisionStages,
            throwable: Throwable
        ): Unit = {
          val numberOfDecisionStages = decisionStages.size

          if (0 < numberOfDecisionStages) {
            cases(limit, Some(numberOfDecisionStages - 1)).foreach {
              case (
                    decisionStagesForPotentialShrunkCase,
                    potentialShrunkCase
                  ) =>
                try {
                  consumer(potentialShrunkCase)
                } catch {
                  case throwableFromPotentialShrunkCase: Throwable =>
                    shrink(
                      potentialShrunkCase,
                      decisionStagesForPotentialShrunkCase,
                      throwableFromPotentialShrunkCase
                    )
                }
            }
          }

          throw new TrialException(throwable) {
            override def provokingCase: Case = caze

            override def recipe: String = decisionStages.asJson.spaces4
          }
        }

        cases(limit, None).foreach { case (decisionStages, caze) =>
          try {
            consumer(caze)
          } catch {
            case throwable: Throwable =>
              shrink(caze, decisionStages, throwable)
          }
        }
      }
    }

  // Java-only API ...
  override def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase]
  ): TrialsImplementation[TransformedCase] = map(transform.apply _)

  override def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, JavaTrials[TransformedCase]]
  ): TrialsImplementation[TransformedCase] =
    flatMap(step.apply _ andThen (_.scalaTrials))

  override def filter(
      predicate: Predicate[_ >: Case]
  ): TrialsImplementation[Case] =
    filter(predicate.test _)

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[_ >: Case, Optional[TransformedCase]]
  ): TrialsImplementation[TransformedCase] =
    mapFilter(filteringTransform.apply _ andThen {
      case withPayload if withPayload.isPresent => Some(withPayload.get())
      case _                                    => None
    })

  override def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
    supplyTo(recipe, consumer.accept _)

  // Scala-only API ...
  override def map[TransformedCase](
      transform: Case => TransformedCase
  ): TrialsImplementation[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): TrialsImplementation[TransformedCase] =
    flatMap((caze: Case) =>
      new TrialsImplementation(FiltrationResult(filteringTransform(caze)))
    )

  override def filter(predicate: Case => Boolean): TrialsImplementation[Case] =
    flatMap((caze: Case) =>
      new TrialsImplementation(FiltrationResult(Some(caze).filter(predicate)))
    )

  override def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]
  ): TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def supplyTo(recipe: String, consumer: Case => Unit): Unit = {
    val decisionStages = parseDecisionIndices(recipe)
    val reproducedCase = reproduce(decisionStages)

    try {
      consumer(reproducedCase)
    } catch {
      case exception: Throwable =>
        throw new TrialException(exception) {
          override def provokingCase: Case = reproducedCase

          override def recipe: String = decisionStages.asJson.spaces4
        }
    }
  }

  private def reproduce(decisionStages: DecisionStages): Case = {

    type DecisionIndicesContext[Caze] = State[DecisionStages, Caze]

    // NOTE: unlike the companion interpreter in `cases`,
    // this one has a relatively sane implementation.
    def interpreter: GenerationOperation ~> DecisionIndicesContext =
      new (GenerationOperation ~> DecisionIndicesContext) {
        override def apply[Case](
            generationOperation: GenerationOperation[Case]
        ): DecisionIndicesContext[Case] = {
          generationOperation match {
            case Choice(choices) =>
              for {
                decisionStages <- State.get[DecisionStages]
                Some((ChoiceOf(decisionIndex), remainingDecisionStages)) =
                  decisionStages.uncons
                _ <- State.set(remainingDecisionStages)
              } yield choices.drop(decisionIndex).head

            case Factory(factory) =>
              for {
                decisionStages <- State.get[DecisionStages]
                Some((FactoryInputOf(input), remainingDecisionStages)) =
                  decisionStages.uncons
                _ <- State.set(remainingDecisionStages)
              } yield factory(input)

            // NOTE: pattern-match only on `Some`, as we are reproducing a case that by
            // dint of being reproduced, must have passed filtration the first time around.
            case FiltrationResult(Some(caze)) =>
              caze.pure[DecisionIndicesContext]
          }
        }
      }

    generation
      .foldMap(interpreter)
      .runA(decisionStages)
      .value
  }

  private def parseDecisionIndices(recipe: String): DecisionStages = {
    decode[DecisionStages](
      recipe
    ).toTry.get // Just throw the exception, the callers are written in Java style.
  }

  private def cases(
      limit: Int,
      overridingMaximumNumberOfDecisionStages: Option[Int]
  ): Iterator[(DecisionStages, Case)] = {
    val randomBehaviour = new Random(734874)

    type DeferredOption[Case] = OptionT[Eval, Case]

    case class State(
        decisionStages: DecisionStages,
        multiplicity: Option[Int]
    ) {
      def update(decision: ChoiceOf, multiplicity: Int): State = copy(
        decisionStages = decisionStages :+ decision,
        multiplicity = this.multiplicity.map(_ * multiplicity)
      )

      def update(decision: FactoryInputOf): State = copy(
        decisionStages = decisionStages :+ decision,
        multiplicity = None
      )
    }

    object State {
      val initial = new State(Dequeue.empty, Some(1))
    }

    type DecisionIndicesAndMultiplicity = (DecisionStages, Int)

    type StateUpdating[Case] =
      StateT[DeferredOption, State, Case]

    // NASTY HACK: what follows is a hacked alternative to using the reader monad whereby the injected
    // context is *mutable*, but at least it's buried in the interpreter for `GenerationOperation`, expressed
    // as a closure over `randomBehaviour`. The reified `FiltrationResult` values are also handled by the
    // interpreter too. If it's any consolation, it means that flat-mapping is stack-safe - although I'm not
    // entirely sure about alternation. Read 'em and weep!

    val maximumNumberOfDecisionStages: Int = 100

    sealed trait Possibilities

    case class Choices(possibleIndices: LazyList[Int]) extends Possibilities

    val possibilitiesThatFollowSomeChoiceOfDecisionStages =
      mutable.Map.empty[DecisionStages, Possibilities]

    def interpreter(depth: Int): GenerationOperation ~> StateUpdating =
      new (GenerationOperation ~> StateUpdating) {
        override def apply[Case](
            generationOperation: GenerationOperation[Case]
        ): StateUpdating[Case] =
          generationOperation match {
            case Choice(choices) =>
              val numberOfChoices = choices.size
              if (0 < numberOfChoices)
                for {
                  state <- StateT.get[DeferredOption, State]
                  _     <- liftUnitIfTheNumberOfDecisionStagesIsNotTooLarge(state)
                  index #:: remainingPossibleIndices =
                    possibilitiesThatFollowSomeChoiceOfDecisionStages
                      .get(
                        state.decisionStages
                      ) match {
                      case Some(Choices(possibleIndices))
                          if possibleIndices.nonEmpty =>
                        possibleIndices
                      case _ =>
                        randomBehaviour
                          .buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(
                            numberOfChoices
                          )
                    }
                  _ <- StateT.set[DeferredOption, State](
                    state.update(
                      ChoiceOf(index),
                      numberOfChoices
                    )
                  )
                } yield {
                  possibilitiesThatFollowSomeChoiceOfDecisionStages(
                    state.decisionStages
                  ) = Choices(remainingPossibleIndices)
                  choices(index)
                }
              else StateT.liftF(OptionT.none)

            case Factory(factory) =>
              for {
                state <- StateT.get[DeferredOption, State]
                _     <- liftUnitIfTheNumberOfDecisionStagesIsNotTooLarge(state)
                input = randomBehaviour.nextLong()
                _ <- StateT.set[DeferredOption, State](
                  state.update(FactoryInputOf(input))
                )
              } yield factory(input)

            case FiltrationResult(result) =>
              StateT.liftF(OptionT.fromOption(result))
          }

        private def liftUnitIfTheNumberOfDecisionStagesIsNotTooLarge[Case](
            state: State
        ): StateUpdating[Unit] = {
          if (
            state.decisionStages.size < overridingMaximumNumberOfDecisionStages
              .getOrElse(maximumNumberOfDecisionStages)
          )
            ().pure[StateUpdating]
          else
            StateT.liftF[DeferredOption, State, Unit](
              OptionT.none
            )
        }
      }

    new Iterator[Option[(DecisionStages, Case)]] {
      var starvationCountdown: Int         = limit
      var numberOfUniqueCasesProduced: Int = 0
      val potentialDuplicates              = mutable.Set.empty[DecisionStages]

      override def hasNext: Boolean =
        numberOfUniqueCasesProduced < limit && 0 < starvationCountdown

      override def next(): Option[(DecisionStages, Case)] =
        generation
          .foldMap(interpreter(depth = 0))
          .run(State.initial)
          .value
          .value match {
          case Some((State(decisionStages, multiplicity), caze))
              if potentialDuplicates.add(decisionStages) =>
            numberOfUniqueCasesProduced += 1
            val remainingGap = limit - numberOfUniqueCasesProduced
            starvationCountdown =
              multiplicity.fold(remainingGap)(_ min remainingGap)
            Some(decisionStages -> caze)
          case _ =>
            starvationCountdown -= 1
            None
        }

    }.collect { case Some(caze) => caze }
  }
}
