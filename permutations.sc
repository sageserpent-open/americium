import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.{RangeOfSlots, Trials}

import scala.collection.immutable.{SortedMap, SortedSet}

def permutationIndices3(size: Int): Trials[Vector[Int]] = {
  require(0 <= size)

  def permutationIndices(
      exclusiveLimitOnVacantSlotIndex: Int,
      previouslyChosenItemsAsBinaryTree: RangeOfSlots
  ): Trials[Vector[Int]] = if (0 == exclusiveLimitOnVacantSlotIndex)
    api.only(Vector.empty)
  else
    for {
      vacantSlotIndex <- api.integers(0, exclusiveLimitOnVacantSlotIndex - 1)
      (filledSlot, chosenItemsAsBinaryTree) = previouslyChosenItemsAsBinaryTree
        .fillVacantSlotAtIndex(vacantSlotIndex)
      permutationTail <- permutationIndices(
        exclusiveLimitOnVacantSlotIndex - 1,
        chosenItemsAsBinaryTree
      )
    } yield filledSlot +: permutationTail

  permutationIndices(size, RangeOfSlots.allSlotsAreVacant(size))
}

for {
  numberOfIndices <- 0 to 4
  combinationSize <- 0 to numberOfIndices
} {
  api
    .indexCombinations(numberOfIndices, combinationSize)
    .withLimit(30)
    .supplyTo(println)
  println("+++++++++++++")
}

for {
  numberOfIndices <- 0 to 4
  permutationSize <- 0 to numberOfIndices
} {
  api
    .indexPermutations(numberOfIndices, permutationSize)
    .withLimit(30)
    .supplyTo(println)
  println("------------")
}

def permutationIndices2(numberOfElements: Int): Trials[Seq[Int]] = {
  def permutationIndices(
      lowerBoundInclusive: Int,
      upperBoundExclusive: Int
  ): Trials[Seq[Int]] = {
    require(
      lowerBoundInclusive <= upperBoundExclusive
    ) // Allow an empty range.

    if (lowerBoundInclusive == upperBoundExclusive) api.only(Seq.empty)
    else {
      val partitions =
        api.integers(lowerBoundInclusive, upperBoundExclusive - 1)

      partitions.flatMap { partition =>
        permutationIndices(lowerBoundInclusive, partition)
          .flatMap(lowerSection =>
            permutationIndices(1 + partition, upperBoundExclusive).flatMap(
              upperSection =>
                api.integers(1, 6).map {
                  case 1 => lowerSection ++ (partition +: upperSection)
                  case 2 => upperSection ++ (partition +: lowerSection)
                  case 3 => partition +: (lowerSection ++ upperSection)
                  case 4 => partition +: (upperSection ++ lowerSection)
                  case 5 => (lowerSection ++ upperSection) :+ partition
                  case 6 => (upperSection ++ lowerSection) :+ partition
                }
            )
          )
      }
    }
  }

  permutationIndices(0, numberOfElements)
}

def permutationIndices(numberOfElements: Int): Trials[Seq[Int]] = {
  require(0 <= numberOfElements)

  numberOfElements match {
    case 0 => api.only(Seq.empty)
    case 1 => api.only(Seq(0))
    case _ =>
      api.integers(0, numberOfElements - 1).flatMap { partitionIndex =>
        permutationIndices(numberOfElements - 1).map { indices =>
          partitionIndex +: indices
            .map(index => if (partitionIndex <= index) 1 + index else index)
        }
      }
  }
}

val permutations: Trials[SortedMap[Int, Int]] =
  api.only(15).flatMap { size =>
    val sourceCollection = 0 until size

    api
      .indexPermutations(size)
      .map(indices => {
        val permutation = SortedMap.from(indices.zip(sourceCollection))

        assert(permutation.size == size)

        assert(SortedSet.from(permutation.values).toSeq == sourceCollection)

        permutation
      })
  }

try {
  permutations
    .withLimit(15)
    .supplyTo { permuted =>
      Trials.whenever(4 < permuted.size) {
        permuted.values zip permuted.values.tail foreach { case (left, right) =>
          if (left > right) {
            println((left, right, permuted.values))

            throw new RuntimeException
          }
        }
      }
    }
} catch {
  case exception: permutations.TrialException =>
    println(exception)
}
