package com.sageserpent.infrastructure

import scala.util.Random

class RichRandom(random: Random) {
  def chooseAnyNumberFromZeroToOneLessThan(exclusiveLimit: Int) = random.nextInt(exclusiveLimit)

  def chooseAnyNumberFromOneTo(inclusiveLimit: Int) =
    1 + chooseAnyNumberFromZeroToOneLessThan(inclusiveLimit)

  def buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(exclusiveLimit: Int): Stream[Int] = {
    require(0 <= exclusiveLimit)

    abstract class BinaryTreeNode {
      def inclusiveLowerBoundForAllItemsInSubtree: Option[Int]

      def exclusiveUpperBoundForAllItemsInSubtree: Option[Int]

      def numberOfInteriorNodesInSubtree: Int

      def numberOfItemsInSubtree: Int

      def numberOfVacantSlotsInSubtreeWithinRange(inclusiveLowerBound: Int, exclusiveUpperBound: Int): Int = {
        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound <= exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        this match {
          case thisAsInteriorNode @ InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            require(thisAsInteriorNode.inclusiveLowerBoundForAllItemsInSubtree match { case Some(inclusiveLowerBoundForAllItemsInSubtree) => inclusiveLowerBound <= inclusiveLowerBoundForAllItemsInSubtree })
            require(thisAsInteriorNode.exclusiveUpperBoundForAllItemsInSubtree match { case Some(exclusiveUpperBoundForAllItemsInSubtree) => exclusiveUpperBoundForAllItemsInSubtree <= exclusiveUpperBound })

          case EmptySubtree =>
            assume(inclusiveLowerBoundForAllItemsInSubtree.isEmpty)
            assume(exclusiveUpperBoundForAllItemsInSubtree.isEmpty)
        }

        exclusiveUpperBound - inclusiveLowerBound - numberOfItemsInSubtree
      }

      def addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem: Int, inclusiveLowerBound: Int, exclusiveUpperBound: Int): (BinaryTreeNode, Int)
      def addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem: Int): (BinaryTreeNode, Int) = addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem, 0, exclusiveLimit)
    }

    case class InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) extends BinaryTreeNode {
      require(lowerBoundForItemRange <= upperBoundForItemRange)

      lesserSubtree match {
        case InteriorNode(_, upperBoundForItemRangeFromLesserSubtree, _, _) => require(upperBoundForItemRangeFromLesserSubtree + 1 < lowerBoundForItemRange)
        case _ => ()
      }

      greaterSubtree match {
        case InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, _, _, _) => require(upperBoundForItemRange + 1 < lowerBoundForItemRangeFromGreaterSubtree)
        case _ => ()
      }

      def this(singleItem: Int) = this(singleItem, singleItem, EmptySubtree, EmptySubtree)

      val numberOfItemsInRange = 1 + upperBoundForItemRange - lowerBoundForItemRange

      val inclusiveLowerBoundForAllItemsInSubtree = lesserSubtree.inclusiveLowerBoundForAllItemsInSubtree orElse (Some(lowerBoundForItemRange))

      val exclusiveUpperBoundForAllItemsInSubtree = greaterSubtree.exclusiveUpperBoundForAllItemsInSubtree orElse (Some(upperBoundForItemRange))

      val numberOfInteriorNodesInSubtree = 1 + lesserSubtree.numberOfInteriorNodesInSubtree + greaterSubtree.numberOfInteriorNodesInSubtree

      val numberOfItemsInSubtree = numberOfItemsInRange + lesserSubtree.numberOfItemsInSubtree + greaterSubtree.numberOfItemsInSubtree

      def addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem: Int, inclusiveLowerBound: Int, exclusiveUpperBound: Int) = {
        require(indexOfVacantSlotAsOrderedByMissingItem >= 0)
        require(indexOfVacantSlotAsOrderedByMissingItem < exclusiveLimit) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
        // decremented for each move to a greater subtree. It does hold however, so is left in as a last line of
        // defence sanity check.

        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        require(inclusiveLowerBound <= lowerBoundForItemRange)
        require(exclusiveUpperBound > upperBoundForItemRange)

        val effectiveIndexAssociatedWithThisInteriorNode = lesserSubtree.numberOfVacantSlotsInSubtreeWithinRange(inclusiveLowerBound, lowerBoundForItemRange)

        def recurseOnLesserSubtree() = {
          val (lesserSubtreeResult, modifiedItemResult) = lesserSubtree.addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem, inclusiveLowerBound, lowerBoundForItemRange)

          (lesserSubtreeResult match {
            case InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, EmptySubtree) if 1 + upperBoundForItemRangeFromLesserSubtree == lowerBoundForItemRange => InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRange, lesserSubtreeFromLesserSubtree, greaterSubtree)

            case InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, greaterSubtreeFromLesserSubtree) =>
              InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, greaterSubtreeFromLesserSubtree, greaterSubtree))

            case _ => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtreeResult, greaterSubtree)
          }) -> modifiedItemResult
        }

        def recurseOnGreaterSubtree() = {
          val (greaterSubtreeResult, modifiedItemResult) = greaterSubtree.addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem - effectiveIndexAssociatedWithThisInteriorNode, 1 + upperBoundForItemRange, exclusiveUpperBound)

          (greaterSubtreeResult match {
            case InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, EmptySubtree, greaterSubtreeFromGreaterSubtree) if 1 + upperBoundForItemRange == lowerBoundForItemRangeFromGreaterSubtree => InteriorNode(lowerBoundForItemRange, upperBoundForItemRangeFromGreaterSubtree, lesserSubtree, greaterSubtreeFromGreaterSubtree)

            case InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, lesserSubtreeFromGreaterSubtree, greaterSubtreeFromGreaterSubtree) => InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, lesserSubtreeFromGreaterSubtree), greaterSubtreeFromGreaterSubtree)

            case _ => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, greaterSubtreeResult)
          }) -> modifiedItemResult
        }

        def lesserSubtreeCanBeConsidered(inclusiveLowerBound: Int): Boolean = inclusiveLowerBound < lowerBoundForItemRange

        def greaterSubtreeCanBeConsidered(exclusiveUpperBound: Int): Boolean = 1 + upperBoundForItemRange < exclusiveUpperBound

        (lesserSubtreeCanBeConsidered(inclusiveLowerBound), greaterSubtreeCanBeConsidered(exclusiveUpperBound)) match {
          case (true, false) => {
            assume(exclusiveUpperBound == exclusiveLimit) // NOTE: in theory this case can occur for other values of 'exclusiveUpperBound', but range-fusion prevents this happening in practice.
            recurseOnLesserSubtree()
          }

          case (false, true) => {
            assume(0 == inclusiveLowerBound) // NOTE: in theory this case can occur for other values of 'inclusiveLowerBound', but range-fusion prevents this happening in practice.
            recurseOnGreaterSubtree()
          }

          case (true, true) => {
            if (0 > indexOfVacantSlotAsOrderedByMissingItem.compare(effectiveIndexAssociatedWithThisInteriorNode)) {
              recurseOnLesserSubtree()
            } else {
              recurseOnGreaterSubtree()
            }
          }
        }
      }
    }

    case object EmptySubtree extends BinaryTreeNode {
      val inclusiveLowerBoundForAllItemsInSubtree = None

      val exclusiveUpperBoundForAllItemsInSubtree = None

      val numberOfInteriorNodesInSubtree = 0

      val numberOfItemsInSubtree = 0

      def addNewItemInTheVacantSlotAtIndex(indexOfVacantSlotAsOrderedByMissingItem: Int, inclusiveLowerBound: Int, exclusiveUpperBound: Int) = {
        require(indexOfVacantSlotAsOrderedByMissingItem >= 0)
        require(indexOfVacantSlotAsOrderedByMissingItem < exclusiveLimit) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
        // decremented for each move to a greater subtree. It does hold however, so is left in as a last line of
        // defence sanity check.

        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        val generatedItem = inclusiveLowerBound + indexOfVacantSlotAsOrderedByMissingItem

        assume(generatedItem < exclusiveUpperBound)

        new InteriorNode(generatedItem) -> generatedItem
      }
    }
    
    var previouslyChosenItemsAsBinaryTree: BinaryTreeNode = EmptySubtree

    def chooseAndRecordUniqueItems(exclusiveLimitOnVacantSlotIndex: Int): Stream[Int] = {
      if (0 == exclusiveLimitOnVacantSlotIndex) {
        Stream.empty
      } else {
        val (chosenItemsAsBinaryTree, chosenItem) = previouslyChosenItemsAsBinaryTree.addNewItemInTheVacantSlotAtIndex(chooseAnyNumberFromZeroToOneLessThan(exclusiveLimitOnVacantSlotIndex))
        
        previouslyChosenItemsAsBinaryTree = chosenItemsAsBinaryTree

        chosenItem #:: chooseAndRecordUniqueItems(exclusiveLimitOnVacantSlotIndex - 1)
      }
    }

    chooseAndRecordUniqueItems(exclusiveLimit)
  }

  def buildRandomSequenceOfDistinctCandidatesChosenFrom[X](candidates: Traversable[X]): Seq[X] = {
    val candidatesWithRandomAccess = candidates.toIndexedSeq

    val numberOfCandidates = candidatesWithRandomAccess.length

    var swappedCandidates = scala.collection.immutable.SortedMap[Int, X]()

    def chooseAndRecordUniqueCandidates(numberOfCandidatesAlreadyChosen: Int): Stream[X] = {
      if (numberOfCandidates == numberOfCandidatesAlreadyChosen) {
        Stream.empty
      } else {
        val chosenCandidateIndex = numberOfCandidatesAlreadyChosen + this.chooseAnyNumberFromZeroToOneLessThan(numberOfCandidates - numberOfCandidatesAlreadyChosen)
        
        val candidatesWithSwapsApplied = swappedCandidates orElse candidatesWithRandomAccess

        val chosenCandidate = candidatesWithSwapsApplied(chosenCandidateIndex)

        if (numberOfCandidatesAlreadyChosen < chosenCandidateIndex) {
            swappedCandidates = swappedCandidates + (chosenCandidateIndex -> candidatesWithSwapsApplied(numberOfCandidatesAlreadyChosen))
          }
        
        swappedCandidates = swappedCandidates - numberOfCandidatesAlreadyChosen	// Optimise memory usage - this index will never be revisited.

        chosenCandidate #:: chooseAndRecordUniqueCandidates(1 + numberOfCandidatesAlreadyChosen)
      }
    }
    
    chooseAndRecordUniqueCandidates(0)
  }

  def chooseSeveralOf[X](candidates: Traversable[X], numberToChoose: Int): Seq[X] = {
    require(numberToChoose <= candidates.size)

    buildRandomSequenceOfDistinctCandidatesChosenFrom(candidates).take(numberToChoose)
  }
  
  def chooseOneOf[X](candidates: Traversable[X]) = {
    // TODO: this can be done without having to build a random-access collection:
    // use an algorithm of weighted probability picking of the head, using a progressive
    // binary tree of fixed-sized samples from the underlying sequence.
    buildRandomSequenceOfDistinctCandidatesChosenFrom(candidates).take(1).head
  }
}