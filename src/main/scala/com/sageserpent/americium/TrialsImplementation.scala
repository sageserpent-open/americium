package com.sageserpent.americium

import cats.data.{OptionT, State, StateT}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.free.Free
import cats.free.Free.liftF
import cats.implicits.*
import cats.{Eval, ~>}
import com.google.common.collect.{Ordering as _, *}
import com.sageserpent.americium.TrialsApis.{javaApi, scalaApi}
import com.sageserpent.americium.java.TrialsScaffolding.OptionalLimits
import com.sageserpent.americium.java.{
  Builder,
  CaseFactory,
  CaseFailureReporting,
  InlinedCaseFiltration,
  TrialsScaffolding as JavaTrialsScaffolding,
  TrialsSkeletalImplementation as JavaTrialsSkeletalImplementation
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsScaffolding as ScalaTrialsScaffolding,
  TrialsSkeletalImplementation as ScalaTrialsSkeletalImplementation
}
import cyclops.control.Either as JavaEither
import cyclops.data.tuple.Tuple2 as JavaTuple2
import fs2.Stream as Fs2Stream
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*

import _root_.java.util.function.{Consumer, Predicate}
import _root_.java.util.{Iterator as JavaIterator, Optional as JavaOptional}
import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Random

object TrialsImplementation {
  val maximumScaleDeflationLevel = 50

  type DecisionStages   = List[Decision]
  type Generation[Case] = Free[GenerationOperation, Case]

  sealed trait Decision

  sealed trait GenerationOperation[Case]

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

  case class ChoiceOf(index: Int) extends Decision

  case class FactoryInputOf(input: Long) extends Decision

  // Use a sorted map keyed by cumulative frequency to implement weighted
  // choices. That idea is inspired by Scalacheck's `Gen.frequency`.
  case class Choice[Case](choicesByCumulativeFrequency: SortedMap[Int, Case])
      extends GenerationOperation[Case]

  case class Factory[Case](factory: CaseFactory[Case])
      extends GenerationOperation[Case]

  // NASTY HACK: as `Free` does not support `filter/withFilter`, reify
  // the optional results of a flat-mapped filtration; the interpreter
  // will deal with these.
  case class FiltrationResult[Case](result: Option[Case])
      extends GenerationOperation[Case]

  case object NoteComplexity extends GenerationOperation[Int]

  case class ResetComplexity[Case](complexity: Int)
      extends GenerationOperation[Unit]
}

