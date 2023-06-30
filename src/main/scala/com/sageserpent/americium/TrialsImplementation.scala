package com.sageserpent.americium

import cats.data.State
import cats.effect.SyncIO
import cats.free.Free.liftF
import cats.implicits.*
import cats.~>
import com.google.common.collect.Ordering as _
import com.google.common.hash
import com.sageserpent.americium.TrialsApis.scalaApi
import com.sageserpent.americium.TrialsScaffolding.ShrinkageStop
import com.sageserpent.americium.generation.*
import com.sageserpent.americium.generation.Decision.{DecisionStages, parseDecisionIndices}
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.java.TrialsScaffolding.OptionalLimits
import com.sageserpent.americium.java.{Builder, CaseSupplyCycle, CasesLimitStrategy, CrossApiIterator, TestIntegrationContext, TrialsScaffolding as JavaTrialsScaffolding, TrialsSkeletalImplementation as JavaTrialsSkeletalImplementation}
import com.sageserpent.americium.{Trials as ScalaTrials, TrialsScaffolding as ScalaTrialsScaffolding, TrialsSkeletalImplementation as ScalaTrialsSkeletalImplementation}
import fs2.Stream as Fs2Stream
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.apache.commons.text.StringEscapeUtils
import org.rocksdb.*

import _root_.java.util.Iterator as JavaIterator
import _root_.java.util.function.{Consumer, Predicate, Function as JavaFunction}
import scala.collection.Iterator as ScalaIterator

object TrialsImplementation {

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

}

