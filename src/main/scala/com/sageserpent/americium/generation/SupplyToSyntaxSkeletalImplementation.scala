package com.sageserpent.americium.generation

import cats.data.{OptionT, StateT}
import cats.effect.SyncIO
import cats.effect.kernel.Resource
import cats.{Eval, ~>}
import com.google.common.collect.{ImmutableList, Ordering as _, *}
import com.sageserpent.americium.TrialsScaffolding.ShrinkageStop
import com.sageserpent.americium.generation.Decision.{
  DecisionStages,
  parseDecisionIndices
}
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.generation.JavaPropertyNames.{
  recipeHashJavaProperty,
  runDatabaseJavaProperty,
  temporaryDirectoryJavaProperty
}
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.{
  maximumScaleDeflationLevel,
  minimumScaleDeflationLevel,
  rocksDbResource
}
import com.sageserpent.americium.java.{
  CaseFailureReporting,
  CaseSupplyCycle,
  CasesLimitStrategy,
  InlinedCaseFiltration,
  TestIntegrationContext,
  TrialsScaffolding as JavaTrialsScaffolding
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import com.sageserpent.americium.{
  TestIntegrationContextImplementation,
  Trials,
  TrialsScaffolding as ScalaTrialsScaffolding
}
import fs2.{Pull, Stream as Fs2Stream}
import org.rocksdb.*

import _root_.java.util.function.Consumer
import _root_.java.util.{ArrayList as JavaArrayList, Iterator as JavaIterator}
import java.nio.file.Path
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Random

object SupplyToSyntaxSkeletalImplementation {

  val runDatabaseDefault = "trialsRunDatabase"

  val minimumScaleDeflationLevel = 0

  val maximumScaleDeflationLevel = 50

  val rocksDbOptions = new DBOptions()
    .optimizeForSmallDb()
    .setCreateIfMissing(true)
    .setCreateMissingColumnFamilies(true)

  val columnFamilyOptions = new ColumnFamilyOptions()
    .setCompressionType(CompressionType.LZ4_COMPRESSION)
    .setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION)

  val defaultColumnFamilyDescriptor = new ColumnFamilyDescriptor(
    RocksDB.DEFAULT_COLUMN_FAMILY,
    columnFamilyOptions
  )

  val columnFamilyDescriptorForRecipeHashes = new ColumnFamilyDescriptor(
    "RecipeHashKeyRecipeValue".getBytes(),
    columnFamilyOptions
  )

  def rocksDbResource(
      readOnly: Boolean = false
  ): Resource[SyncIO, (RocksDB, ColumnFamilyHandle)] =
    Resource.make(acquire = SyncIO {
      Option(System.getProperty(temporaryDirectoryJavaProperty)).fold(ifEmpty =
        throw new RuntimeException(
          s"No definition of Java property: `$temporaryDirectoryJavaProperty`"
        )
      ) { directory =>
        val runDatabase = Option(
          System.getProperty(runDatabaseJavaProperty)
        ).getOrElse(runDatabaseDefault)

        val columnFamilyDescriptors =
          ImmutableList.of(
            defaultColumnFamilyDescriptor,
            columnFamilyDescriptorForRecipeHashes
          )

        val columnFamilyHandles = new JavaArrayList[ColumnFamilyHandle]()

        val rocksDB =
          if (readOnly)
            RocksDB.openReadOnly(
              rocksDbOptions,
              Path
                .of(directory)
                .resolve(runDatabase)
                .toString,
              columnFamilyDescriptors,
              columnFamilyHandles
            )
          else
            RocksDB.open(
              rocksDbOptions,
              Path
                .of(directory)
                .resolve(runDatabase)
                .toString,
              columnFamilyDescriptors,
              columnFamilyHandles
            )

        rocksDB -> columnFamilyHandles.get(1)
      }
    })(release = { case (rocksDB, columnFamilyForRecipeHashes) =>
      SyncIO {
        columnFamilyForRecipeHashes.close()
        rocksDB.close()
      }
    })
}