case class TrialsImplementation[Case](
    override val generation: TrialsImplementation.Generation[_ <: Case]
) extends ScalaTrialsSkeletalImplementation[Case]
    with JavaTrialsSkeletalImplementation[Case] {
  thisTrialsImplementation =>

  override type SupplySyntaxType = ScalaTrialsScaffolding.SupplyToSyntax[Case]

  import TrialsImplementation.*

  override def scalaTrials: TrialsImplementation[Case] = this

  override def javaTrials: TrialsImplementation[Case] = this

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    reproduce(parseDecisionIndices(recipe))

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
            case Choice(choicesByCumulativeFrequency) =>
              for {
                decisionStages <- State.get[DecisionStages]
                ChoiceOf(decisionIndex) :: remainingDecisionStages =
                  decisionStages
                _ <- State.set(remainingDecisionStages)
              } yield choicesByCumulativeFrequency
                .minAfter(1 + decisionIndex)
                .get
                ._2

            case Factory(factory) =>
              for {
                decisionStages <- State.get[DecisionStages]
                FactoryInputOf(input) :: remainingDecisionStages =
                  decisionStages
                _ <- State.set(remainingDecisionStages)
              } yield factory(input.toInt)

            // NOTE: pattern-match only on `Some`, as we are reproducing a case
            // that by dint of being reproduced, must have passed filtration the
            // first time around.
            case FiltrationResult(Some(result)) =>
              result.pure[DecisionIndicesContext]

            case NoteComplexity =>
              0.pure[DecisionIndicesContext]

            case ResetComplexity(_) =>
              ().pure[DecisionIndicesContext]
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

  override def withLimit(
      limit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(casesLimit = limit)

  override def withLimit(
      limit: Int,
      complexityLimit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(casesLimit = limit, complexityLimit = complexityLimit)

  override def withLimits(
      casesLimit: Int,
      additionalLimits: OptionalLimits
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(
      casesLimit = casesLimit,
      complexityLimit = additionalLimits.complexity,
      shrinkageAttemptsLimit = additionalLimits.shrinkageAttempts,
      shrinkageStop = { () =>
        val predicate: Predicate[_ >: Case] =
          JavaTrialsScaffolding.noStopping.build()

        predicate.test _
      }
    )

  override def withLimits(
      casesLimit: Int,
      additionalLimits: OptionalLimits,
      shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[_ >: Case]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(
      casesLimit = casesLimit,
      complexityLimit = additionalLimits.complexity,
      shrinkageAttemptsLimit = additionalLimits.shrinkageAttempts,
      shrinkageStop = { () =>
        val predicate = shrinkageStop.build()

        predicate.test _
      }
    )

  override def withLimits(
      casesLimit: Int,
      complexityLimit: Int,
      shrinkageAttemptsLimit: Int,
      shrinkageStop: ScalaTrialsScaffolding.ShrinkageStop[Case]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    new JavaTrialsScaffolding.SupplyToSyntax[Case]
      with ScalaTrialsScaffolding.SupplyToSyntax[Case] {
      final case class NonEmptyDecisionStages(
          latestDecision: Decision,
          previousDecisions: DecisionStagesInReverseOrder
      )

      case object NoDecisionStages extends DecisionStagesInReverseOrder

      final case class InternedDecisionStages(index: Int)
          extends DecisionStagesInReverseOrder

      private val nonEmptyToAndFromInternedDecisionStages
          : BiMap[NonEmptyDecisionStages, InternedDecisionStages] =
        HashBiMap.create()

      private def interned(
          nonEmptyDecisionStages: NonEmptyDecisionStages
      ): InternedDecisionStages =
        Option(
          nonEmptyToAndFromInternedDecisionStages.computeIfAbsent(
            nonEmptyDecisionStages,
            _ => {
              val freshIndex = nonEmptyToAndFromInternedDecisionStages.size
              InternedDecisionStages(freshIndex)
            }
          )
        ).get

      sealed trait DecisionStagesInReverseOrder {
        def reverse: DecisionStages = appendInReverseOnTo(List.empty)

        @tailrec
        final def appendInReverseOnTo(
            partialResult: DecisionStages
        ): DecisionStages = this match {
          case NoDecisionStages => partialResult
          case _: InternedDecisionStages =>
            Option(
              nonEmptyToAndFromInternedDecisionStages.inverse().get(this)
            ) match {
              case Some(
                    NonEmptyDecisionStages(latestDecision, previousDecisions)
                  ) =>
                previousDecisions.appendInReverseOnTo(
                  latestDecision :: partialResult
                )
            }
        }

        def addLatest(decision: Decision): DecisionStagesInReverseOrder =
          interned(
            NonEmptyDecisionStages(decision, this)
          )
      }

      private def cases(
          limit: Int,
          complexityLimit: Int,
          randomBehaviour: Random,
          scaleDeflationLevel: Option[Int],
          decisionStagesToGuideShrinkage: Option[DecisionStages]
      ): (
          Fs2Stream[IO, (DecisionStagesInReverseOrder, Case)],
          InlinedCaseFiltration
      ) = {
        scaleDeflationLevel.foreach(level =>
          require((0 to maximumScaleDeflationLevel).contains(level))
        )

        // This is used instead of a straight `Option[Case]` to avoid stack
        // overflow when interpreting `this.generation`. We need to do this
        // because a) we have to support recursively flat-mapped trials and b)
        // even non-recursive trials can bring in a lot of nested flat-maps. Of
        // course, in the recursive case we merely convert the possibility of
        // infinite recursion into infinite looping through the `Eval`
        // trampolining mechanism, so we still have to guard against that and
        // terminate at some point.
        type DeferredOption[Case] = OptionT[Eval, Case]

        case class State(
            decisionStagesToGuideShrinkage: Option[DecisionStages],
            decisionStagesInReverseOrder: DecisionStagesInReverseOrder,
            complexity: Int
        ) {
          def update(
              remainingGuidance: Option[DecisionStages],
              decision: Decision
          ): State = copy(
            decisionStagesToGuideShrinkage = remainingGuidance,
            decisionStagesInReverseOrder =
              decisionStagesInReverseOrder.addLatest(decision),
            complexity = 1 + complexity
          )
        }

        object State {
          val initial = new State(
            decisionStagesToGuideShrinkage = decisionStagesToGuideShrinkage,
            decisionStagesInReverseOrder = NoDecisionStages,
            complexity = 0
          )
        }

        type StateUpdating[Case] =
          StateT[DeferredOption, State, Case]

        // NASTY HACK: what follows is a hacked alternative to using the reader
        // monad whereby the injected context is *mutable*, but at least it's
        // buried in the interpreter for `GenerationOperation`, expressed as a
        // closure over `randomBehaviour`. The reified `FiltrationResult` values
        // are also handled by the interpreter too. Read 'em and weep!

        sealed trait Possibilities

        case class Choices(possibleIndices: LazyList[Int]) extends Possibilities

        val possibilitiesThatFollowSomeChoiceOfDecisionStages =
          mutable.Map.empty[DecisionStagesInReverseOrder, Possibilities]

        def interpreter(depth: Int): GenerationOperation ~> StateUpdating =
          new (GenerationOperation ~> StateUpdating) {
            override def apply[Case](
                generationOperation: GenerationOperation[Case]
            ): StateUpdating[Case] =
              generationOperation match {
                case Choice(choicesByCumulativeFrequency) =>
                  val numberOfChoices =
                    choicesByCumulativeFrequency.keys.lastOption.getOrElse(0)
                  if (0 < numberOfChoices)
                    StateT
                      .get[DeferredOption, State]
                      .flatMap(state =>
                        state.decisionStagesToGuideShrinkage match {
                          case Some(ChoiceOf(guideIndex) :: remainingGuidance)
                              if guideIndex < numberOfChoices =>
                            for {
                              _ <- StateT
                                .set[DeferredOption, State](
                                  state.update(
                                    Some(remainingGuidance),
                                    ChoiceOf(guideIndex)
                                  )
                                )
                            } yield choicesByCumulativeFrequency
                              .minAfter(1 + guideIndex)
                              .get
                              ._2
                          case _ =>
                            for {
                              _ <- liftUnitIfTheComplexityIsNotTooLarge(state)
                              index #:: remainingPossibleIndices =
                                possibilitiesThatFollowSomeChoiceOfDecisionStages
                                  .get(
                                    state.decisionStagesInReverseOrder
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

                              _ <- StateT
                                .set[DeferredOption, State](
                                  state.update(None, ChoiceOf(index))
                                )
                            } yield {
                              possibilitiesThatFollowSomeChoiceOfDecisionStages(
                                state.decisionStagesInReverseOrder
                              ) = Choices(remainingPossibleIndices)
                              choicesByCumulativeFrequency
                                .minAfter(1 + index)
                                .get
                                ._2
                            }
                        }
                      )
                  else StateT.liftF(OptionT.none)

                case Factory(factory) =>
                  StateT
                    .get[DeferredOption, State]
                    .flatMap(state =>
                      state.decisionStagesToGuideShrinkage match {
                        case Some(
                              FactoryInputOf(guideIndex) :: remainingGuidance
                            )
                            if factory
                              .lowerBoundInput() <= guideIndex && factory
                              .upperBoundInput() >= guideIndex =>
                          val index = Math
                            .round(
                              factory.maximallyShrunkInput() + randomBehaviour
                                .nextDouble() * (guideIndex - factory
                                .maximallyShrunkInput())
                            )

                          for {
                            _ <- StateT.set[DeferredOption, State](
                              state.update(
                                Some(remainingGuidance),
                                FactoryInputOf(index)
                              )
                            )
                          } yield factory(index)
                        case _ =>
                          for {
                            _ <- liftUnitIfTheComplexityIsNotTooLarge(state)
                            input = {
                              val upperBoundInput: BigDecimal =
                                factory.upperBoundInput()
                              val lowerBoundInput: BigDecimal =
                                factory.lowerBoundInput()
                              val maximallyShrunkInput: BigDecimal =
                                factory.maximallyShrunkInput()

                              val maximumScale: BigDecimal =
                                upperBoundInput - lowerBoundInput

                              if (
                                scaleDeflationLevel.fold(true)(
                                  maximumScaleDeflationLevel > _
                                ) && 0 < maximumScale
                              ) {
                                // Calibrate the scale to come out at around one
                                // at maximum shrinkage, even though the guard
                                // clause above handles maximum shrinkage
                                // explicitly.
                                val scale: BigDecimal =
                                  scaleDeflationLevel.fold(maximumScale)(
                                    level =>
                                      maximumScale / Math.pow(
                                        maximumScale.toDouble,
                                        level.toDouble / maximumScaleDeflationLevel
                                      )
                                  )
                                val blend: BigDecimal = scale / maximumScale

                                val midPoint: BigDecimal =
                                  blend * (upperBoundInput + lowerBoundInput) / 2 + (1 - blend) * maximallyShrunkInput

                                val sign =
                                  if (randomBehaviour.nextBoolean()) 1 else -1

                                val delta: BigDecimal =
                                  sign * scale * randomBehaviour
                                    .nextDouble() / 2

                                (midPoint + delta)
                                  .setScale(
                                    0,
                                    BigDecimal.RoundingMode.HALF_EVEN
                                  )
                                  .rounded
                                  .toLong
                              } else { maximallyShrunkInput.toLong }
                            }
                            _ <- StateT.set[DeferredOption, State](
                              state.update(None, FactoryInputOf(input))
                            )
                          } yield factory(input)
                      }
                    )

                case FiltrationResult(result) =>
                  StateT.liftF(OptionT.fromOption(result))

                case NoteComplexity =>
                  for {
                    state <- StateT.get[DeferredOption, State]
                  } yield state.complexity

                case ResetComplexity(complexity)
                    if scaleDeflationLevel.isEmpty =>
                  for {
                    _ <- StateT.modify[DeferredOption, State](
                      _.copy(complexity = complexity)
                    )
                  } yield ()

                case ResetComplexity(_) =>
                  StateT.pure(())
              }

            private def liftUnitIfTheComplexityIsNotTooLarge[Case](
                state: State
            ): StateUpdating[Unit] = {
              // NOTE: this is called *prior* to the complexity being
              // potentially increased by one, hence the strong inequality
              // below; `complexityLimit` *is* inclusive.
              if (state.complexity < complexityLimit)
                StateT.pure(())
              else
                StateT.liftF[DeferredOption, State, Unit](
                  OptionT.none
                )
            }
          }

        {
          // NASTY HACK: what was previously a Java-style imperative iterator
          // implementation has, ahem, 'matured' into an overall imperative
          // iterator forwarding to another with a `collect` to flatten out the
          // `Option` part of the latter's output. Both co-operate by sharing
          // mutable state used to determine when the overall iterator should
          // stop yielding output. This in turn allows another hack, namely to
          // intercept calls to `forEach` on the overall iterator so that it can
          // monitor cases that don't pass inline filtration.
          var starvationCountdown: Int         = limit
          var backupOfStarvationCountdown      = 0
          var numberOfUniqueCasesProduced: Int = 0
          val potentialDuplicates =
            mutable.Set.empty[DecisionStagesInReverseOrder]

          val inlinedCaseFiltration: InlinedCaseFiltration =
            new InlinedCaseFiltration {
              override def executeInFiltrationContext(
                  runnable: Runnable,
                  additionalExceptionsToNoteAsFiltration: Array[
                    Class[_ <: Throwable]
                  ]
              ): Boolean = {
                val inlineFilterRejection = new RuntimeException

                try {
                  Trials.throwInlineFilterRejection.withValue(() =>
                    throw inlineFilterRejection
                  ) { runnable.run() }

                  true
                } catch {
                  case exception: RuntimeException
                      if inlineFilterRejection == exception =>
                    noteRejectionOfCase()

                    false
                  case throwable: Throwable
                      if additionalExceptionsToNoteAsFiltration.exists(
                        _.isInstance(throwable)
                      ) =>
                    noteRejectionOfCase()

                    throw throwable
                }
              }

              private def noteRejectionOfCase() = {
                numberOfUniqueCasesProduced -= 1
                starvationCountdown = backupOfStarvationCountdown - 1
              }
            }

          // TODO: use an effect and do this directly in a stream construct -
          // this would fit in better with the streaming philosophy.
          Fs2Stream.suspend(
            Fs2Stream.fromIterator[IO](
              iterator =
                new Iterator[Option[(DecisionStagesInReverseOrder, Case)]] {
                  private def remainingGap = limit - numberOfUniqueCasesProduced

                  override def hasNext: Boolean =
                    0 < remainingGap && 0 < starvationCountdown

                  override def next()
                      : Option[(DecisionStagesInReverseOrder, Case)] =
                    generation
                      .foldMap(interpreter(depth = 0))
                      .run(State.initial)
                      .value
                      .value match {
                      case Some((State(_, decisionStages, _), caze))
                          if potentialDuplicates.add(decisionStages) =>
                        {
                          numberOfUniqueCasesProduced += 1
                          backupOfStarvationCountdown = starvationCountdown
                          starvationCountdown = Math
                            .round(Math.sqrt(limit * remainingGap))
                            .toInt
                        }

                        Some(decisionStages -> caze)
                      case _ =>
                        { starvationCountdown -= 1 }

                        None
                    }
                }.collect { case Some(caze) => caze },
              chunkSize =
                1 // TODO: have to have a chunk size of 1 to allow inline filtration to propagate back up the stream asap, else we get a test failure. It would be better to finesse this somehow...
            )
          ) -> inlinedCaseFiltration
        }
      }

      // Java-only API ...
      override def supplyTo(consumer: Consumer[Case]): Unit =
        supplyTo(consumer.accept)

      override def asIterator(): JavaIterator[Case] = ??? /*{
        val randomBehaviour = new Random(734874)

        cases(
          casesLimit,
          complexityLimit,
          randomBehaviour,
          None,
          decisionStagesToGuideShrinkage = None
        )._1.map(_._2).asJava
      }*/

      override def testIntegration(): JavaTuple2[JavaIterator[
        Case
      ], InlinedCaseFiltration] = ??? /*{
        val randomBehaviour = new Random(734874)

        cases(
          casesLimit,
          complexityLimit,
          randomBehaviour,
          None,
          decisionStagesToGuideShrinkage = None
        ) match {
          case (cases, inlinedCaseFiltration) =>
            JavaTuple2.of(cases.map(_._2).asJava, inlinedCaseFiltration)
        }
      }*/

      private def shrinkableCases: Fs2Stream[
        IO,
        (
            Case,
            CaseFailureReporting,
            InlinedCaseFiltration
        )
      ] = {
        val randomBehaviour = new Random(734874)

        var shrinkageCasesFromDownstream: Option[Fs2Stream[
          IO,
          (
              Case,
              CaseFailureReporting,
              InlinedCaseFiltration
          )
        ]] = None

        def rethrowAsWrappingTrialException(
            caze: Case,
            throwable: Throwable,
            decisionStages: DecisionStages
        ): Unit = {
          throw new TrialException(throwable) {
            override def provokingCase: Case = caze

            override def recipe: String =
              decisionStages.asJson.spaces4
          }
        }

        def reportCaseFailure(
            decisionStagesInReverseOrder: DecisionStagesInReverseOrder,
            caze: Case
        )(throwable: Throwable): Unit = {
          val decisionStages = decisionStagesInReverseOrder.reverse

          def shrink(
              shrinkageAttemptIndex: Int,
              scaleDeflationLevel: Int,
              casesLimit: Int,
              numberOfShrinksInPanicModeIncludingThisOne: Int,
              externalStoppingCondition: Case => Boolean
          ): Fs2Stream[
            IO,
            (
                Case,
                CaseFailureReporting,
                InlinedCaseFiltration
            )
          ] = ???

          shrinkageCasesFromDownstream = Some(
            shrink(
              shrinkageAttemptIndex = 0,
              scaleDeflationLevel = 0,
              casesLimit = casesLimit,
              numberOfShrinksInPanicModeIncludingThisOne = 0,
              externalStoppingCondition = shrinkageStop()
            )
          )

          rethrowAsWrappingTrialException(caze, throwable, decisionStages)
        }

        val businessAsUsualCases: Fs2Stream[
          IO,
          (Case, CaseFailureReporting, InlinedCaseFiltration)
        ] = cases(
          casesLimit,
          complexityLimit,
          randomBehaviour,
          None,
          decisionStagesToGuideShrinkage = None
        ) match {
          case (cases, inlinedCaseFiltration) =>
            cases.map {
              case (
                    decisionStagesInReverseOrder: DecisionStagesInReverseOrder,
                    caze: Case
                  ) =>
                (
                  caze,
                  reportCaseFailure(decisionStagesInReverseOrder, caze)(_),
                  inlinedCaseFiltration
                )
            }
        }

        def carryOnButSwitchToShrinkageApproachOnCaseFailure(
            businessAsUsualCases: Fs2Stream[
              IO,
              (Case, CaseFailureReporting, InlinedCaseFiltration)
            ]
        ): Fs2Stream[
          IO,
          (Case, CaseFailureReporting, InlinedCaseFiltration)
        ] = {
          businessAsUsualCases.flatMap(usualStuff =>
            Fs2Stream
              .eval(IO {
                val capturedShrinkageCases = shrinkageCasesFromDownstream

                shrinkageCasesFromDownstream.foreach(_ => {
                  shrinkageCasesFromDownstream = None
                })

                capturedShrinkageCases
              })
              .flatMap(
                _.fold(ifEmpty =
                  Fs2Stream.emit(usualStuff).covary[IO]
                )          // Just carry on.
                (identity) // Switch to shrinkage cases.
              )
          )
        }

        carryOnButSwitchToShrinkageApproachOnCaseFailure(businessAsUsualCases)
      }

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit = {
        shrinkableCases
          .flatMap {
            case (
                  caze: Case,
                  caseFailureReporting: CaseFailureReporting,
                  inlinedCaseFiltration: InlinedCaseFiltration
                ) =>
              Fs2Stream.eval(IO {
                try {
                  inlinedCaseFiltration.executeInFiltrationContext(
                    () => consumer(caze),
                    Array.empty
                  )
                } catch {
                  case throwable: Throwable =>
                    caseFailureReporting.report(throwable)
                }
              })
          }
          .compile
          .drain
          .unsafeRunSync()
      }

    }

  def this(
      generationOperation: TrialsImplementation.GenerationOperation[Case]
  ) = {
    this(liftF(generationOperation))
  }

  def withRecipe(
      recipe: String
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    new JavaTrialsScaffolding.SupplyToSyntax[Case]
      with ScalaTrialsScaffolding.SupplyToSyntax[Case] {
      // Java-only API ...
      override def supplyTo(consumer: Consumer[Case]): Unit =
        supplyTo(consumer.accept)

      override def asIterator(): JavaIterator[Case] = Seq {
        val decisionStages = parseDecisionIndices(recipe)
        reproduce(decisionStages)
      }.asJava.iterator()

      override def testIntegration()
          : JavaTuple2[JavaIterator[Case], InlinedCaseFiltration] =
        JavaTuple2.of(
          asIterator(),
          {
            (
                runnable: Runnable,
                additionalExceptionsToHandleAsFiltration: Array[
                  Class[_ <: Throwable]
                ]
            ) =>
              runnable.run()
              true
          }
        )

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit = {
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
    }

  // Scala-only API ...
  protected override def several[Collection](
      builderFactory: => Builder[Case, Collection]
  ): TrialsImplementation[Collection] = {
    def addItems(partialResult: List[Case]): TrialsImplementation[Collection] =
      scalaApi.alternate(
        scalaApi.only {
          val builder = builderFactory
          partialResult.foreach(builder add _)
          builder.build()
        },
        flatMap(item =>
          addItems(item :: partialResult): ScalaTrials[Collection]
        )
      )

    addItems(Nil)
  }

  override def several[Collection](implicit
      factory: scala.collection.Factory[Case, Collection]
  ): TrialsImplementation[Collection] = several(new Builder[Case, Collection] {
    private val underlyingBuilder = factory.newBuilder

    override def add(caze: Case): Unit = {
      underlyingBuilder += caze
    }

    override def build(): Collection = underlyingBuilder.result()
  })

  protected def lotsOfSize[Collection](
      size: Int,
      builderFactory: => Builder[Case, Collection]
  ): TrialsImplementation[Collection] =
    scalaApi.complexities.flatMap(complexity => {
      def addItems(
          numberOfItems: Int,
          partialResult: List[Case]
      ): ScalaTrials[Collection] =
        if (0 >= numberOfItems)
          scalaApi.only {
            val builder = builderFactory
            partialResult.foreach(builder add _)
            builder.build()
          }
        else
          flatMap(item =>
            (scalaApi
              .resetComplexity(complexity): ScalaTrials[Unit])
              .flatMap(_ =>
                addItems(
                  numberOfItems - 1,
                  item :: partialResult
                )
              )
          )

      addItems(size, Nil)
    })

  override def lotsOfSize[Collection](size: Int)(implicit
      factory: collection.Factory[Case, Collection]
  ): TrialsImplementation[Collection] = lotsOfSize(
    size,
    new Builder[Case, Collection] {
      private val underlyingBuilder = factory.newBuilder

      override def add(caze: Case): Unit = {
        underlyingBuilder += caze
      }

      override def build(): Collection = underlyingBuilder.result()
    }
  )

  override def or[Case2](
      alternativeTrials: ScalaTrials[Case2]
  ): TrialsImplementation[Either[Case, Case2]] = scalaApi.alternate(
    this.map(Either.left[Case, Case2]),
    alternativeTrials.map(Either.right)
  )

  override def or[Case2](
      alternativeTrials: java.Trials[Case2]
  ): TrialsImplementation[
    JavaEither[Case, Case2]
  ] = javaApi.alternate(
    this.map(JavaEither.left[Case, Case2]),
    alternativeTrials.map(JavaEither.right[Case, Case2])
  )

  override def options: TrialsImplementation[Option[Case]] =
    scalaApi.alternate(scalaApi.only(None), this.map(Some.apply[Case]))

  override def optionals(): TrialsImplementation[
    JavaOptional[Case]
  ] = javaApi.alternate(
    javaApi.only(JavaOptional.empty()),
    this.map(JavaOptional.of[Case])
  )
}
