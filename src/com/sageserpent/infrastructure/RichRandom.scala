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
      lazy val inclusiveLowerBoundForAllItemsInSubtree: Option[Int] = {
        this match {
          case InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            lesserSubtree.inclusiveLowerBoundForAllItemsInSubtree orElse (Some(lowerBoundForItemRange))
          case EmptySubtree =>
            None
        }
      }

      lazy val exclusiveUpperBoundForAllItemsInSubtree: Option[Int] = {
        this match {
          case InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            greaterSubtree.exclusiveUpperBoundForAllItemsInSubtree orElse (Some(upperBoundForItemRange))
          case EmptySubtree =>
            None
        }
      }

      lazy val numberOfInteriorNodesInSubtree: Int = {
        this match {
          case InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            1 + lesserSubtree.numberOfInteriorNodesInSubtree + greaterSubtree.numberOfInteriorNodesInSubtree
          case EmptySubtree =>
            0
        }
      }

      lazy val numberOfItemsInSubtree: Int = {
        this match {
          case thisAsInteriorNode @ InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            thisAsInteriorNode.numberOfItemsInRange + lesserSubtree.numberOfItemsInSubtree + greaterSubtree.numberOfItemsInSubtree
          case EmptySubtree =>
            0
        }
      }

      def numberOfVacantSlotsInSubtreeWithinRange(inclusiveLowerBound: Int, exclusiveUpperBound: Int): Int = {
        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound <= exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        this match {
          case thisAsInteriorNode @ InteriorNode(lowerBoundForItemRange: Int, upperBoundForItemRange: Int, lesserSubtree: BinaryTreeNode, greaterSubtree: BinaryTreeNode) =>
            require(thisAsInteriorNode.inclusiveLowerBoundForAllItemsInSubtree match { case Some(inclusiveLowerBoundForAllItemsInSubtree) => inclusiveLowerBound <= inclusiveLowerBoundForAllItemsInSubtree })
            require(thisAsInteriorNode.exclusiveUpperBoundForAllItemsInSubtree match { case Some(exclusiveUpperBoundForAllItemsInSubtree) => exclusiveUpperBoundForAllItemsInSubtree <= exclusiveUpperBound })

          case EmptySubtree =>
            require(inclusiveLowerBoundForAllItemsInSubtree.isEmpty)
            require(exclusiveUpperBoundForAllItemsInSubtree.isEmpty)
        }

        exclusiveUpperBound - inclusiveLowerBound - numberOfItemsInSubtree
      }

      def generateAndAddNewItem(inclusiveLowerBound: Int, exclusiveUpperBound: Int): (BinaryTreeNode, Int)
      def generateAndAddNewItem(): (BinaryTreeNode, Int) = generateAndAddNewItem(0, exclusiveLimit)
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

      def generateAndAddNewItem(inclusiveLowerBound: Int, exclusiveUpperBound: Int) = {
        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        require(inclusiveLowerBound <= lowerBoundForItemRange)
        require(exclusiveUpperBound > upperBoundForItemRange)

        def recurseOnLesserSubtree() = {
          val (lesserSubtreeResult, modifiedItemResult) = lesserSubtree.generateAndAddNewItem(inclusiveLowerBound, lowerBoundForItemRange)

          ((lesserSubtreeResult, greaterSubtree) match {
            case (InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, EmptySubtree),
              _) if 1 + upperBoundForItemRangeFromLesserSubtree == lowerBoundForItemRange => InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRange, lesserSubtreeFromLesserSubtree, greaterSubtree)

            case (InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, greaterSubtreeFromLesserSubtree), _) =>
              InteriorNode(lowerBoundForItemRangeFromLesserSubtree, upperBoundForItemRangeFromLesserSubtree, lesserSubtreeFromLesserSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, greaterSubtreeFromLesserSubtree, greaterSubtree))

            case (_, _) => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtreeResult, greaterSubtree)
          }) -> modifiedItemResult
        }

        def recurseOnGreaterSubtree() = {
          val (greaterSubtreeResult, modifiedItemResult) = greaterSubtree.generateAndAddNewItem(1 + upperBoundForItemRange, exclusiveUpperBound)

          ((lesserSubtree, greaterSubtreeResult) match {
            case (_,
              InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, EmptySubtree, greaterSubtreeFromGreaterSubtree)) if 1 + upperBoundForItemRange == lowerBoundForItemRangeFromGreaterSubtree => InteriorNode(lowerBoundForItemRange, upperBoundForItemRangeFromGreaterSubtree, lesserSubtree, greaterSubtreeFromGreaterSubtree)

            case (_, InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, lesserSubtreeFromGreaterSubtree, greaterSubtreeFromGreaterSubtree)) => InteriorNode(lowerBoundForItemRangeFromGreaterSubtree, upperBoundForItemRangeFromGreaterSubtree, InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, lesserSubtreeFromGreaterSubtree), greaterSubtreeFromGreaterSubtree)

            case (_, _) => InteriorNode(lowerBoundForItemRange, upperBoundForItemRange, lesserSubtree, greaterSubtreeResult)
          }) -> modifiedItemResult

        }

        (lesserSubtreeCanBeConsidered(inclusiveLowerBound), greaterSubtreeCanBeConsidered(exclusiveUpperBound)) match {
          case (true, false) => {
            require(exclusiveUpperBound == exclusiveLimit) // NOTE: in theory this case can occur for other values of 'exclusiveUpperBound', but range-fusion prevents this happening in practice.
            recurseOnLesserSubtree()
          }

          case (false, true) => {
            require(0 == inclusiveLowerBound) // NOTE: in theory this case can occur for other values of 'inclusiveLowerBound', but range-fusion prevents this happening in practice.
            recurseOnGreaterSubtree()
          }

          case (true, true) => {
            val weightInFavourOfLesserSubtree = lesserSubtree.numberOfVacantSlotsInSubtreeWithinRange(inclusiveLowerBound, lowerBoundForItemRange)
            val weightInFavourOfGreaterSubtree = greaterSubtree.numberOfVacantSlotsInSubtreeWithinRange(1 + upperBoundForItemRange, exclusiveUpperBound)

            assert(0 < weightInFavourOfLesserSubtree)
            assert(0 < weightInFavourOfGreaterSubtree)

            val totalOdds = weightInFavourOfLesserSubtree + weightInFavourOfGreaterSubtree

            if (chooseAnyNumberFromZeroToOneLessThan(totalOdds) < weightInFavourOfLesserSubtree) {
              recurseOnLesserSubtree()
            } else {
              recurseOnGreaterSubtree()
            }
          }
        }
      }

      def lesserSubtreeCanBeConsidered(inclusiveLowerBound: Int): Boolean = inclusiveLowerBound < lowerBoundForItemRange

      def greaterSubtreeCanBeConsidered(exclusiveUpperBound: Int): Boolean = 1 + upperBoundForItemRange < exclusiveUpperBound
    }

    case object EmptySubtree extends BinaryTreeNode {
      def generateAndAddNewItem(inclusiveLowerBound: Int, exclusiveUpperBound: Int) = {
        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= exclusiveLimit)

        val generatedItem = inclusiveLowerBound + chooseAnyNumberFromZeroToOneLessThan(exclusiveUpperBound - inclusiveLowerBound)
        new InteriorNode(generatedItem) -> generatedItem
      }
    }

    def chooseAndRecordUniqueItems(numberOfAttemptsLeft: Int, previouslyChosenItemsAsBinaryTree: BinaryTreeNode): Stream[Int] = {
      if (0 == numberOfAttemptsLeft) {
        Stream.empty
      } else {
        val (chosenItemsAsBinaryTree, chosenItem) = previouslyChosenItemsAsBinaryTree.generateAndAddNewItem()

        chosenItem #:: chooseAndRecordUniqueItems(numberOfAttemptsLeft - 1, chosenItemsAsBinaryTree)
      }
    }

    chooseAndRecordUniqueItems(exclusiveLimit, EmptySubtree)
  }

  def ChooseSeveralOf[X](candidates: Seq[X], numberToChoose: Int) = {
    require(numberToChoose <= candidates.size)

    // TODO.
  }
}