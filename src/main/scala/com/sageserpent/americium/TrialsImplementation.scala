package com.sageserpent.americium

import cats.collections.Dequeue
import cats.data.{OptionT, State, StateT}
import cats.free.Free
import cats.free.Free.{liftF, pure}
import cats.syntax.applicative._
import cats.{Eval, ~>}
import com.google.common.collect._
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import com.sageserpent.americium.randomEnrichment.RichRandom
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import _root_.java.lang.{
  Boolean => JavaBoolean,
  Character => JavaCharacter,
  Double => JavaDouble,
  Iterable => JavaIterable,
  Long => JavaLong
}
import _root_.java.time.Instant
import _root_.java.util.function.{
  Consumer,
  Predicate,
  Supplier,
  Function => JavaFunction
}
import _root_.java.util.{
  Optional,
  Comparator => JavaComparator,
  Iterator => JavaIterator,
  Map => JavaMap
}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Random

object TrialsImplementation {
  type DecisionStages   = Dequeue[Decision]
  type Generation[Case] = Free[GenerationOperation, Case]
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

    override def integers: TrialsImplementation[Integer] =
      scalaApi.integers.map(Int.box _)

    override def longs: TrialsImplementation[JavaLong] =
      scalaApi.longs.map(Long.box _)

    override def doubles: TrialsImplementation[JavaDouble] =
      scalaApi.doubles.map(Double.box _)

    override def booleans: TrialsImplementation[JavaBoolean] =
      scalaApi.booleans.map(Boolean.box _)

    override def characters(): TrialsImplementation[JavaCharacter] =
      scalaApi.characters.map(Char.box _)

    override def instants(): TrialsImplementation[Instant] =
      scalaApi.instants

    override def strings(): TrialsImplementation[String] =
      scalaApi.strings
  }

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

    override def integers: TrialsImplementation[Int] = stream(_.hashCode)

    override def longs: TrialsImplementation[Long] = stream(identity)

    override def doubles: TrialsImplementation[Double] =
      stream { input =>
        val betweenZeroAndOne = new Random(input).nextDouble()
        Math.scalb(
          betweenZeroAndOne,
          (input.toDouble * JavaDouble.MAX_EXPONENT / Long.MaxValue).toInt
        )
      }
        .flatMap((zeroOrPositive: Double) =>
          booleans
            .map((negative: Boolean) =>
              if (negative) -zeroOrPositive else zeroOrPositive
            )
        )

    override def booleans: TrialsImplementation[Boolean] =
      choose(true, false)

    override def characters: TrialsImplementation[Char] =
      choose(0 to 0xffff).map((_: Int).toChar)

    override def instants: TrialsImplementation[Instant] =
      longs.map(Instant.ofEpochMilli _)

    override def strings: TrialsImplementation[String] = {
      characters.several[String]
    }
  }

  implicit val decisionStagesEncoder: Encoder[DecisionStages] =
    implicitly[Encoder[List[Decision]]].contramap(_.toList)
  implicit val decisionStagesDecoder: Decoder[DecisionStages] =
    implicitly[Decoder[List[Decision]]].emap(list =>
      Right(Dequeue.apply(list: _*))
    )

  sealed trait Decision

  // Java and Scala API ...

  sealed trait GenerationOperation[Case]

  // Java-only API ...

  private[americium] trait GenerationSupport[+Case] {
    val generation: Generation[_ <: Case]
  }

  // Scala-only API ...

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

  case class ChoiceOf(index: Int) extends Decision

  case class FactoryInputOf(input: Long) extends Decision

  case class Choice[Case](choices: Vector[Case])
      extends GenerationOperation[Case]

  case class Factory[Case](factory: Long => Case)
      extends GenerationOperation[Case]

  // NASTY HACK: as `Free` does not support `filter/withFilter`, reify
  // the optional results of a flat-mapped filtration; the interpreter
  // will deal with these.
  case class FiltrationResult[Case](result: Option[Case])
      extends GenerationOperation[Case]

  trait Builder[-Case, Container] {
    def +=(caze: Case): Unit

    def result(): Container
  }
}

