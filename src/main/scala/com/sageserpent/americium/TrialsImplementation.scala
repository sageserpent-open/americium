package com.sageserpent.americium

import cats.data.State
import cats.effect.SyncIO
import cats.free.Free.liftF
import cats.implicits.*
import cats.~>
import com.google.common.collect.Ordering as _
import com.google.common.hash
import com.sageserpent.americium.TrialsApis.scalaApi
import com.sageserpent.americium.TrialsScaffolding.{ShrinkageStop, noStopping}
import com.sageserpent.americium.generation.*
import com.sageserpent.americium.generation.Decision.{
  DecisionStages,
  parseDecisionIndices
}
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.java.TrialsDefaults.{
  defaultComplexityLimit,
  defaultShrinkageAttemptsLimit
}
import com.sageserpent.americium.java.{
  Builder,
  CaseSupplyCycle,
  CasesLimitStrategy,
  CrossApiIterator,
  TestIntegrationContext,
  TrialsScaffolding as JavaTrialsScaffolding,
  TrialsSkeletalImplementation as JavaTrialsSkeletalImplementation
}
import com.sageserpent.americium.storage.RocksDBConnection
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsScaffolding as ScalaTrialsScaffolding,
  TrialsSkeletalImplementation as ScalaTrialsSkeletalImplementation
}
import fs2.Stream as Fs2Stream
import org.apache.commons.text.StringEscapeUtils

