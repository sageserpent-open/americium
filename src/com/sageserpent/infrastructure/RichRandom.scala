package com.sageserpent.infrastructure
import scala.util.Random

class RichRandom(random: Random) {
  def chooseAnyNumberFromZeroToOneLessThan(exclusiveLimit: Int) = random.nextInt(exclusiveLimit)

  def chooseAnyNumberFromOneTo(inclusiveLimit: Int) =
    1 + chooseAnyNumberFromZeroToOneLessThan(inclusiveLimit)

  def headsItIs() = random.nextBoolean()

  def buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(exclusiveLimit: Int): Stream[Int] = {
    require(0 <= exclusiveLimit)
    
    // TODO - use either the swapping approach, the trie approach or the partitions of gaps and inclusions approach.

    exclusiveLimit match {
      case 0 => Stream.Empty

      case 1 => Stream(0)

      case _ => {
        val chosenItem = chooseAnyNumberFromZeroToOneLessThan(exclusiveLimit)

        chosenItem #:: buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(exclusiveLimit - 1).map(previouslyChosenItem => (chosenItem + 1 + previouslyChosenItem) % exclusiveLimit)
      }
    }
  }

  def ChooseSeveralOf[X](candidates: Seq[X], numberToChoose: Int) = {
    require(numberToChoose <= candidates.size)

    // TODO.
  }
}