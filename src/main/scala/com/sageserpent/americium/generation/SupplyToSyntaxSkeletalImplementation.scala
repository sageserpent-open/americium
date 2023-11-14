package com.sageserpent.americium.generation

import cats.data.StateT
import cats.effect.SyncIO
import cats.effect.kernel.Resource
import cats.~>
import com.google.common.collect.{ImmutableList, Ordering as _, *}
import com.sageserpent.americium.TrialsScaffolding.ShrinkageStop
import com.sageserpent.americium.generation.Decision.{DecisionStages, parseDecisionIndices}
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.generation.JavaPropertyNames.*
import com.sageserpent.americium.generation.SupplyToSyntaxSkeletalImplementation.{maximumScaleDeflationLevel, minimumScaleDeflationLevel, rocksDbResource}
import com.sageserpent.americium.java.{CaseFailureReporting, CaseSupplyCycle, CasesLimitStrategy, CrossApiIterator, InlinedCaseFiltration, NoValidTrialsException, TestIntegrationContext, TrialsScaffolding as JavaTrialsScaffolding}
import com.sageserpent.americium.randomEnrichment.RichRandom
import com.sageserpent.americium.{CaseFactory, TestIntegrationContextImplementation, Trials, TrialsScaffolding as ScalaTrialsScaffolding}
import fs2.{Pull, Stream as Fs2Stream}
import org.rocksdb.{Cache as _, *}
import scalacache.*
import scalacache.caffeine.CaffeineCache

import _root_.java.util.function.Consumer
import _root_.java.util.{ArrayList as JavaArrayList, Iterator as JavaIterator}
import java.nio.file.Path
import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.collection.{mutable, Iterator as ScalaIterator}
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

  implicit val cache: Cache[BigDecimal] = CaffeineCache[BigDecimal]
}