case class TrialsImplementation[Case](
    override val generation: Generation[_ <: Case]
) extends ScalaTrialsSkeletalImplementation[Case]
    with JavaTrialsSkeletalImplementation[Case] {
  thisTrialsImplementation =>

  override type SupplySyntaxType = ScalaTrialsScaffolding.SupplyToSyntax[Case]

  override def trials: TrialsImplementation[Case] = this

  override def scalaTrials: TrialsImplementation[Case] = this

  override def javaTrials[CovarianceFudge >: Case]
      : TrialsImplementation[CovarianceFudge] = {
    // NASTY HACK: the Java API can't express covariance, but it
    // does use `Case` in a covariant manner, so let's fudge it...
    this.asInstanceOf[TrialsImplementation[CovarianceFudge]]
  }

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    reproduce(parseDecisionIndices(recipe))

  private def reproduce(decisionStages: DecisionStages): Case = {

    type DecisionIndicesContext[Caze] = State[DecisionStages, Caze]

    // NOTE: unlike the companion interpreter over in
    // `SupplyToSyntaxSkeletalImplementation.cases`, this one has a relatively
    // sane implementation.
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

  override def withLimit(
      limit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(casesLimit = limit)

  override def withStrategy(
      casesLimitStrategyFactory: JavaFunction[
        CaseSupplyCycle,
        CasesLimitStrategy
      ]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withStrategy(
      casesLimitStrategyFactory = casesLimitStrategyFactory,
      OptionalLimits.defaults
    )

  override def withLimits(
      casesLimit: Int,
      optionalLimits: OptionalLimits
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(
      casesLimit = casesLimit,
      complexityLimit = optionalLimits.complexity,
      shrinkageAttemptsLimit = optionalLimits.shrinkageAttempts,
      shrinkageStop = { () =>
        val predicate: Predicate[_ >: Case] =
          JavaTrialsScaffolding.noStopping.build()

        predicate.test _
      }
    )

  override def withStrategy(
      casesLimitStrategyFactory: JavaFunction[
        CaseSupplyCycle,
        CasesLimitStrategy
      ],
      optionalLimits: OptionalLimits
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withStrategy(
      casesLimitStrategyFactory = casesLimitStrategyFactory.apply,
      complexityLimit = optionalLimits.complexity,
      shrinkageAttemptsLimit = optionalLimits.shrinkageAttempts,
      shrinkageStop = { () =>
        val predicate: Predicate[_ >: Case] =
          JavaTrialsScaffolding.noStopping.build()

        predicate.test _
      }
    )

  override def withLimits(
      casesLimit: Int,
      optionalLimits: OptionalLimits,
      shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[_ >: Case]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(
      casesLimit = casesLimit,
      complexityLimit = optionalLimits.complexity,
      shrinkageAttemptsLimit = optionalLimits.shrinkageAttempts,
      shrinkageStop = { () =>
        val predicate = shrinkageStop.build()

        predicate.test _
      }
    )

  override def withStrategy(
      casesLimitStrategyFactory: JavaFunction[
        CaseSupplyCycle,
        CasesLimitStrategy
      ],
      optionalLimits: OptionalLimits,
      shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[_ >: Case]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withStrategy(
      casesLimitStrategyFactory = casesLimitStrategyFactory.apply,
      complexityLimit = optionalLimits.complexity,
      shrinkageAttemptsLimit = optionalLimits.shrinkageAttempts,
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
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] = {
    val simpleStrategyFactory: CaseSupplyCycle => CasesLimitStrategy = _ =>
      new CasesLimitStrategy {
        private var starvationCountdown: Int         = casesLimit
        private var backupOfStarvationCountdown      = 0
        private var numberOfUniqueCasesProduced: Int = 0

        override def moreToDo() =
          0 < remainingGap() && 0 < starvationCountdown

        override def noteRejectionOfCase() = {
          numberOfUniqueCasesProduced -= 1
          starvationCountdown = backupOfStarvationCountdown - 1
        }

        override def noteEmissionOfCase(): Unit = {
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

        override def noteStarvation(): Unit = {
          starvationCountdown -= 1
        }

        private def remainingGap() =
          casesLimit - numberOfUniqueCasesProduced
      }

    withStrategy(
      simpleStrategyFactory,
      complexityLimit,
      shrinkageAttemptsLimit,
      shrinkageStop
    )
  }

  override def withStrategy(
      casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy,
      complexityLimit: Int,
      shrinkageAttemptsLimit: Int,
      shrinkageStop: ShrinkageStop[
        Case
      ]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] = {
    case class SupplyToSyntaxImplementation(
        casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy,
        complexityLimit: Int,
        shrinkageAttemptsLimit: Int,
        seed: Long,
        shrinkageStop: ShrinkageStop[Case]
    ) extends SupplyToSyntaxSkeletalImplementation[Case] {
      override protected val generation: Generation[_ <: Case] =
        thisTrialsImplementation.generation

      override protected def reproduce(
          decisionStages: DecisionStages
      ): Case = thisTrialsImplementation.reproduce(decisionStages)

      protected override def raiseTrialException(
          rocksDb: Option[(RocksDB, ColumnFamilyHandle)]
      )(
          throwable: Throwable,
          caze: Case,
          decisionStages: DecisionStages
      ): StreamedCases = {
        val exception: TrialException =
          trialException(throwable, caze, decisionStages)

        // TODO: suppose this throws an exception? Probably best to
        // just log it and carry on, as the user wants to see a test
        // failure rather than an issue with the database.
        rocksDb.foreach { case (rocksDb, columnFamilyForRecipeHashes) =>
          rocksDb.put(
            columnFamilyForRecipeHashes,
            exception.recipeHash.map(_.toByte).toArray,
            exception.recipe.map(_.toByte).toArray
          )
        }

        Fs2Stream.raiseError[SyncIO](exception)
      }

      override def withSeed(
          seed: Long
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(seed = seed)

      override def withComplexityLimit(
          complexityLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(complexityLimit = complexityLimit)

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(shrinkageAttemptsLimit = shrinkageAttemptsLimit)

      // Java-only API ...
      override def withShrinkageStop(
          shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[
            _ >: Case
          ]
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(shrinkageStop = { () =>
          val predicate = shrinkageStop.build()

          predicate.test _
        })

      // Scala-only API ...
      override def withShrinkageStop(
          shrinkageStop: ShrinkageStop[Case]
      ): JavaTrialsScaffolding.SupplyToSyntax[Case]
        with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(shrinkageStop = shrinkageStop)
    }

    SupplyToSyntaxImplementation(
      casesLimitStrategyFactory = casesLimitStrategyFactory,
      complexityLimit = complexityLimit,
      shrinkageAttemptsLimit = shrinkageAttemptsLimit,
      seed = 734874L,
      shrinkageStop = shrinkageStop
    )
  }

  private def trialException(
      throwable: Throwable,
      caze: Case,
      decisionStages: DecisionStages
  ) = {
    val json           = decisionStages.asJson.spaces4
    val compressedJson = decisionStages.asJson.noSpaces

    val jsonHashInHexadecimal = hash.Hashing
      .murmur3_128()
      .hashUnencodedChars(json)
      .toString

    val trialException = new TrialException(throwable) {
      override def provokingCase: Case = caze

      override def recipe: String = json

      override def escapedRecipe: String =
        StringEscapeUtils.escapeJava(compressedJson)

      override def recipeHash: String = jsonHashInHexadecimal
    }
    trialException
  }
  def this(
      generationOperation: GenerationOperation[Case]
  ) = {
    this(liftF(generationOperation))
  }

  def withRecipe(
      recipe: String
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    new JavaTrialsScaffolding.SupplyToSyntax[Case]
      with ScalaTrialsScaffolding.SupplyToSyntax[Case] {
      override def withSeed(
          seed: Long
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // Seeding has no effect, as the reproduction is deterministic according to `recipe`.

      override def withComplexityLimit(
          complexityLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // There is no complexity limit, as the reproduction is deterministic according to `recipe`.

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // Shrinkage does not take place when reproducing a test case.

      // Java-only API ...
      override def withShrinkageStop(
          shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[
            _ >: Case
          ]
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // Shrinkage does not take place when reproducing a test case.

      override def supplyTo(consumer: Consumer[Case]): Unit =
        supplyTo(consumer.accept)

      override def asIterator: JavaIterator[Case] with ScalaIterator[Case] =
        CrossApiIterator.from(Seq {
          val decisionStages = parseDecisionIndices(recipe)
          reproduce(decisionStages)
        }.iterator)

      override def testIntegrationContexts
          : JavaIterator[TestIntegrationContext[Case]]
            with ScalaIterator[TestIntegrationContext[Case]] =
        CrossApiIterator.from(Seq({
          val decisionStages = parseDecisionIndices(recipe)
          val caze           = reproduce(decisionStages)

          TestIntegrationContextImplementation[Case](
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
        }: TestIntegrationContext[Case]).iterator)

      // Scala-only API ...
      override def withShrinkageStop(
          shrinkageStop: ShrinkageStop[
            Case
          ]
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] = this

      override def supplyTo(consumer: Case => Unit): Unit = {
        val decisionStages = parseDecisionIndices(recipe)
        val reproducedCase = reproduce(decisionStages)

        try {
          consumer(reproducedCase)
        } catch {
          case exception: Throwable =>
            throw trialException(exception, reproducedCase, decisionStages)
        }
      }
    }

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
}
