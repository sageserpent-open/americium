package com.sageserpent.americium
import cats.Traverse
import cats.free.Free
import cats.free.Free.pure
import cats.implicits.*
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.TrialsImplementation.*
import com.sageserpent.americium.java.CaseFactory
import com.sageserpent.americium.{Trials as ScalaTrials, TrialsApi as ScalaTrialsApi}

import _root_.java.lang.{Boolean as JavaBoolean, Byte as JavaByte, Character as JavaCharacter, Double as JavaDouble, Integer as JavaInteger, Iterable as JavaIterable, Long as JavaLong}
import _root_.java.time.Instant
import _root_.java.util.function.{Supplier, Function as JavaFunction}
import _root_.java.util.{List as JavaList, Map as JavaMap}
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters.*
import scala.util.Random

object TrialsApiImplementation {
  abstract class CommonApi {

    def only[Case](onlyCase: Case): TrialsImplementation[Case] =
      TrialsImplementation(pure[GenerationOperation, Case](onlyCase))

    def choose[Case](
        firstChoice: Case,
        secondChoice: Case,
        otherChoices: Case*
    ): TrialsImplementation[Case] =
      scalaApi.choose(firstChoice +: secondChoice +: otherChoices)

    def stream[Case](
        caseFactory: CaseFactory[Case]
    ): TrialsImplementation[Case] = new TrialsImplementation(
      Factory(new CaseFactory[Case] {
        override def apply(input: Long): Case = {
          require(lowerBoundInput() <= input)
          require(upperBoundInput() >= input)
          caseFactory(input)
        }
        override def lowerBoundInput(): Long = caseFactory.lowerBoundInput()
        override def upperBoundInput(): Long = caseFactory.upperBoundInput()
        override def maximallyShrunkInput(): Long =
          caseFactory.maximallyShrunkInput()
      })
    )
  }

  class ScalaTrialsApiImplementation extends CommonApi with ScalaTrialsApi {
    override def delay[Case](
        delayed: => ScalaTrials[Case]
    ): TrialsImplementation[Case] =
      TrialsImplementation(Free.defer(delayed.generation))

    override def choose[Case](
        choices: Iterable[Case]
    ): TrialsImplementation[Case] =
      new TrialsImplementation(
        Choice(SortedMap.from(LazyList.from(1).zip(choices)))
      )

    override def chooseWithWeights[Case](
        firstChoice: (Int, Case),
        secondChoice: (Int, Case),
        otherChoices: (Int, Case)*
    ): TrialsImplementation[Case] =
      chooseWithWeights(firstChoice +: secondChoice +: otherChoices)

    override def chooseWithWeights[Case](
        choices: Iterable[(Int, Case)]
    ): TrialsImplementation[Case] =
      new TrialsImplementation(
        Choice(choices.unzip match {
          case (weights, plainChoices) =>
            SortedMap.from(
              weights
                .scanLeft(0) {
                  case (cumulativeWeight, weight) if 0 < weight =>
                    cumulativeWeight + weight
                  case (_, weight) =>
                    throw new IllegalArgumentException(
                      s"Weight $weight amongst provided weights of $weights must be greater than zero"
                    )
                }
                .drop(1)
                .zip(plainChoices)
            )
        })
      )

    override def alternate[Case](
        firstAlternative: ScalaTrials[Case],
        secondAlternative: ScalaTrials[Case],
        otherAlternatives: ScalaTrials[Case]*
    ): TrialsImplementation[Case] =
      alternate(
        firstAlternative +: secondAlternative +: otherAlternatives
      )

    override def alternate[Case](
        alternatives: Iterable[ScalaTrials[Case]]
    ): TrialsImplementation[Case] =
      choose(alternatives).flatMap(identity[ScalaTrials[Case]])

    override def alternateWithWeights[Case](
        firstAlternative: (Int, ScalaTrials[Case]),
        secondAlternative: (Int, ScalaTrials[Case]),
        otherAlternatives: (Int, ScalaTrials[Case])*
    ): TrialsImplementation[Case] = alternateWithWeights(
      firstAlternative +: secondAlternative +: otherAlternatives
    )

    override def alternateWithWeights[Case](
        alternatives: Iterable[
          (Int, ScalaTrials[Case])
        ]
    ): TrialsImplementation[Case] =
      chooseWithWeights(alternatives).flatMap(identity[ScalaTrials[Case]])

