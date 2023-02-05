package com.sageserpent.americium
import cats.Traverse
import cats.free.Free
import cats.implicits.*
import com.sageserpent.americium.generation.{
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

  override def chooseWithWeights[Case](
      choices: Iterable[(Int, Case)]
  ): TrialsImplementation[Case] =
    new TrialsImplementation(
      Choice(choices.filter(0 != _._1).unzip match {
        case (weights, plainChoices) =>
          SortedMap.from(
            weights
              .scanLeft(0) {
                case (_, weight) if 0 > weight =>
                  throw new IllegalArgumentException(
                    s"Weight $weight amongst provided weights of $weights must be non-negative"
                  )
                case (cumulativeWeight, weight) =>
                  assert(0 < weight)
                  cumulativeWeight + weight
              }
              .drop(1)
              .zip(plainChoices)
          )
      })
    )

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

  override def choose[Case](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): TrialsImplementation[Case] = choose(
    firstChoice +: secondChoice +: otherChoices
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
          lazy val convertedLowerBoundInput: BigDecimal =
            BigDecimal(lowerBoundInput())
          lazy val convertedUpperBoundInput: BigDecimal =
            BigDecimal(upperBoundInput())

          // NOTE: the input side representation of `shrinkageTarget` is
          // anchored to an exact long integer, so that the forward conversion
          // to the image doesn't lose precision.
          lazy val convertedMaximallyShrunkInput: BigDecimal =
            (((convertedShrinkageTarget - convertedLowerBound) * convertedUpperBoundInput + (convertedUpperBound - convertedShrinkageTarget) * convertedLowerBoundInput) / imageInterval)
              .setScale(
                0,
                BigDecimal.RoundingMode.HALF_EVEN
              )
              .rounded

          override def apply(input: Long): Double = {
            val convertedInput: BigDecimal = BigDecimal(input)

            // NOTE: because `convertedMaximallyShrunkInput` is anchored to a
            // exact long, we split the linear interpolation used to map from
            // input values to image values into two halves to ensure precise
            // mapping of `lowerBoundInput()` -> `lowerBound`,
            // `maximallyShrunkInput()` -> `shrinkageTarget` and
            // `upperBoundInput()` -> `upperBound`.
            input.compareTo(maximallyShrunkInput) match {
              case signed if 0 > signed =>
                // Have to clamp against the lower bound due to precision
                // error...
                lowerBound max
                  (((convertedInput - convertedLowerBoundInput) * convertedShrinkageTarget + (convertedMaximallyShrunkInput - convertedInput) * convertedLowerBound) /
                    (convertedMaximallyShrunkInput - convertedLowerBoundInput)).toDouble
              case signed if 0 < signed =>
                // Have to clamp against the upper bound due to precision
                // error...
                upperBound min
                  (((convertedInput - convertedMaximallyShrunkInput) * convertedUpperBound + (convertedUpperBoundInput - convertedInput) * convertedShrinkageTarget) /
                    (convertedUpperBoundInput - convertedMaximallyShrunkInput)).toDouble
              case 0 => shrinkageTarget
            }
          }
          override def lowerBoundInput(): Long = Long.MinValue
          override def upperBoundInput(): Long = Long.MaxValue
          override def maximallyShrunkInput(): Long =
            convertedMaximallyShrunkInput.toLong
        }
      )
    else only(shrinkageTarget)
  }

  override def characters(
      lowerBound: Char,
      upperBound: Char
  ): TrialsImplementation[Char] = choose(lowerBound to upperBound)

  override def choose[Case](
      choices: Iterable[Case]
  ): TrialsImplementation[Case] =
    new TrialsImplementation(
      Choice(SortedMap.from(LazyList.from(1).zip(choices)))
    )

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

  override def strings: TrialsImplementation[String] = {
    characters.several[String]
  }

  override def characters: TrialsImplementation[Char] =
    choose(Char.MinValue to Char.MaxValue)
}