import _root_.java.util.Iterator as JavaIterator
import _root_.java.util.function.{Consumer, Function as JavaFunction}
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

  override def javaTrials: TrialsImplementation[Case] = this

  override def withLimit(
      limit: Int
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withLimits(casesLimit = limit)

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
        shrinkageStop: ShrinkageStop[Case],
        validTrialsCheckEnabled: Boolean
    ) extends SupplyToSyntaxSkeletalImplementation[Case] {
      override protected val generation: Generation[_ <: Case] =
        thisTrialsImplementation.generation

      override def reproduce(recipe: String): Case =
        thisTrialsImplementation.reproduce(recipe)

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

      override def withValidTrialsCheck(
          enabled: Boolean
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        copy(validTrialsCheckEnabled = enabled)

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

      override protected def reproduce(
          decisionStages: DecisionStages
      ): Case = thisTrialsImplementation.reproduce(decisionStages)

      protected override def raiseTrialException(
          rocksDbConnection: Option[RocksDBConnection],
          throwable: Throwable,
          caze: Case,
          decisionStages: DecisionStages
      ): StreamedCases = {
        val exception: TrialException =
          trialException(throwable, caze, decisionStages)

        // TODO: suppose this throws an exception? Probably best to
        // just log it and carry on, as the user wants to see a test
        // failure rather than an issue with the database.
        rocksDbConnection.foreach(
          _.recordRecipeHash(exception.recipeHash, exception.recipe)
        )

        Fs2Stream.raiseError[SyncIO](exception)
      }
    }

    SupplyToSyntaxImplementation(
      casesLimitStrategyFactory = casesLimitStrategyFactory,
      complexityLimit = complexityLimit,
      shrinkageAttemptsLimit = shrinkageAttemptsLimit,
      seed = 734874L,
      shrinkageStop = shrinkageStop,
      validTrialsCheckEnabled = true
    )
  }

  // Java and Scala API ...
  override def reproduce(recipe: String): Case =
    reproduce(parseDecisionIndices(recipe))

  private def reproduce(decisionStages: DecisionStages): Case = {
    case class Context(
        decisionStages: DecisionStages,
        complexity: Int,
        nextUniqueId: Int
    ) {
      def uniqueId(): (Context, Int) =
        copy(nextUniqueId = 1 + nextUniqueId) -> nextUniqueId
    }

    type DecisionIndicesContext[Caze] = State[Context, Caze]

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
                decisionStages <- State.get[Context]
                Context(
                  ChoiceOf(decisionIndex) :: remainingDecisionStages,
                  complexity,
                  nextUniqueId
                ) =
                  decisionStages: @unchecked
                _ <- State.set(
                  Context(remainingDecisionStages, 1 + complexity, nextUniqueId)
                )
              } yield choicesByCumulativeFrequency
                .minAfter(1 + decisionIndex)
                .get
                ._2

            case Factory(factory) =>
              for {
                decisionStages <- State.get[Context]
                Context(
                  FactoryInputOf(input) :: remainingDecisionStages,
                  complexity,
                  nextUniqueId
                ) =
                  decisionStages: @unchecked
                _ <- State.set(
                  Context(remainingDecisionStages, 1 + complexity, nextUniqueId)
                )
              } yield factory(input)

            // NOTE: pattern-match only on `Some`, as we are reproducing a case
            // that by dint of being reproduced, must have passed filtration the
            // first time around.
            case FiltrationResult(Some(result)) =>
              result.pure[DecisionIndicesContext]

            case NoteComplexity =>
              State.get[Context].map(_.complexity)

            case ResetComplexity(_) =>
              ().pure[DecisionIndicesContext]

            case UniqueId => State(_.uniqueId())
          }
        }
      }

    generation
      .foldMap(interpreter)
      .runA(Context(decisionStages, complexity = 0, nextUniqueId = 0))
      .value
  }

  private def trialException(
      throwable: Throwable,
      caze: Case,
      decisionStages: DecisionStages
  ) = {
    val json           = Decision.json(decisionStages)
    val compressedJson = Decision.compressedJson(decisionStages)

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

  override def withStrategy(
      casesLimitStrategyFactory: JavaFunction[
        CaseSupplyCycle,
        CasesLimitStrategy
      ]
  ): JavaTrialsScaffolding.SupplyToSyntax[Case]
    with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
    withStrategy(
      casesLimitStrategyFactory = casesLimitStrategyFactory.apply,
      // This is hokey: although the Scala compiler refuses to allow a call to
      // the Scala-API overload of `.withStrategy` using a raw Java function
      // value, without providing an additional argument it regards the call as
      // ambiguous between the Java API and Scala API overloads. Ho-hum.
      defaultComplexityLimit,
      defaultShrinkageAttemptsLimit,
      noStopping
    )

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
        this // Seeding has no effect, as the reproduction is determined entirely by `recipe`.

      override def withComplexityLimit(
          complexityLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // There is no complexity limit, as the reproduction is determined entirely by `recipe`.

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // Shrinkage does not take place, as the reproduction is determined entirely by `recipe`.

      override def withValidTrialsCheck(
          enabled: Boolean
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // There is no valid trials check, as the reproduction is determined entirely by `recipe`.

      // Java-only API ...
      override def withShrinkageStop(
          shrinkageStop: JavaTrialsScaffolding.ShrinkageStop[
            _ >: Case
          ]
      ): JavaTrialsScaffolding.SupplyToSyntax[
        Case
      ] with ScalaTrialsScaffolding.SupplyToSyntax[Case] =
        this // Shrinkage does not take place, as the reproduction is determined entirely by `recipe`.

      override def supplyTo(consumer: Consumer[Case]): Unit =
        supplyTo(consumer.accept)

      override def asIterator(): JavaIterator[Case] with ScalaIterator[Case] =
        CrossApiIterator.from(Seq {
          val decisionStages = parseDecisionIndices(recipe)
          thisTrialsImplementation.reproduce(decisionStages)
        }.iterator)

      override def testIntegrationContexts()
          : CrossApiIterator[TestIntegrationContext[Case]] = {
        CrossApiIterator.from(Seq({
          val decisionStages = parseDecisionIndices(recipe)
          val caze = thisTrialsImplementation.reproduce(decisionStages)

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
            isPartOfShrinkage = false,
            recipe = recipe
          )
        }: TestIntegrationContext[Case]).iterator)
      }

      override def reproduce(recipe: String): Case =
        thisTrialsImplementation.reproduce(recipe)

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
        val reproducedCase = thisTrialsImplementation.reproduce(decisionStages)

        try {
          consumer(reproducedCase)
        } catch {
          case exception: Throwable =>
            throw trialException(exception, reproducedCase, decisionStages)
        }
      }
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
}