trait SupplyToSyntaxSkeletalImplementation[Case]
    extends JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] {
  protected val casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy
  protected val complexityLimit: Int
  protected val shrinkageAttemptsLimit: Int
  protected val seed: Long
  protected val shrinkageStop: ShrinkageStop[Case]
  protected val generation: Generation[_ <: Case]

  type StreamedCases =
    Fs2Stream[SyncIO, TestIntegrationContext[Case]]

  type PullOfCases =
    Pull[SyncIO, TestIntegrationContext[Case], Unit]

  final case class NonEmptyDecisionStages(
      latestDecision: Decision,
      previousDecisions: DecisionStagesInReverseOrder
  ) { def size: Int = 1 + previousDecisions.size }

  case object NoDecisionStages extends DecisionStagesInReverseOrder {
    override def nonEmpty: Boolean = false
    override def size: Int         = 0
  }

  final case class InternedDecisionStages(
      index: Int,
      override val size: Int
  ) extends DecisionStagesInReverseOrder {
    require(0 < size)

    override def nonEmpty: Boolean = true
  }

  // NOTE: this cache is maintained at the instance-level rather than in the
  // companion object. Hoisting it into, say the companion object would cause
  // failures of SBT when it tries to run multiple tests in parallel using
  // multithreading.
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
          InternedDecisionStages(
            index = freshIndex,
            size = nonEmptyDecisionStages.size
          )
        }
      )
    ).get

  sealed trait DecisionStagesInReverseOrder {
    def nonEmpty: Boolean

    def size: Int

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

  case class CaseData(
      caze: Case,
      decisionStagesInReverseOrder: DecisionStagesInReverseOrder,
      cost: BigInt
  )

  type ShrinkageIsImproving =
    Function[(DecisionStagesInReverseOrder, BigInt), Boolean]

  val potentialDuplicates =
    mutable.Set.empty[DecisionStagesInReverseOrder]

  private def cases(
      complexityLimit: Int,
      randomBehaviour: Random,
      scaleDeflationLevel: Option[Int],
      shrinkageIsImproving: ShrinkageIsImproving,
      decisionStagesToGuideShrinkage: Option[DecisionStages],
      cycleIndex: Int
  ): (
      Fs2Stream[SyncIO, CaseData],
      InlinedCaseFiltration
  ) = {
    scaleDeflationLevel.foreach(level =>
      require(
        (minimumScaleDeflationLevel to maximumScaleDeflationLevel).contains(
          level
        )
      )
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
        complexity: Int,
        cost: BigInt
    ) {
      def update(
          remainingGuidance: Option[DecisionStages],
          decision: Decision,
          costIncrement: BigInt = BigInt(0)
      ): State = copy(
        decisionStagesToGuideShrinkage = remainingGuidance,
        decisionStagesInReverseOrder =
          decisionStagesInReverseOrder.addLatest(decision),
        complexity = 1 + complexity,
        cost = cost + costIncrement
      )
    }

    object State {
      val initial = new State(
        decisionStagesToGuideShrinkage = decisionStagesToGuideShrinkage,
        decisionStagesInReverseOrder = NoDecisionStages,
        complexity = 0,
        cost = BigInt(0)
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

    def interpreter(): GenerationOperation ~> StateUpdating =
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
                          FactoryInputOf(guideInput) :: remainingGuidance
                        )
                        if (remainingGuidance.forall(_ match {
                          case _: FactoryInputOf => false
                          case _: ChoiceOf       => true
                        }) || 1 < randomBehaviour
                          .chooseAnyNumberFromOneTo(
                            1 + remainingGuidance
                              .filter(_ match {
                                case _: FactoryInputOf => true
                                case _: ChoiceOf       => false
                              })
                              .size
                          )) && factory
                          .lowerBoundInput() <= guideInput && factory
                          .upperBoundInput() >= guideInput =>
                      val input = Math
                        .round(
                          factory.maximallyShrunkInput() + randomBehaviour
                            .nextDouble() * (guideInput - factory
                            .maximallyShrunkInput())
                        )

                      for {
                        _ <- StateT.set[DeferredOption, State](
                          state.update(
                            Some(remainingGuidance),
                            FactoryInputOf(input),
                            (BigInt(input) - factory.maximallyShrunkInput())
                              .pow(2)
                          )
                        )
                      } yield factory(input)
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
                            // explicitly. Also handle an explicit scale
                            // deflation level of zero in the same manner as
                            // the implicit situation.
                            val scale: BigDecimal =
                              scaleDeflationLevel
                                .filter(minimumScaleDeflationLevel < _)
                                .fold(maximumScale)(level =>
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
                          state.update(
                            state.decisionStagesToGuideShrinkage
                              .map(_.tail),
                            FactoryInputOf(input),
                            (BigInt(input) - factory.maximallyShrunkInput())
                              .pow(2)
                          )
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
                // NOTE: only when *not* shrinking.
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
      val caseSupplyCycle = new CaseSupplyCycle {
        override def numberOfPreviousCycles(): Int =
          cycleIndex
      }

      val casesLimitStrategy = casesLimitStrategyFactory(caseSupplyCycle)

      val inlinedCaseFiltration: InlinedCaseFiltration =
        (
            runnable: Runnable,
            additionalExceptionsToNoteAsFiltration: Array[
              Class[_ <: Throwable]
            ]
        ) => {
          val inlineFilterRejection = new RuntimeException

          try {
            Trials.throwInlineFilterRejection.withValue(() =>
              throw inlineFilterRejection
            ) { runnable.run() }

            true
          } catch {
            case exception: RuntimeException
                if inlineFilterRejection == exception =>
              casesLimitStrategy.noteRejectionOfCase()

              false
            case throwable: Throwable
                if additionalExceptionsToNoteAsFiltration.exists(
                  _.isInstance(throwable)
                ) =>
              casesLimitStrategy.noteRejectionOfCase()

              throw throwable
          }
        }

      def emitCases(): Fs2Stream[SyncIO, CaseData] =
        Fs2Stream.force(SyncIO {
          if (casesLimitStrategy.moreToDo())
            Fs2Stream
              .eval(SyncIO {
                generation
                  .foldMap(interpreter())
                  .run(State.initial)
                  .value
                  .value match {
                  case Some(
                        (
                          State(_, decisionStages, _, factoryInputsCost),
                          caze
                        )
                      )
                      if potentialDuplicates
                        .add(decisionStages) && shrinkageIsImproving(
                        decisionStages,
                        factoryInputsCost
                      ) =>
                    casesLimitStrategy.noteEmissionOfCase()

                    Some(CaseData(caze, decisionStages, factoryInputsCost))
                  case _ =>
                    casesLimitStrategy.noteStarvation()

                    None
                }
              })
              .collect { case Some(caze) => caze } ++ emitCases()
          else Fs2Stream.empty
        })

      emitCases() -> inlinedCaseFiltration
    }
  }

  override def withSeed(
      seed: Long
  ): JavaTrialsScaffolding.SupplyToSyntax[
    Case
  ] with ScalaTrialsScaffolding.SupplyToSyntax[Case]

  // Java-only API ...
  override def supplyTo(consumer: Consumer[Case]): Unit =
    supplyTo(consumer.accept)

  override def asIterator(): JavaIterator[Case] =
    lazyListOfTestIntegrationContexts().map(_.caze).asJava.iterator()

  override def testIntegrationContexts()
      : JavaIterator[TestIntegrationContext[Case]] =
    lazyListOfTestIntegrationContexts().asJava.iterator()

  private def lazyListOfTestIntegrationContexts()
      : LazyList[TestIntegrationContext[Case]] = {
    LazyList.unfold(shrinkableCases()) { streamedCases =>
      streamedCases.pull.uncons1
        .flatMap {
          case None              => Pull.done
          case Some(headAndTail) => Pull.output1(headAndTail)
        }
        .stream
        .head
        .compile
        .last
        .attempt
        .unsafeRunSync() match {
        case Left(throwable) =>
          throw throwable
        case Right(cargo) =>
          cargo
      }
    }
  }

  protected def reproduce(decisionStages: DecisionStages): Case

  protected def raiseTrialException(
      rocksDb: Option[(RocksDB, ColumnFamilyHandle)]
  )(
      throwable: Throwable,
      caze: Case,
      decisionStages: DecisionStages
  ): StreamedCases

  private def raiseTrialException(
      throwable: Throwable,
      caseData: CaseData
  ): StreamedCases = Fs2Stream
    .resource(rocksDbResource())
    .flatMap(rocksDbPayload =>
      raiseTrialException(Some(rocksDbPayload))(
        throwable,
        caseData.caze,
        caseData.decisionStagesInReverseOrder.reverse
      )
    )

  private def shrinkableCases(): StreamedCases = {
    var shrinkageCasesFromDownstream: Option[StreamedCases] = None

    def carryOnButSwitchToShrinkageApproachOnCaseFailure(
        businessAsUsualCases: StreamedCases
    ): PullOfCases = Pull
      .eval(SyncIO {
        val capture = shrinkageCasesFromDownstream

        capture.foreach { _ => shrinkageCasesFromDownstream = None }

        capture
      })
      .flatMap(
        _.fold
          // If there are no shrinkage cases from downstream, we need to
          // pull a single case and carry on with the remaining business
          // as usual via a recursive call to this method.
          (ifEmpty =
            businessAsUsualCases.pull.uncons1
              .flatMap(
                _.fold(ifEmpty =
                  Pull.done
                    .covary[SyncIO]
                    .covaryOutput[
                      TestIntegrationContext[Case]
                    ]
                ) { case (headCase, remainingCases) =>
                  Pull.output1(
                    headCase
                  ) >> carryOnButSwitchToShrinkageApproachOnCaseFailure(
                    remainingCases
                  )
                }
              )
          )
          // If there are shrinkage cases from downstream, we need drop
          // business as usual and switch to them instead.
          (carryOnButSwitchToShrinkageApproachOnCaseFailure)
      )

    Option(System.getProperty(recipeHashJavaProperty)).fold {
      val randomBehaviour = new Random(seed)

      def shrink(
          caseData: CaseData,
          throwable: Throwable,
          shrinkageAttemptIndex: Int,
          scaleDeflationLevel: Int,
          numberOfShrinksInPanicModeIncludingThisOne: Int,
          externalStoppingCondition: Case => Boolean,
          exhaustionStrategy: => StreamedCases
      ): StreamedCases = {
        require(caseData.decisionStagesInReverseOrder.nonEmpty)

        if (
          shrinkageAttemptsLimit == shrinkageAttemptIndex || externalStoppingCondition(
            caseData.caze
          )
        ) raiseTrialException(throwable, caseData)
        else {
          require(shrinkageAttemptsLimit > shrinkageAttemptIndex)

          val numberOfDecisionStages =
            caseData.decisionStagesInReverseOrder.size

          val mainProcessing = cases(
            numberOfDecisionStages,
            randomBehaviour,
            scaleDeflationLevel = Some(scaleDeflationLevel),
            shrinkageIsImproving = {
              case (decisionStagesInReverseOrder, factoryInputsCost) =>
                decisionStagesInReverseOrder.size < caseData.decisionStagesInReverseOrder.size
                || (factoryInputsCost <= caseData.cost)
            },
            decisionStagesToGuideShrinkage = Option.when(
              0 < numberOfShrinksInPanicModeIncludingThisOne
            )(caseData.decisionStagesInReverseOrder.reverse),
            cycleIndex = 1 + shrinkageAttemptIndex
          ) match {
            case (cases, inlinedCaseFiltration) =>
              cases.flatMap { case potentialShrunkCaseData =>
                Fs2Stream.emit(
                  TestIntegrationContextImplementation[Case](
                    caze = potentialShrunkCaseData.caze,
                    caseFailureReporting =
                      (throwableFromPotentialShrunkCase: Throwable) => {

                        assert(
                          potentialShrunkCaseData.decisionStagesInReverseOrder.size <= numberOfDecisionStages
                        )

                        val lessComplex =
                          potentialShrunkCaseData.decisionStagesInReverseOrder.size < numberOfDecisionStages

                        val stillEnoughRoomToDecreaseScale =
                          scaleDeflationLevel < maximumScaleDeflationLevel

                        shrinkageCasesFromDownstream = Some(
                          {
                            val scaleDeflationLevelForRecursion =
                              if (
                                stillEnoughRoomToDecreaseScale && !lessComplex
                              )
                                1 + scaleDeflationLevel
                              else scaleDeflationLevel

                            shrink(
                              caseData = potentialShrunkCaseData,
                              throwable = throwableFromPotentialShrunkCase,
                              shrinkageAttemptIndex = 1 + shrinkageAttemptIndex,
                              scaleDeflationLevel =
                                scaleDeflationLevelForRecursion,
                              numberOfShrinksInPanicModeIncludingThisOne = 0,
                              externalStoppingCondition =
                                externalStoppingCondition,
                              exhaustionStrategy = {
                                // At this point, slogging through the
                                // potential shrunk cases failed to
                                // find any failures; go into (or
                                // remain in) panic mode...
                                shrink(
                                  caseData = potentialShrunkCaseData,
                                  throwable = throwableFromPotentialShrunkCase,
                                  shrinkageAttemptIndex =
                                    1 + shrinkageAttemptIndex,
                                  scaleDeflationLevel = scaleDeflationLevel,
                                  numberOfShrinksInPanicModeIncludingThisOne =
                                    1 + numberOfShrinksInPanicModeIncludingThisOne,
                                  externalStoppingCondition =
                                    externalStoppingCondition,
                                  exhaustionStrategy = {
                                    raiseTrialException(
                                      throwableFromPotentialShrunkCase,
                                      potentialShrunkCaseData
                                    )
                                  }
                                )
                              }
                            )
                          }
                        )
                      },
                    inlinedCaseFiltration = inlinedCaseFiltration,
                    isPartOfShrinkage = true
                  )
                )
              }
          }

          mainProcessing ++ exhaustionStrategy
        }
      }

      val businessAsUsualCases: StreamedCases = cases(
        complexityLimit,
        randomBehaviour,
        scaleDeflationLevel = None,
        shrinkageIsImproving = _ => true,
        decisionStagesToGuideShrinkage = None,
        cycleIndex = 0
      ) match {
        case (cases, inlinedCaseFiltration) =>
          cases.map { case caseData =>
            TestIntegrationContextImplementation[Case](
              caze = caseData.caze,
              caseFailureReporting = (throwable: Throwable) => {
                shrinkageCasesFromDownstream = Some(
                  if (caseData.decisionStagesInReverseOrder.nonEmpty)
                    shrink(
                      caseData = caseData,
                      throwable = throwable,
                      shrinkageAttemptIndex = 0,
                      scaleDeflationLevel = 0,
                      numberOfShrinksInPanicModeIncludingThisOne = 0,
                      externalStoppingCondition = shrinkageStop(),
                      exhaustionStrategy = {
                        raiseTrialException(throwable, caseData)
                      }
                    )
                  else
                    raiseTrialException(throwable, caseData)
                )
              },
              inlinedCaseFiltration = inlinedCaseFiltration,
              isPartOfShrinkage = false
            )
          }
      }

      carryOnButSwitchToShrinkageApproachOnCaseFailure(
        businessAsUsualCases
      ).stream
    }(recipeHash =>
      Fs2Stream
        .resource(rocksDbResource(readOnly = true))
        .flatMap { case (rocksDb, columnFamilyForRecipeHashes) =>
          val singleTestIntegrationContext = Fs2Stream
            .eval(SyncIO {
              val recipe = rocksDb
                .get(
                  columnFamilyForRecipeHashes,
                  recipeHash.map(_.toByte).toArray
                )
                .map(_.toChar)
                .mkString

              {
                val decisionStages = parseDecisionIndices(recipe)
                val caze           = reproduce(decisionStages)

                TestIntegrationContextImplementation[Case](
                  caze = caze,
                  caseFailureReporting = { (throwable: Throwable) =>
                    shrinkageCasesFromDownstream = Some(
                      raiseTrialException(None)(
                        throwable,
                        caze,
                        decisionStages
                      )
                    )
                  },
                  inlinedCaseFiltration = {
                    (
                        runnable: Runnable,
                        additionalExceptionsToHandleAsFiltration: Array[
                          Class[_ <: Throwable]
                        ]
                    ) =>
                      runnable.run()
                      true
                  },
                  isPartOfShrinkage = false
                )
              }
            })
          carryOnButSwitchToShrinkageApproachOnCaseFailure(
            singleTestIntegrationContext
          ).stream
        }
    )
  }

  // Scala-only API ...
  override def supplyTo(consumer: Case => Unit): Unit = {
    shrinkableCases()
      .flatMap {
        case TestIntegrationContextImplementation(
              caze: Case,
              caseFailureReporting: CaseFailureReporting,
              inlinedCaseFiltration: InlinedCaseFiltration,
              _
            ) =>
          Fs2Stream.eval(SyncIO {
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
      .attempt
      .unsafeRunSync()
      .toTry
      .get
  }
}
