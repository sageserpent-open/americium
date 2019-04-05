package com.sageserpent.americium

object BargainBasement {
  def numberOfPermutations(originalSize: Int, permutationSize: Int) = {
    require(originalSize >= 0)
    require(permutationSize >= 0)

    if (permutationSize > originalSize)
      0
    else {
      val numberOfItemsLeftOutOfPermutation = originalSize - permutationSize
      def productOfPartialResultAndNumberOfSubpermutations(
          originalSize: Int,
          partialResult: Int): Int =
        if (originalSize == numberOfItemsLeftOutOfPermutation)
          partialResult
        else
          productOfPartialResultAndNumberOfSubpermutations(
            (originalSize - 1),
            (originalSize * partialResult))
      productOfPartialResultAndNumberOfSubpermutations(originalSize, 1)
    }
  }

  def factorial(x: Int) =
    numberOfPermutations(x, x)

  def numberOfCombinations(originalSize: Int, combinationSize: Int) = {
    val unpickedSize = originalSize - combinationSize
    if (combinationSize < unpickedSize)
      numberOfPermutations(originalSize, combinationSize) / factorial(
        combinationSize)
    else
      numberOfPermutations(originalSize, unpickedSize) / factorial(
        unpickedSize)
  }
}