    override def sequences[Case, Sequence[_]: Traverse](
        sequenceOfTrials: Sequence[ScalaTrials[Case]]
    )(implicit
        factory: collection.Factory[Case, Sequence[Case]]
    ): ScalaTrials[Sequence[Case]] = sequenceOfTrials.sequence

    override def complexities: TrialsImplementation[Int] =
      new TrialsImplementation(NoteComplexity)

    def resetComplexity(complexity: Int): TrialsImplementation[Unit] =
      new TrialsImplementation(ResetComplexity(complexity))

    override def streamLegacy[Case](
        factory: Long => Case
    ): TrialsImplementation[Case] = stream(
      new CaseFactory[Case] {
        override def apply(input: Long): Case   = factory(input)
        override val lowerBoundInput: Long      = Long.MinValue
        override val upperBoundInput: Long      = Long.MaxValue
        override val maximallyShrunkInput: Long = 0L
      }
    )

    override def bytes: TrialsImplementation[Byte] =
      stream(new CaseFactory[Byte] {
        override def apply(input: Long): Byte     = input.toByte
        override def lowerBoundInput(): Long      = Byte.MinValue
        override def upperBoundInput(): Long      = Byte.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def integers: TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = Int.MinValue
        override def upperBoundInput(): Long      = Int.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def integers(
        lowerBound: Int,
        upperBound: Int
    ): TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int = input.toInt
        override def lowerBoundInput(): Long = lowerBound
        override def upperBoundInput(): Long = upperBound
        override def maximallyShrunkInput(): Long = if (0L > upperBound)
          upperBound
        else if (0L < lowerBound) lowerBound
        else 0L
      })

    override def integers(
        lowerBound: Int,
        upperBound: Int,
        shrinkageTarget: Int
    ): TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = lowerBound
        override def upperBoundInput(): Long      = upperBound
        override def maximallyShrunkInput(): Long = shrinkageTarget
      })

    override def nonNegativeIntegers: TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = 0L
        override def upperBoundInput(): Long      = Int.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def longs: TrialsImplementation[Long] = streamLegacy(identity)

    override def longs(
        lowerBound: Long,
        upperBound: Long
    ): TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long = input
        override def lowerBoundInput(): Long  = lowerBound
        override def upperBoundInput(): Long  = upperBound
        override def maximallyShrunkInput(): Long = if (0L > upperBound)
          upperBound
        else if (0L < lowerBound) lowerBound
        else 0L
      })

    override def longs(
        lowerBound: Long,
        upperBound: Long,
        shrinkageTarget: Long
    ): TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = lowerBound
        override def upperBoundInput(): Long      = upperBound
        override def maximallyShrunkInput(): Long = shrinkageTarget
      })

    override def nonNegativeLongs: TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = 0L
        override def upperBoundInput(): Long      = Long.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def doubles: TrialsImplementation[Double] =
      streamLegacy { input =>
        val betweenZeroAndOne = new Random(input).nextDouble()
        Math.scalb(
          betweenZeroAndOne,
          (input.toDouble * JavaDouble.MAX_EXPONENT / Long.MaxValue).toInt
        )
      }
        .flatMap(zeroOrPositive =>
          booleans
            .map((negative: Boolean) =>
              if (negative) -zeroOrPositive else zeroOrPositive
            ): ScalaTrials[Double]
        )

    override def booleans: TrialsImplementation[Boolean] =
      choose(true, false)

    override def characters: TrialsImplementation[Char] =
      choose(Char.MinValue to Char.MaxValue)

    override def characters(
        lowerBound: Char,
        upperBound: Char
    ): TrialsImplementation[Char] = choose(lowerBound to upperBound)

    override def characters(
        lowerBound: Char,
        upperBound: Char,
        shrinkageTarget: Char
    ): TrialsImplementation[Char] = stream(new CaseFactory[Char] {
      override def apply(input: Long): Char     = input.toChar
      override def lowerBoundInput(): Long      = lowerBound.toLong
      override def upperBoundInput(): Long      = upperBound.toLong
      override def maximallyShrunkInput(): Long = shrinkageTarget.toLong
    })

    override def instants: TrialsImplementation[Instant] =
      longs.map(Instant.ofEpochMilli)

    override def strings: TrialsImplementation[String] = {
      characters.several[String]
    }
  }

  val scalaApi = new ScalaTrialsApiImplementation()
}
