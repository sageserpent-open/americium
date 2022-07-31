package com.sageserpent.americium
import cats.Traverse
import cats.free.Free
import cats.implicits.*
import com.sageserpent.americium.TrialsImplementation.{
  Choice,
  NoteComplexity,
  ResetComplexity
}
import com.sageserpent.americium.java.CaseFactory
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsApi as ScalaTrialsApi
}

import _root_.java.lang.Double as JavaDouble
import _root_.java.time.Instant
import scala.collection.immutable.SortedMap
import scala.util.Random

class TrialsApiImplementation extends CommonApi with ScalaTrialsApi {
  override def delay[Case](
      delayed: => ScalaTrials[Case]
  ): TrialsImplementation[Case] =
    TrialsImplementation(Free.defer(delayed.generation))

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

  override def bytes: TrialsImplementation[Byte] =
    stream(new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = 0L
    })

  override def integers: TrialsImplementation[Int] =
    integers(Int.MinValue, Int.MaxValue)

  override def integers(
      lowerBound: Int,
      upperBound: Int
  ): TrialsImplementation[Int] = integers(
    lowerBound,
    upperBound,
    if (0 > upperBound)
      upperBound
    else if (0 < lowerBound) lowerBound
    else 0
  )

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
    integers(0, Int.MaxValue)

  override def nonNegativeLongs: TrialsImplementation[Long] =
    longs(0, Long.MaxValue)

  override def longs(
      lowerBound: Long,
      upperBound: Long
  ): TrialsImplementation[Long] = longs(
    lowerBound,
    upperBound,
    if (0L > upperBound)
      upperBound
    else if (0L < lowerBound) lowerBound
    else 0L
  )

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

  override def doubles(
      lowerBound: Double,
      upperBound: Double
  ): TrialsImplementation[Double] = doubles(
    lowerBound,
    upperBound,
    if (0.0 > upperBound)
      upperBound
    else if (0.0 < lowerBound) lowerBound
    else 0.0
  )

  override def doubles(
      lowerBound: Double,
      upperBound: Double,
      shrinkageTarget: Double
  ): TrialsImplementation[Double] = {
    require(lowerBound <= shrinkageTarget)
    require(shrinkageTarget <= upperBound)

    val convertedLowerBound      = BigDecimal(lowerBound)
    val convertedUpperBound      = BigDecimal(upperBound)
    val convertedShrinkageTarget = BigDecimal(shrinkageTarget)

    val imageInterval: BigDecimal = convertedUpperBound - convertedLowerBound

    if (0 != imageInterval)
      stream(
        new CaseFactory[Double] {

          override def apply(input: Long): Double = {
            val convertedInput: BigDecimal = BigDecimal(input)
            val convertedMaximallyShrunkInput: BigDecimal =
              BigDecimal(maximallyShrunkInput)

            input.compareTo(maximallyShrunkInput) match {
              case signed if 0 > signed =>
                lowerBound max (((convertedInput - lowerBoundInput()) * convertedShrinkageTarget + (convertedMaximallyShrunkInput - convertedInput) * convertedLowerBound) / (convertedMaximallyShrunkInput - lowerBoundInput())).toDouble
              case signed if 0 < signed =>
                upperBound min (((convertedInput - convertedMaximallyShrunkInput) * convertedUpperBound + (upperBoundInput() - convertedInput) * convertedShrinkageTarget) / (upperBoundInput() - convertedMaximallyShrunkInput)).toDouble
              case 0 => shrinkageTarget
            }
          }
          override def lowerBoundInput(): Long = Long.MinValue
          override def upperBoundInput(): Long = Long.MaxValue
          override val maximallyShrunkInput =
            (((convertedShrinkageTarget - convertedLowerBound) * upperBoundInput() + (convertedUpperBound - convertedShrinkageTarget) * lowerBoundInput()) / imageInterval)
              .setScale(
                0,
                BigDecimal.RoundingMode.HALF_EVEN
              )
              .rounded
              .toLong
        }
      )
    else only(shrinkageTarget)
  }

  override def booleans: TrialsImplementation[Boolean] =
    choose(true, false)

  override def choose[Case](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): TrialsImplementation[Case] = choose(
    firstChoice +: secondChoice +: otherChoices
  )

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

  override def longs: TrialsImplementation[Long] = streamLegacy(identity)

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

  override def instants(
      lowerBound: Instant,
      upperBound: Instant
  ): TrialsImplementation[Instant] =
    longs(lowerBound.toEpochMilli, upperBound.toEpochMilli).map(
      Instant.ofEpochMilli
    )

  override def instants(
      lowerBound: Instant,
      upperBound: Instant,
      shrinkageTarget: Instant
  ): TrialsImplementation[Instant] = longs(
    lowerBound.toEpochMilli,
    upperBound.toEpochMilli,
    shrinkageTarget.toEpochMilli
  ).map(Instant.ofEpochMilli)

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

  override def strings: TrialsImplementation[String] = {
    characters.several[String]
  }

  override def characters: TrialsImplementation[Char] =
    choose(Char.MinValue to Char.MaxValue)

  override def choose[Case](
      choices: Iterable[Case]
  ): TrialsImplementation[Case] =
    new TrialsImplementation(
      Choice(SortedMap.from(LazyList.from(1).zip(choices)))
    )
}