trait SupplyToSyntaxSkeletalImplementation[Case]
    extends JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] {
  protected val casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy
  protected val complexityLimit: Int
  protected val shrinkageAttemptsLimit: Int
  protected val seed: Long
  protected val shrinkageStop: ShrinkageStop[Case]
  protected val validTrialsCheckEnabled: Boolean
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
      shrinkageAttemptIndex: Int,
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

    case class State(
        decisionStagesToGuideShrinkage: Option[DecisionStages],
        decisionStagesInReverseOrder: DecisionStagesInReverseOrder,
        complexity: Int,
        cost: BigInt,
        nextUniqueId: Int
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

      def uniqueId(): (State, Int) =
        copy(nextUniqueId = 1 + nextUniqueId) -> nextUniqueId
    }

    object State {
      val initial = new State(
        decisionStagesToGuideShrinkage = decisionStagesToGuideShrinkage,
        decisionStagesInReverseOrder = NoDecisionStages,
        complexity = 0,
        cost = BigInt(0),
        nextUniqueId = 0
      )
    }

    type StateUpdating[Case] =
      StateT[Option, State, Case]

    // NASTY HACK: what follows is a hacked alternative to using the reader
    // monad whereby the injected context is *mutable*, but at least it's
    // buried in the interpreter for `GenerationOperation`, expressed as a
    // closure over `randomBehaviour`. The reified `FiltrationResult` values
    // are also handled by the interpreter too. Read 'em and weep!

    sealed trait Possibilities

    case class Choices(possibleIndices: LazyList[Int]) extends Possibilities

    val possibilitiesThatFollowSomeChoiceOfDecisionStages =
      mutable.Map.empty[DecisionStagesInReverseOrder, Possibilities]

    def liftUnitIfTheComplexityIsNotTooLarge[Case](
        state: State
    ): StateUpdating[Unit] = {
      // NOTE: this is called *prior* to the complexity being
      // potentially increased by one, hence the strong inequality
      // below; `complexityLimit` *is* inclusive.
      if (state.complexity < complexityLimit)
        StateT.pure(())
      else
        StateT.liftF[Option, State, Unit](
          None
        )
    }

    def interpretChoice[Case](
        choicesByCumulativeFrequency: SortedMap[
          Int,
          Case
        ]
    ): StateUpdating[Case] = {
      val numberOfChoices =
        choicesByCumulativeFrequency.keys.lastOption.getOrElse(0)
      if (0 < numberOfChoices)
        StateT
          .get[Option, State]
          .flatMap(state =>
            state.decisionStagesToGuideShrinkage match {
              case Some(ChoiceOf(guideIndex) :: remainingGuidance)
                  if guideIndex < numberOfChoices =>
                // Guided shrinkage - use the same choice index as the one in
                // the guidance decision stages.
                for {
                  _ <- StateT
                    .set[Option, State](
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
                // Unguided shrinkage isn't applicable to a choice - just choose
                // an index and make sure to cycle in a fair and random way
                // through the alternative choice index values that could follow
                // the preceding decision stages each time this code block is
                // executed.
                for {
                  _ <- liftUnitIfTheComplexityIsNotTooLarge(state)
                  index #:: remainingPossibleIndices =
                    (possibilitiesThatFollowSomeChoiceOfDecisionStages
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
                    }): @unchecked

                  _ <- StateT
                    .set[Option, State](
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
      else StateT.liftF(None)
    }

    def deflatedScale(maximumScale: BigDecimal, level: Int): BigDecimal = {
      import SupplyToSyntaxSkeletalImplementation.cache
      import scalacache.modes.sync.*

      caching[Id, BigDecimal](maximumScale -> level)(None) {
        if (maximumScale <= Double.MaxValue)
          maximumScale / Math.pow(
            maximumScale.toDouble,
            level.toDouble / maximumScaleDeflationLevel
          )
        else {
          deflatedScale(Double.MaxValue, level) * deflatedScale(
            maximumScale / Double.MaxValue,
            level
          )
        }
      }
    }

    def interpretFactory[Case](
        factory: CaseFactory[Case]
    ): StateUpdating[Case] = {
      StateT
        .get[Option, State]
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
                  )) && factory.lowerBoundInput <= guideInput && factory.upperBoundInput >= guideInput =>
              // Guided shrinkage - can choose a factory input somewhere between
              // the one in the guidance decision stages and the shrinkage
              // target's value.
              val input: BigInt =
                (BigDecimal(factory.maximallyShrunkInput) + randomBehaviour
                  .nextDouble() * BigDecimal(
                  guideInput - factory.maximallyShrunkInput
                )).setScale(
                  0,
                  BigDecimal.RoundingMode.HALF_EVEN
                ).rounded
                  .toBigInt

              for {
                _ <- StateT.set[Option, State](
                  state.update(
                    Some(remainingGuidance),
                    FactoryInputOf(input),
                    (input - factory.maximallyShrunkInput)
                      .pow(2)
                  )
                )
              } yield factory(input)
            case _ =>
              // Unguided shrinkage - choose an input between lower and upper
              // bounds that tighten towards the shrinkage target value as the
              // level of shrinkage increases.
              for {
                _ <- liftUnitIfTheComplexityIsNotTooLarge(state)
                input: BigInt = {
                  val upperBoundInput: BigDecimal =
                    BigDecimal(factory.upperBoundInput)
                  val lowerBoundInput: BigDecimal =
                    BigDecimal(factory.lowerBoundInput)
                  val maximallyShrunkInput: BigDecimal =
                    BigDecimal(factory.maximallyShrunkInput)

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
                          deflatedScale(maximumScale, level)
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
                      .toBigInt
                  } else { factory.maximallyShrunkInput }
                }
                _ <- StateT.set[Option, State](
                  state.update(
                    state.decisionStagesToGuideShrinkage
                      .map(_.tail),
                    FactoryInputOf(input),
                    (input - factory.maximallyShrunkInput)
                      .pow(2)
                  )
                )
              } yield factory(input)
          }
        )
    }
    def interpreter(): GenerationOperation ~> StateUpdating =
      new (GenerationOperation ~> StateUpdating) {
        override def apply[Case](
            generationOperation: GenerationOperation[Case]
        ): StateUpdating[Case] =
          generationOperation match {
            case Choice(choicesByCumulativeFrequency) =>
              interpretChoice(choicesByCumulativeFrequency)

            case Factory(factory) =>
              interpretFactory(factory)

            case FiltrationResult(result) =>
              StateT.liftF(result)

            case NoteComplexity =>
              for {
                state <- StateT.get[Option, State]
              } yield state.complexity

            case ResetComplexity(complexity)
                // NOTE: only when *not* shrinking.
                if scaleDeflationLevel.isEmpty =>
              for {
                _ <- StateT.modify[Option, State](
                  _.copy(complexity = complexity)
                )
              } yield ()

            case ResetComplexity(_) =>
              StateT.pure(())

            case UniqueId =>
              StateT[Option, State, Int](state => Some(state.uniqueId()))
          }
      }

    {
      val caseSupplyCycle = new CaseSupplyCycle {
        override def numberOfPreviousCycles(): Int = cycleIndex

        override def numberOfPreviousFailures(): Int = shrinkageAttemptIndex
      }

      val undecoratedCasesLimitStrategy: CasesLimitStrategy =
        casesLimitStrategyFactory(
          caseSupplyCycle
        )

      val casesLimitStrategy = {
        // If we're in the initial cycle of supplying test cases, check to see
        // if *any* valid trials were made in that cycle. Otherwise don't
        // bother; exhaustion is a possibility when shrinking, due to the same
        // maximally shrunk case being de-duplicated, or because shrinkage is
        // not improving, or because shrinkage forces all potential test cases
        // to be filtered out.
        if (validTrialsCheckEnabled && caseSupplyCycle.isInitial)
          new CasesLimitStrategy {

            val underlyingStrategy = undecoratedCasesLimitStrategy

            var numberOfValidCasesEmitted = 0

            override def moreToDo(): Boolean = {
              val moreToDo = underlyingStrategy.moreToDo()
              if (!moreToDo && 0 == numberOfValidCasesEmitted)
                throw new NoValidTrialsException()
              moreToDo
            }
            override def noteRejectionOfCase(): Unit = {
              require(0 < numberOfValidCasesEmitted)
              numberOfValidCasesEmitted -= 1
              underlyingStrategy.noteRejectionOfCase()
            }
            override def noteEmissionOfCase(): Unit = {
              numberOfValidCasesEmitted += 1
              underlyingStrategy.noteEmissionOfCase()
            }
            override def noteStarvation(): Unit =
              underlyingStrategy.noteStarvation()
          }
        else undecoratedCasesLimitStrategy
      }

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
                  .run(State.initial) match {
                  case Some(
                        (
                          State(_, decisionStages, _, factoryInputsCost, _),
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

  override def withComplexityLimit(
      complexityLimit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[
    Case
  ] with ScalaTrialsScaffolding.SupplyToSyntax[Case]

  override def withShrinkageAttemptsLimit(
      shrinkageAttemptsLimit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[
    Case
  ] with ScalaTrialsScaffolding.SupplyToSyntax[Case]

  override def withValidTrialsCheck(
      enabled: Boolean
  ): JavaTrialsScaffolding.SupplyToSyntax[
    Case
  ] with ScalaTrialsScaffolding.SupplyToSyntax[Case]

  // Java-only API ...
  override def supplyTo(consumer: Consumer[Case]): Unit =
    supplyTo(consumer.accept)

  override def asIterator(): JavaIterator[Case] with ScalaIterator[Case] =
    CrossApiIterator.from(
      lazyListOfTestIntegrationContexts().map(_.caze).iterator
    )

  override def testIntegrationContexts()
      : JavaIterator[TestIntegrationContext[Case]]
        with ScalaIterator[TestIntegrationContext[Case]] =
    CrossApiIterator.from(lazyListOfTestIntegrationContexts().iterator)

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

    def streamedCasesWithShrinkageOnFailure(): StreamedCases = {
      val nonDeterministic = Option(
        System.getProperty(nondeterminsticJavaProperty)
      ).fold(ifEmpty = false)(_.toBoolean)

      val randomBehaviour =
        if (nonDeterministic) new Random else new Random(seed)

      def shrink(
          caseData: CaseData,
          throwable: Throwable,
          shrinkageAttemptIndex: Int,
          cycleIndex: Int,
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
            shrinkageAttemptIndex = shrinkageAttemptIndex,
            cycleIndex = cycleIndex
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
                              cycleIndex = 1 + cycleIndex,
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
                                  cycleIndex = 2 + cycleIndex,
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
        shrinkageAttemptIndex = 0,
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
                      cycleIndex = 1,
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

    }

    def testIntegrationContextReproducing(
        recipe: String
    ): TestIntegrationContext[Case] = {
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

    Option(System.getProperty(recipeHashJavaProperty))
      .map(recipeHash =>
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

                testIntegrationContextReproducing(recipe)
              })
            carryOnButSwitchToShrinkageApproachOnCaseFailure(
              singleTestIntegrationContext
            ).stream
          }
      )
      .orElse(
        Option(System.getProperty(recipeJavaProperty))
          .map(recipe =>
            carryOnButSwitchToShrinkageApproachOnCaseFailure(
              Fs2Stream.emit(testIntegrationContextReproducing(recipe))
            ).stream
          )
      )
      .getOrElse(streamedCasesWithShrinkageOnFailure())
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
