package com.sageserpent.americium
import cats.Traverse
import cats.free.Free
import cats.implicits.*
import com.sageserpent.americium.generation.*
import com.sageserpent.americium.{Trials as ScalaTrials, TrialsApi as ScalaTrialsApi}

import _root_.java.time.Instant
import scala.collection.immutable.SortedMap

class TrialsApiImplementation extends CommonApi with ScalaTrialsApi {
  override def delay[Case](
      delayed: => ScalaTrials[Case]
  ): TrialsImplementation[Case] =
    TrialsImplementation(Free.defer(delayed.generation))

  override def impossible[Case]: TrialsImplementation[Case] = {
    // Rather than defining a new case object and additional logic in the
    // interpreters for `Generation`, just use `FiltrationResult` to model a
    // case that is filtered out from the very start.
    new TrialsImplementation(FiltrationResult(None: Option[Case]))
  }

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
      override def apply(input: BigInt): Byte   = input.toByte
      override def lowerBoundInput: BigInt      = Byte.MinValue
      override def upperBoundInput: BigInt      = Byte.MaxValue
      override def maximallyShrunkInput: BigInt = 0
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
      override def apply(input: BigInt): Int    = input.toInt
      override def lowerBoundInput: BigInt      = lowerBound
      override def upperBoundInput: BigInt      = upperBound
      override def maximallyShrunkInput: BigInt = shrinkageTarget
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
      override def apply(input: BigInt): Long   = input.toLong
      override def lowerBoundInput: BigInt      = lowerBound
      override def upperBoundInput: BigInt      = upperBound
      override def maximallyShrunkInput: BigInt = shrinkageTarget
    })

  override def bigInts(
      lowerBound: BigInt,
      upperBound: BigInt
  ): TrialsImplementation[BigInt] = bigInts(
    lowerBound,
    upperBound,
    if (0 > upperBound)
      upperBound
    else if (0 < lowerBound) lowerBound
    else 0
  )

  override def bigInts(
      lowerBound: BigInt,
      upperBound: BigInt,
      shrinkageTarget: BigInt
  ): TrialsImplementation[BigInt] = stream(new CaseFactory[BigInt] {
    override def apply(input: BigInt): BigInt = input
    override def lowerBoundInput: BigInt      = lowerBound
    override def upperBoundInput: BigInt      = upperBound
    override def maximallyShrunkInput: BigInt = shrinkageTarget
  })

  override def doubles: TrialsImplementation[Double] =
    doubles(Double.MinValue, Double.MaxValue, 0.0)

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
  ): TrialsImplementation[Double] =
    bigDecimals(lowerBound, upperBound, shrinkageTarget).map(_.toDouble)

  override def bigDecimals(
      lowerBound: BigDecimal,
      upperBound: BigDecimal
  ): TrialsImplementation[BigDecimal] = bigDecimals(
    lowerBound,
    upperBound,
    if (0.0 > upperBound)
      upperBound
    else if (0.0 < lowerBound) lowerBound
    else 0.0
  )

  override def bigDecimals(
      lowerBound: BigDecimal,
      upperBound: BigDecimal,
      shrinkageTarget: BigDecimal
  ): TrialsImplementation[BigDecimal] = {
    require(lowerBound <= shrinkageTarget)
    require(shrinkageTarget <= upperBound)

    val imageInterval: BigDecimal = upperBound - lowerBound

    if (0 != imageInterval)
      stream(
        new CaseFactory[BigDecimal] {
          val numberOfSubdivisionsOfDoubleUnity = 100

          lazy val convertedLowerBoundInput: BigDecimal =
            BigDecimal(lowerBoundInput)
          lazy val convertedUpperBoundInput: BigDecimal =
            BigDecimal(upperBoundInput)

          // NOTE: the input side representation of `shrinkageTarget` is
          // anchored to an exact big integer, so that the forward conversion to
          // the image doesn't lose precision.
          lazy val convertedMaximallyShrunkInput: BigDecimal =
            (((shrinkageTarget - lowerBound) * convertedUpperBoundInput + (upperBound - shrinkageTarget) * convertedLowerBoundInput) / imageInterval)
              .setScale(
                0,
                BigDecimal.RoundingMode.HALF_EVEN
              )
              .rounded

          override def apply(input: BigInt): BigDecimal = {
            val convertedInput: BigDecimal = BigDecimal(input)

            // NOTE: because `convertedMaximallyShrunkInput` is anchored to a
            // exact big integer, we split the linear interpolation used to map
            // from input values to image values into two halves to ensure
            // precise mapping of `lowerBoundInput()` -> `lowerBound`,
            // `maximallyShrunkInput()` -> `shrinkageTarget` and
            // `upperBoundInput()` -> `upperBound`.
            input.compareTo(maximallyShrunkInput) match {
              case signed if 0 > signed =>
                // Have to clamp against the lower bound due to precision
                // error...
                lowerBound max
                  (((convertedInput - convertedLowerBoundInput) * shrinkageTarget + (convertedMaximallyShrunkInput - convertedInput) * lowerBound) /
                    (convertedMaximallyShrunkInput - convertedLowerBoundInput))
              case signed if 0 < signed =>
                // Have to clamp against the upper bound due to precision
                // error...
                upperBound min
                  (((convertedInput - convertedMaximallyShrunkInput) * upperBound + (convertedUpperBoundInput - convertedInput) * shrinkageTarget) /
                    (convertedUpperBoundInput - convertedMaximallyShrunkInput))
              case 0 => shrinkageTarget
            }
          }
          override def lowerBoundInput: BigInt =
            BigInt(Long.MinValue) min (lowerBound
              .setScale(
                0,
                BigDecimal.RoundingMode.FLOOR
              )
              .rounded
              .toBigInt * numberOfSubdivisionsOfDoubleUnity)

          override def upperBoundInput: BigInt =
            BigInt(Long.MaxValue) max (upperBound
              .setScale(
                0,
                BigDecimal.RoundingMode.CEILING
              )
              .rounded
              .toBigInt * numberOfSubdivisionsOfDoubleUnity)

          override def maximallyShrunkInput: BigInt =
            convertedMaximallyShrunkInput.toBigInt
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
    override def apply(input: BigInt): Char   = input.toChar
    override def lowerBoundInput: BigInt      = BigInt(lowerBound)
    override def upperBoundInput: BigInt      = BigInt(upperBound)
    override def maximallyShrunkInput: BigInt = BigInt(shrinkageTarget)
  })

  override def instants: TrialsImplementation[Instant] =
    longs.map(Instant.ofEpochMilli)

  override def longs: TrialsImplementation[Long] = streamLegacy(identity)

  def stream[Case](
      caseFactory: CaseFactory[Case]
  ): TrialsImplementation[Case] = new TrialsImplementation(
    Factory(new CaseFactory[Case] {
      require(lowerBoundInput <= maximallyShrunkInput)
      require(maximallyShrunkInput <= upperBoundInput)

      override def apply(input: BigInt): Case = {
        require(lowerBoundInput <= input)
        require(upperBoundInput >= input)
        caseFactory(input)
      }
      override def lowerBoundInput: BigInt = caseFactory.lowerBoundInput
      override def upperBoundInput: BigInt = caseFactory.upperBoundInput
      override def maximallyShrunkInput: BigInt =
        caseFactory.maximallyShrunkInput
    })
  )

  override def streamLegacy[Case](
      factory: Long => Case
  ): TrialsImplementation[Case] = stream(
    new CaseFactory[Case] {
      override def apply(input: BigInt): Case   = factory(input.longValue)
      override val lowerBoundInput: BigInt      = Long.MinValue
      override val upperBoundInput: BigInt      = Long.MaxValue
      override val maximallyShrunkInput: BigInt = 0
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

  override def indexPermutations(
      numberOfIndices: Int
  ): TrialsImplementation[Vector[Int]] =
    indexPermutations(numberOfIndices, numberOfIndices)

  override def indexPermutations(
      numberOfIndices: Int,
      permutationSize: Int
  ): TrialsImplementation[Vector[Int]] = {
    require(0 <= numberOfIndices)
    require(0 to numberOfIndices contains permutationSize)

    def indexPermutations(
        cumulativePermutationSize: Int,
        previouslyChosenItemsAsBinaryTree: RangeOfSlots,
        partialResult: TrialsImplementation[Vector[Int]]
    ): TrialsImplementation[Vector[Int]] = if (
      permutationSize == cumulativePermutationSize
    ) partialResult
    else {
      val exclusiveLimitOnVacantSlotIndex =
        numberOfIndices - cumulativePermutationSize

      integers(0, exclusiveLimitOnVacantSlotIndex - 1).flatMap(
        vacantSlotIndex => {
          val (filledSlot, chosenItemsAsBinaryTree) =
            previouslyChosenItemsAsBinaryTree
              .fillVacantSlotAtIndex(vacantSlotIndex)

          indexPermutations(
            1 + cumulativePermutationSize,
            chosenItemsAsBinaryTree,
            partialResult.map(_ :+ filledSlot)
          )
        }
      )
    }

    indexPermutations(
      0,
      RangeOfSlots.allSlotsAreVacant(numberOfIndices),
      only(Vector.empty)
    )
  }

  override def indexCombinations(
      numberOfIndices: Int,
      combinationSize: Int
  ): TrialsImplementation[Vector[Int]] = {
    require(0 <= numberOfIndices)
    require(0 to numberOfIndices contains combinationSize)

    def indexCombinations(
        cumulativeCombinationSize: Int,
        candidateIndex: Int,
        partialResult: TrialsImplementation[Vector[Int]]
    ): TrialsImplementation[Vector[Int]] = if (
      combinationSize == cumulativeCombinationSize
    ) partialResult
    else {
      val leewayToDiscardCandidateIndex =
        candidateIndex + combinationSize - cumulativeCombinationSize < numberOfIndices

      if (leewayToDiscardCandidateIndex) {
        booleans.flatMap(pick =>
          if (pick)
            indexCombinations(
              1 + cumulativeCombinationSize,
              1 + candidateIndex,
              partialResult.map(_ :+ candidateIndex)
            )
          else
            indexCombinations(
              cumulativeCombinationSize,
              1 + candidateIndex,
              partialResult
            )
        )
      } else
        partialResult.map(_ ++ (candidateIndex until numberOfIndices))
    }

    indexCombinations(0, 0, only(Vector.empty))
  }

  override def pickAlternatelyFrom[X](
      iterables: Iterable[X]*
  ): Trials[List[X]] = complexities.flatMap { complexity =>
    def pickAnItem(
        lazyLists: Seq[LazyList[X]]
    ): Trials[List[X]] = {
      if (lazyLists.isEmpty) only(List.empty)
      else
        indexPermutations(lazyLists.size).flatMap { permutationIndices =>
          val candidateLazyListToPickFrom :: remainders = List.tabulate(
            lazyLists.size
          )(index => lazyLists(permutationIndices(index)))

          resetComplexity(complexity).flatMap(_ =>
            candidateLazyListToPickFrom match {
              case LazyList() =>
                pickAnItem(remainders)
              case pickedItem #:: tailFromPickedStream =>
                pickAnItem(tailFromPickedStream :: remainders).map(
                  pickedItem :: _
                )
            }
          )
        }
    }

    pickAnItem(iterables map (LazyList.from(_)))
  }
}
