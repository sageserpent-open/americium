package com.sageserpent.infrastructure
import scala.util.Random

class RichRandom(random: Random) {
  def chooseAnyNumberFromZeroToOneLessThan(exclusiveLimit: Int) = random.nextInt(exclusiveLimit)

  def chooseAnyNumberFromOneTo(inclusiveLimit: Int) =
    1 + chooseAnyNumberFromZeroToOneLessThan(inclusiveLimit)

  def headsItIs() = random.nextBoolean()

  def buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(exclusiveLimit: Int): Stream[Int] = {
    require(0 <= exclusiveLimit)

    abstract class BinaryTreeNode {
      def numberOfItemsInSubtree: Int
      def addWithinSubtreeModifyingAddedItemToEnsureUniqueness(item: Int, exclusiveUpperBoundOnModifiedItem: Unbounded[Int]): (BinaryTreeNode, Option[Int])
      def addWithinSubtreeModifyingAddedItemToEnsureUniqueness(item: Int): (BinaryTreeNode, Int) = {
        addWithinSubtreeModifyingAddedItemToEnsureUniqueness(item, PositiveInfinity) match {
          case (subtreeWithAddedItem, Some(modifiedItemAfterAddition)) => subtreeWithAddedItem -> modifiedItemAfterAddition
          case (rearrangedSubtreeRootedAtNodeWhereSearchBreachedExclusiveUpperBound, None) => rearrangedSubtreeRootedAtNodeWhereSearchBreachedExclusiveUpperBound.addWithinSubtreeModifyingAddedItemToEnsureUniqueness(item)
        }
      }
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

      val numberOfItemsInSubtree = numberOfItemsInRange + lesserSubtree.numberOfItemsInSubtree + greaterSubtree.numberOfItemsInSubtree

      def addWithinSubtreeModifyingAddedItemToEnsureUniqueness(itemToBeAdded: Int, exclusiveUpperBoundOnModifiedItem: Unbounded[Int]) = {

        itemToBeAdded.compare(lowerBoundForItemRange) match {
          case result if result < 0 => {
            val (lesserSubtreeResult, modifiedItemResult) = lesserSubtree.addWithinSubtreeModifyingAddedItemToEnsureUniqueness(itemToBeAdded, Finite(lowerBoundForItemRange))

            ((lesserSubtreeResult, greaterSubtree, modifiedItemResult) match {
              case (InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, EmptySubtree),
                _, Some(_)) if 1 + upperBoundForItemRangeFromLesserSubtree == lowerBoundForItemRange => InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRange, lesserSubtreeFromLesserSubtree, greaterSubtree)

              case (InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, greaterSubtreeFromLesserSubtree), _, _) =>
                InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, greaterSubtreeFromLesserSubtree, greaterSubtree))

              case (_, _, Some(_)) => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtreeResult, greaterSubtree)
            }) -> modifiedItemResult
          }

          case _ => {
            val offsetToApplyWhenSearchProceedsInGreaterSubtree = numberOfItemsInRange + lesserSubtree.numberOfItemsInSubtree

            val modifiedItemWithOffsetApplied = offsetToApplyWhenSearchProceedsInGreaterSubtree + itemToBeAdded

            if (Finite(modifiedItemWithOffsetApplied) >= exclusiveUpperBoundOnModifiedItem) {
              this -> None
            } else {
              val (greaterSubtreeResult, modifiedItemResult) = greaterSubtree.addWithinSubtreeModifyingAddedItemToEnsureUniqueness(modifiedItemWithOffsetApplied, exclusiveUpperBoundOnModifiedItem)

              ((lesserSubtree, greaterSubtreeResult, modifiedItemResult) match {
                case (_,
                  InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, EmptySubtree, greaterSubtreeFromGreaterSubtree),
                  Some(_)) if 1 + upperBoundForItemRange == lowerBoundForItemRangeFromGreaterSubtree => InteriorNode(lowerBoundForItemRange, upperBoundForItemRangeFromGreaterSubtree, lesserSubtree, greaterSubtreeFromGreaterSubtree)

                case (_, InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, lesserSubtreeFromGreaterSubtree, greaterSubtreeFromGreaterSubtree), Some(_)) => InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, lesserSubtreeFromGreaterSubtree), greaterSubtreeFromGreaterSubtree)

                case (_, _, _) => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, greaterSubtreeResult)
              }) -> modifiedItemResult
            }
          }
        }
      }
    }

    case object EmptySubtree extends BinaryTreeNode {
      val numberOfItemsInSubtree = 0
      def addWithinSubtreeModifyingAddedItemToEnsureUniqueness(item: Int, exclusiveUpperBoundOnModifiedItem: Unbounded[Int]) = new InteriorNode(item) -> Some(item)
    }

    def chooseAndRecordUniqueItem(exclusiveLimit: Int, previouslyChosenItemsAsStreamAndAsBinaryTree: (Stream[Int], BinaryTreeNode)) = {
      val (previouslyChosenItemsAsStream, previouslyChosenItemsAsBinaryTree) = previouslyChosenItemsAsStreamAndAsBinaryTree

      val (chosenItemsAsBinaryTree, chosenItem) = previouslyChosenItemsAsBinaryTree.addWithinSubtreeModifyingAddedItemToEnsureUniqueness(chooseAnyNumberFromZeroToOneLessThan(exclusiveLimit))

      (chosenItem #:: previouslyChosenItemsAsStream) -> chosenItemsAsBinaryTree
    }

    ((1 to exclusiveLimit) :\ ((Stream.Empty -> EmptySubtree): (Stream[Int], BinaryTreeNode)))(chooseAndRecordUniqueItem) _1
  }

  def ChooseSeveralOf[X](candidates: Seq[X], numberToChoose: Int) = {
    require(numberToChoose <= candidates.size)

    // TODO.
  }
}