case class TrialsImplementation[+Case](
    override val generation: TrialsImplementation.Generation[_ <: Case]
) extends JavaTrials[Case]
    with Trials[Case] {

  import TrialsImplementation._

  override private[americium] val scalaTrials = this

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

  override def withLimit(
      limit: Int
  ): JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] =
    new JavaTrials.WithLimit[Case] with Trials.WithLimit[Case] {

      // Java-only API ...
      override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
        supplyTo(consumer.accept _)

      // Scala-only API ...
      override def supplyTo(consumer: Case => Unit): Unit = {
        val randomBehaviour = new Random(734874)

        def shrink(
            caze: Case,
            throwable: Throwable,
            decisionStages: DecisionStages,
            factoryShrinkage: Long,
            limit: Int
        ): Unit = {
          if ((Long.MaxValue >> 1) >= factoryShrinkage) {
            val increasedFactoryShrinkage = factoryShrinkage * 2

            cases(
              limit,
              Some(decisionStages.size),
              randomBehaviour,
              increasedFactoryShrinkage
            )
              .foreach {
                case (
                      decisionStagesForPotentialShrunkCase,
                      potentialShrunkCase
                    ) =>
                  try {
                    consumer(potentialShrunkCase)
                  } catch {
                    case throwableFromPotentialShrunkCase: Throwable =>
                      def shrinkDecisionStages(
                          caze: Case,
                          throwable: Throwable,
                          decisionStages: DecisionStages,
                          factoryShrinkage: Long
                      ): Unit = {
                        val numberOfDecisionStages = decisionStages.size

                        if (0 < numberOfDecisionStages) {
                          val reducedNumberOfDecisionStages =
                            numberOfDecisionStages - 1

                          cases(
                            limit,
                            Some(reducedNumberOfDecisionStages),
                            randomBehaviour,
                            factoryShrinkage
                          )
                            .foreach {
                              case (
                                    decisionStagesForPotentialShrunkCase,
                                    potentialShrunkCase
                                  ) =>
                                try {
                                  consumer(potentialShrunkCase)
                                } catch {
                                  case throwableFromPotentialShrunkCase: Throwable =>
                                    shrinkDecisionStages(
                                      potentialShrunkCase,
                                      throwableFromPotentialShrunkCase,
                                      decisionStagesForPotentialShrunkCase,
                                      factoryShrinkage
                                    )
                                }
                            }
                        }

                        // NOTE: there's some voodoo in choosing the exponential scaling factor - if it's too high, say 2,
                        // then the solutions are hardly shrunk at all. If it is unity, then the solutions are shrunk a
                        // bit but can be still involve overly 'large' values, in the sense that the factory input values
                        // are large. This needs finessing, but will do for now...
                        val limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases =
                          (100 * limit / 99) max limit

                        shrink(
                          caze,
                          throwable,
                          decisionStages,
                          factoryShrinkage,
                          limitWithExtraLeewayThatHasBeenObservedToFindBetterShrunkCases
                        )
                      }

                      shrinkDecisionStages(
                        potentialShrunkCase,
                        throwableFromPotentialShrunkCase,
                        decisionStagesForPotentialShrunkCase,
                        increasedFactoryShrinkage
                      )
                  }
              }
          }

          throw new TrialException(throwable) {
            override def provokingCase: Case = caze

            override def recipe: String = decisionStages.asJson.spaces4
          }
        }

        val factoryShrinkage = 1

        cases(limit, None, randomBehaviour, factoryShrinkage).foreach {
          case (decisionStages, caze) =>
            try {
              consumer(caze)
            } catch {
              case throwable: Throwable =>
                shrink(caze, throwable, decisionStages, factoryShrinkage, limit)
            }
        }
      }

      override def asIterator(): JavaIterator[_ <: Case] = {
        val randomBehaviour = new Random(734874)

        val factoryShrinkage = 1

        cases(limit, None, randomBehaviour, factoryShrinkage).map(_._2).asJava
      }
    }

  private def cases(
      limit: Int,
      overridingMaximumNumberOfDecisionStages: Option[Int],
      randomBehaviour: Random,
      factoryShrinkage: Long
  ): Iterator[(DecisionStages, Case)] = {
    require(0 < factoryShrinkage)

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
                input = randomBehaviour.nextLong() / factoryShrinkage
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
          val numberOfDecisionStages = state.decisionStages.size
          val limit = overridingMaximumNumberOfDecisionStages
            .getOrElse(maximumNumberOfDecisionStages)
          if (numberOfDecisionStages < limit)
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
        0 < remainingGap && 0 < starvationCountdown

      override def next(): Option[(DecisionStages, Case)] =
        generation
          .foldMap(interpreter(depth = 0))
          .run(State.initial)
          .value
          .value match {
          case Some((State(decisionStages, multiplicity), caze))
              if potentialDuplicates.add(decisionStages) =>
            numberOfUniqueCasesProduced += 1
            starvationCountdown =
              Math.round(Math.sqrt(starvationCountdown * remainingGap)).toInt
            Some(decisionStages -> caze)
          case _ =>
            starvationCountdown -= 1
            None
        }

      private def remainingGap = limit - numberOfUniqueCasesProduced
    }.collect { case Some(caze) => caze }
  }

  // Java-only API ...
  override def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase]
  ): TrialsImplementation[TransformedCase] = map(transform.apply _)

  override def lists(): TrialsImplementation[ImmutableList[_ <: Case]] =
    several(new Builder[Case, ImmutableList[_ <: Case]] {
      private val underlyingBuilder = ImmutableList.builder[Case]()

      override def +=(caze: Case): Unit = { underlyingBuilder.add(caze) }

      override def result(): ImmutableList[_ <: Case] =
        underlyingBuilder.build()
    })

  override def sets(): TrialsImplementation[ImmutableSet[_ <: Case]] =
    several(new Builder[Case, ImmutableSet[_ <: Case]] {
      private val underlyingBuilder = ImmutableSet.builder[Case]()

      override def +=(caze: Case): Unit = { underlyingBuilder.add(caze) }

      override def result(): ImmutableSet[_ <: Case] =
        underlyingBuilder.build()
    })

  override def sortedSets(
      elementComparator: JavaComparator[_ >: Case]
  ): TrialsImplementation[ImmutableSortedSet[_ <: Case]] =
    several(new Builder[Case, ImmutableSortedSet[_ <: Case]] {
      private val underlyingBuilder: ImmutableSortedSet.Builder[Case] =
        new ImmutableSortedSet.Builder(elementComparator)

      override def +=(caze: Case): Unit = { underlyingBuilder.add(caze) }

      override def result(): ImmutableSortedSet[_ <: Case] =
        underlyingBuilder.build()
    })

  override def maps[Value](
      values: JavaTrials[Value]
  ): TrialsImplementation[ImmutableMap[_ <: Case, Value]] = {
    val annoyingWorkaroundToPreventAmbiguity
        : JavaFunction[Case, JavaTrials[(Case, Value)]] = key =>
      values.map(key -> _)

    flatMap(annoyingWorkaroundToPreventAmbiguity)
      .several[Map[_ <: Case, Value]]
      .map((_: Map[_ <: Case, Value]).asJava)
      .map((mapToWrap: JavaMap[_ <: Case, Value]) =>
        ImmutableMap.copyOf(mapToWrap)
      )
  }

  override def sortedMaps[Value](
      elementComparator: JavaComparator[_ >: Case],
      values: JavaTrials[Value]
  ): TrialsImplementation[ImmutableSortedMap[_ <: Case, Value]] = {
    val annoyingWorkaroundToPreventAmbiguity
        : JavaFunction[Case, JavaTrials[(Case, Value)]] = key =>
      values.map(key -> _)

    flatMap(annoyingWorkaroundToPreventAmbiguity)
      .several[Map[_ <: Case, Value]]
      .map((_: Map[_ <: Case, Value]).asJava)
      .map((mapToWrap: JavaMap[_ <: Case, Value]) =>
        ImmutableSortedMap.copyOf(mapToWrap, elementComparator)
      )
  }

  // Scala-only API ...
  override def map[TransformedCase](
      transform: Case => TransformedCase
  ): TrialsImplementation[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, JavaTrials[TransformedCase]]
  ): TrialsImplementation[TransformedCase] =
    flatMap(step.apply _ andThen (_.scalaTrials))

  override def filter(
      predicate: Predicate[_ >: Case]
  ): TrialsImplementation[Case] =
    filter(predicate.test _)

  override def filter(predicate: Case => Boolean): TrialsImplementation[Case] =
    flatMap((caze: Case) =>
      new TrialsImplementation(
        FiltrationResult(Some(caze).filter(predicate))
      )
    )

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[_ >: Case, Optional[TransformedCase]]
  ): TrialsImplementation[TransformedCase] =
    mapFilter(filteringTransform.apply _ andThen {
      case withPayload if withPayload.isPresent => Some(withPayload.get())
      case _                                    => None
    })

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): TrialsImplementation[TransformedCase] =
    flatMap((caze: Case) =>
      new TrialsImplementation(FiltrationResult(filteringTransform(caze)))
    )

  def this(
      generationOperation: TrialsImplementation.GenerationOperation[Case]
  ) = {
    this(liftF(generationOperation))
  }

  override def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]
  ): TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
    supplyTo(recipe, consumer.accept _)

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

  override def several[Container](implicit
      factory: scala.collection.Factory[Case, Container]
  ): TrialsImplementation[Container] = several(new Builder[Case, Container] {
    val underlyingBuilder = factory.newBuilder

    override def +=(caze: Case): Unit = { underlyingBuilder += caze }

    override def result(): Container = underlyingBuilder.result()
  })

  private def several[Container](
      builderFactory: => Builder[Case, Container]
  ): TrialsImplementation[Container] = {
    def addItem(partialResult: List[Case]): TrialsImplementation[Container] =
      scalaApi.alternate(
        scalaApi.only {
          val builder = builderFactory
          partialResult.foreach(builder += _)
          builder.result()
        },
        flatMap((item: Case) => addItem(item :: partialResult))
      )

    addItem(Nil)
  }
}
