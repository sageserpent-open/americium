package com.sageserpent.americium

import cats.data.{OptionT, State, StateT}
import cats.effect.SyncIO
import cats.effect.kernel.Resource
import cats.free.Free
import cats.free.Free.liftF
import cats.implicits.*
import cats.{Eval, ~>}
import com.google.common.collect.{Ordering as _, *}
import com.google.common.hash
import com.sageserpent.americium.TrialsApis.{javaApi, scalaApi}
import com.sageserpent.americium.java.TrialsScaffolding.OptionalLimits
import com.sageserpent.americium.java.{
  Builder,
  CaseFactory,
  CaseFailureReporting,
  InlinedCaseFiltration,
  TestIntegrationContext,
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
import fs2.{Pull, Stream as Fs2Stream}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.rocksdb.*

import _root_.java.nio.file.Path
import _root_.java.util.function.{Consumer, Predicate}
import _root_.java.util.{
  ArrayList as JavaArrayList,
  Iterator as JavaIterator,
  Optional as JavaOptional
}
import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Random

object TrialsImplementation {
  val temporaryDirectoryJavaProperty = "java.io.tmpdir"

  val runDatabaseJavaPropertyName = "trials.runDatabase"

  val recipeHashJavaPropertyName = "trials.recipeHash"

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
          System.getProperty(runDatabaseJavaPropertyName)
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

case class TrialsImplementation[Case](
    override val generation: TrialsImplementation.Generation[_ <: Case]
) extends ScalaTrialsSkeletalImplementation[Case]
    with JavaTrialsSkeletalImplementation[Case] {
  thisTrialsImplementation =>

  override type SupplySyntaxType = ScalaTrialsScaffolding.SupplyToSyntax[Case]

  type StreamedCases =
    Fs2Stream[SyncIO, TestIntegrationContext[Case]]

  type PullOfCases =
    Pull[SyncIO, TestIntegrationContext[Case], Unit]

  case class TestIntegrationContextImplementation(
      caze: Case,
      caseFailureReporting: CaseFailureReporting,
      inlinedCaseFiltration: InlinedCaseFiltration,
      isPartOfShrinkage: Boolean
  ) extends com.sageserpent.americium.java.TestIntegrationContext[Case] {}

  import TrialsImplementation.*

  override def trials: TrialsImplementation[Case] = this

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

      // NOTE: this cache is the reason for all of the
      // `DecisionStagesInReverseOrder` support being defined as method-local.
      // Hoisting it into, say the companion object `TrialsImplementation`
      // causes failures of SBT when it tries to run multiple tests in parallel
      // using multithreading.
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
          limit: Int,
          complexityLimit: Int,
          randomBehaviour: Random,
          scaleDeflationLevel: Option[Int],
          shrinkageIsImproving: ShrinkageIsImproving,
          decisionStagesToGuideShrinkage: Option[DecisionStages]
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
          class CasesLimitStrategy {
            val casesLimit: Int                  = limit
            var starvationCountdown: Int         = casesLimit
            var backupOfStarvationCountdown      = 0
            var numberOfUniqueCasesProduced: Int = 0

            def noteRejectionOfCase() = {
              numberOfUniqueCasesProduced -= 1
              starvationCountdown = backupOfStarvationCountdown - 1
            }

            def remainingGap() =
              casesLimit - numberOfUniqueCasesProduced

            def moreToDo() =
              0 < remainingGap && 0 < starvationCountdown

            def noteEmissionOfCase(): Unit = {
              backupOfStarvationCountdown = starvationCountdown
              starvationCountdown = Math
                .round(
                  Math.sqrt(
                    casesLimit.toDouble * remainingGap()
                  )
                )
                .toInt
              numberOfUniqueCasesProduced += 1
            }

            def noteStarvation(): Unit = {
              starvationCountdown -= 1
            }
          }

          val casesLimitStrategy = new CasesLimitStrategy();

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

      private def raiseTrialException(
          rocksDb: Option[(RocksDB, ColumnFamilyHandle)]
      )(
          throwable: Throwable,
          caze: Case,
          decisionStages: DecisionStages
      ): StreamedCases = {
        val json = decisionStages.asJson.spaces4
        val jsonHashInHexadecimal = hash.Hashing
          .murmur3_128()
          .hashUnencodedChars(json)
          .toString

        // TODO: suppose this throws an exception? Probably best to
        // just log it and carry on, as the user wants to see a test
        // failure rather than an issue with the database.
        rocksDb.foreach { case (rocksDb, columnFamilyForRecipeHashes) =>
          rocksDb.put(
            columnFamilyForRecipeHashes,
            jsonHashInHexadecimal.map(_.toByte).toArray,
            json.map(_.toByte).toArray
          )
        }

        Fs2Stream.raiseError[SyncIO](new TrialException(throwable) {
          override def provokingCase: Case = caze

          override def recipe: String = json

          override def recipeHash: String = jsonHashInHexadecimal
        })
      }

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

        Option(System.getProperty(recipeHashJavaPropertyName)).fold {
          val randomBehaviour = new Random(734874)

          def shrink(
              caseData: CaseData,
              throwable: Throwable,
              shrinkageAttemptIndex: Int,
              scaleDeflationLevel: Int,
              casesLimit: Int,
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

              // NOTE: there's some voodoo in choosing the exponential
              // scaling factor - if it's too high, say 2, then the
              // solutions are hardly shrunk at all. If it is unity, then
              // the solutions are shrunk a bit but can be still involve
              // overly 'large' values, in the sense that the factory input
              // values are large. This needs finessing, but will do for
              // now...
              val limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases =
                (100 * casesLimit / 99) max casesLimit

              val mainProcessing = cases(
                limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases,
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
                )(caseData.decisionStagesInReverseOrder.reverse)
              ) match {
                case (cases, inlinedCaseFiltration) =>
                  cases.flatMap { case potentialShrunkCaseData =>
                    Fs2Stream.emit(
                      TestIntegrationContextImplementation(
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
                                  shrinkageAttemptIndex =
                                    1 + shrinkageAttemptIndex,
                                  scaleDeflationLevel =
                                    scaleDeflationLevelForRecursion,
                                  casesLimit =
                                    limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases,
                                  numberOfShrinksInPanicModeIncludingThisOne =
                                    0,
                                  externalStoppingCondition =
                                    externalStoppingCondition,
                                  exhaustionStrategy = {
                                    // At this point, slogging through the
                                    // potential shrunk cases failed to
                                    // find any failures; go into (or
                                    // remain in) panic mode...
                                    shrink(
                                      caseData = potentialShrunkCaseData,
                                      throwable =
                                        throwableFromPotentialShrunkCase,
                                      shrinkageAttemptIndex =
                                        1 + shrinkageAttemptIndex,
                                      scaleDeflationLevel = scaleDeflationLevel,
                                      casesLimit =
                                        limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases,
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
            casesLimit,
            complexityLimit,
            randomBehaviour,
            scaleDeflationLevel = None,
            shrinkageIsImproving = _ => true,
            decisionStagesToGuideShrinkage = None
          ) match {
            case (cases, inlinedCaseFiltration) =>
              cases.map { case caseData =>
                TestIntegrationContextImplementation(
                  caze = caseData.caze,
                  caseFailureReporting = (throwable: Throwable) => {
                    shrinkageCasesFromDownstream = Some(
                      if (caseData.decisionStagesInReverseOrder.nonEmpty)
                        shrink(
                          caseData = caseData,
                          throwable = throwable,
                          shrinkageAttemptIndex = 0,
                          scaleDeflationLevel = 0,
                          casesLimit = casesLimit,
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

                    TestIntegrationContextImplementation(
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

      override def testIntegrationContexts()
          : JavaIterator[TestIntegrationContext[Case]] =
        Seq({
          val decisionStages = parseDecisionIndices(recipe)
          val caze           = reproduce(decisionStages)

          TestIntegrationContextImplementation(
            caze = caze,
            caseFailureReporting = { (throwable: Throwable) => },
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
        }: TestIntegrationContext[Case]).asJava.iterator()

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit = {
        val decisionStages = parseDecisionIndices(recipe)
        val reproducedCase = reproduce(decisionStages)

        try {
          consumer(reproducedCase)
        } catch {
          case exception: Throwable =>
            val json = decisionStages.asJson.spaces4

            throw new TrialException(exception) {
              override def provokingCase: Case = reproducedCase

              override def recipe: String = json

              // NOTE: as we are reproducing using a recipe, we can assume that
              // the recipe hash is already stored from back when the recipe was
              // generated.
              override def recipeHash: String = json.hashCode().toHexString
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
