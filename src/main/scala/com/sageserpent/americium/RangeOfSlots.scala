package com.sageserpent.americium

trait RangeOfSlots {
  def addNewItemInTheVacantSlotAtIndex(
      indexOfVacantSlotAsOrderedByMissingItem: Int
  ): (RangeOfSlots, Int)
}

object RangeOfSlots {
  def allSlotsAreVacant(numberOfSlots: Int): RangeOfSlots = {
    require(0 <= numberOfSlots)

    // NOTE: have to make this an abstract class and *not* a trait to avoid a
    // code-generation bug seen in Scala 2.13 when this code is executed.
    sealed abstract class BinaryTreeNode extends RangeOfSlots {
      override def addNewItemInTheVacantSlotAtIndex(
          indexOfVacantSlotAsOrderedByMissingItem: Int
      ): (BinaryTreeNode, Int) =
        addNewItemInTheVacantSlotAtIndex(
          indexOfVacantSlotAsOrderedByMissingItem,
          0,
          numberOfSlots
        )

      def inclusiveLowerBoundForAllItemsInSubtree: Option[Int]

      def exclusiveUpperBoundForAllItemsInSubtree: Option[Int]

      def numberOfInteriorNodesInSubtree: Int

      def numberOfItemsInSubtree: Int

      def numberOfVacantSlotsInSubtreeWithinRange(
          inclusiveLowerBound: Int,
          exclusiveUpperBound: Int
      ): Int = {
        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound <= exclusiveUpperBound)
        require(exclusiveUpperBound <= numberOfSlots)

        this match {
          case thisAsInteriorNode: InteriorNode =>
            require(
              thisAsInteriorNode.inclusiveLowerBoundForAllItemsInSubtree match {
                case Some(inclusiveLowerBoundForAllItemsInSubtree) =>
                  inclusiveLowerBound <= inclusiveLowerBoundForAllItemsInSubtree
              }
            )
            require(
              thisAsInteriorNode.exclusiveUpperBoundForAllItemsInSubtree match {
                case Some(exclusiveUpperBoundForAllItemsInSubtree) =>
                  exclusiveUpperBoundForAllItemsInSubtree <= exclusiveUpperBound
              }
            )

          case EmptySubtree =>
            assume(inclusiveLowerBoundForAllItemsInSubtree.isEmpty)
            assume(exclusiveUpperBoundForAllItemsInSubtree.isEmpty)
        }

        exclusiveUpperBound - inclusiveLowerBound - numberOfItemsInSubtree
      }

      def addNewItemInTheVacantSlotAtIndex(
          indexOfVacantSlotAsOrderedByMissingItem: Int,
          inclusiveLowerBound: Int,
          exclusiveUpperBound: Int
      ): (BinaryTreeNode, Int)
    }

    case class InteriorNode(
        lowerBoundForItemRange: Int,
        upperBoundForItemRange: Int,
        lesserSubtree: BinaryTreeNode,
        greaterSubtree: BinaryTreeNode
    ) extends BinaryTreeNode {
      require(lowerBoundForItemRange <= upperBoundForItemRange)

      lesserSubtree match {
        case InteriorNode(_, upperBoundForItemRangeFromLesserSubtree, _, _) =>
          require(
            upperBoundForItemRangeFromLesserSubtree + 1 < lowerBoundForItemRange
          )
        case _ => ()
      }

      greaterSubtree match {
        case InteriorNode(
              lowerBoundForItemRangeFromGreaterSubtree,
              _,
              _,
              _
            ) =>
          require(
            upperBoundForItemRange + 1 < lowerBoundForItemRangeFromGreaterSubtree
          )
        case _ => ()
      }

      def this(singleItem: Int) =
        this(singleItem, singleItem, EmptySubtree, EmptySubtree)

      private val numberOfItemsInRange =
        1 + upperBoundForItemRange - lowerBoundForItemRange

      val inclusiveLowerBoundForAllItemsInSubtree: Option[Int] =
        lesserSubtree.inclusiveLowerBoundForAllItemsInSubtree orElse Some(
          lowerBoundForItemRange
        )

      val exclusiveUpperBoundForAllItemsInSubtree: Option[Int] =
        greaterSubtree.exclusiveUpperBoundForAllItemsInSubtree orElse Some(
          upperBoundForItemRange
        )

      val numberOfInteriorNodesInSubtree: Int =
        1 + lesserSubtree.numberOfInteriorNodesInSubtree + greaterSubtree.numberOfInteriorNodesInSubtree

      val numberOfItemsInSubtree: Int =
        numberOfItemsInRange + lesserSubtree.numberOfItemsInSubtree + greaterSubtree.numberOfItemsInSubtree

      def addNewItemInTheVacantSlotAtIndex(
          indexOfVacantSlotAsOrderedByMissingItem: Int,
          inclusiveLowerBound: Int,
          exclusiveUpperBound: Int
      ) = {
        require(indexOfVacantSlotAsOrderedByMissingItem >= 0)
        require(
          indexOfVacantSlotAsOrderedByMissingItem < numberOfSlots
        ) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
        // decremented for each move to a greater subtree. It does hold
        // however, so is left in as a last line of
        // defence sanity check.

        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= numberOfSlots)

        require(inclusiveLowerBound <= lowerBoundForItemRange)
        require(exclusiveUpperBound > upperBoundForItemRange)

        val effectiveIndexAssociatedWithThisInteriorNode =
          lesserSubtree.numberOfVacantSlotsInSubtreeWithinRange(
            inclusiveLowerBound,
            lowerBoundForItemRange
          )

        def recurseOnLesserSubtree() = {
          val (lesserSubtreeResult, modifiedItemResult) =
            lesserSubtree.addNewItemInTheVacantSlotAtIndex(
              indexOfVacantSlotAsOrderedByMissingItem,
              inclusiveLowerBound,
              lowerBoundForItemRange
            )

          (lesserSubtreeResult match {
            case InteriorNode(
                  lowerBoundForItemRangeFromLesserSubtree,
                  upperBoundForItemRangeFromLesserSubtree,
                  lesserSubtreeFromLesserSubtree,
                  EmptySubtree
                )
                if 1 + upperBoundForItemRangeFromLesserSubtree == lowerBoundForItemRange =>
              InteriorNode(
                lowerBoundForItemRangeFromLesserSubtree,
                upperBoundForItemRange,
                lesserSubtreeFromLesserSubtree,
                greaterSubtree
              )

            case InteriorNode(
                  lowerBoundForItemRangeFromLesserSubtree,
                  upperBoundForItemRangeFromLesserSubtree,
                  lesserSubtreeFromLesserSubtree,
                  greaterSubtreeFromLesserSubtree
                ) =>
              InteriorNode(
                lowerBoundForItemRangeFromLesserSubtree,
                upperBoundForItemRangeFromLesserSubtree,
                lesserSubtreeFromLesserSubtree,
                InteriorNode(
                  lowerBoundForItemRange,
                  upperBoundForItemRange,
                  greaterSubtreeFromLesserSubtree,
                  greaterSubtree
                )
              )

            case _ =>
              InteriorNode(
                lowerBoundForItemRange,
                upperBoundForItemRange,
                lesserSubtreeResult,
                greaterSubtree
              )
          }) -> modifiedItemResult
        }

        def recurseOnGreaterSubtree() = {
          val (greaterSubtreeResult, modifiedItemResult) =
            greaterSubtree.addNewItemInTheVacantSlotAtIndex(
              indexOfVacantSlotAsOrderedByMissingItem - effectiveIndexAssociatedWithThisInteriorNode,
              1 + upperBoundForItemRange,
              exclusiveUpperBound
            )

          (greaterSubtreeResult match {
            case InteriorNode(
                  lowerBoundForItemRangeFromGreaterSubtree,
                  upperBoundForItemRangeFromGreaterSubtree,
                  EmptySubtree,
                  greaterSubtreeFromGreaterSubtree
                )
                if 1 + upperBoundForItemRange == lowerBoundForItemRangeFromGreaterSubtree =>
              InteriorNode(
                lowerBoundForItemRange,
                upperBoundForItemRangeFromGreaterSubtree,
                lesserSubtree,
                greaterSubtreeFromGreaterSubtree
              )

            case InteriorNode(
                  lowerBoundForItemRangeFromGreaterSubtree,
                  upperBoundForItemRangeFromGreaterSubtree,
                  lesserSubtreeFromGreaterSubtree,
                  greaterSubtreeFromGreaterSubtree
                ) =>
              InteriorNode(
                lowerBoundForItemRangeFromGreaterSubtree,
                upperBoundForItemRangeFromGreaterSubtree,
                InteriorNode(
                  lowerBoundForItemRange,
                  upperBoundForItemRange,
                  lesserSubtree,
                  lesserSubtreeFromGreaterSubtree
                ),
                greaterSubtreeFromGreaterSubtree
              )

            case _ =>
              InteriorNode(
                lowerBoundForItemRange,
                upperBoundForItemRange,
                lesserSubtree,
                greaterSubtreeResult
              )
          }) -> modifiedItemResult
        }

        def lesserSubtreeCanBeConsidered(inclusiveLowerBound: Int): Boolean =
          inclusiveLowerBound < lowerBoundForItemRange

        def greaterSubtreeCanBeConsidered(exclusiveUpperBound: Int): Boolean =
          1 + upperBoundForItemRange < exclusiveUpperBound

        (
          lesserSubtreeCanBeConsidered(inclusiveLowerBound),
          greaterSubtreeCanBeConsidered(exclusiveUpperBound)
        ) match {
          case (true, false) =>
            assume(
              exclusiveUpperBound == numberOfSlots
            ) // NOTE: in theory this case can occur for other values of 'exclusiveUpperBound', but range-fusion prevents this happening in practice.
            recurseOnLesserSubtree()

          case (false, true) =>
            assume(
              0 == inclusiveLowerBound
            ) // NOTE: in theory this case can occur for other values of 'inclusiveLowerBound', but range-fusion prevents this happening in practice.
            recurseOnGreaterSubtree()

          case (true, true) =>
            if (
              0 > indexOfVacantSlotAsOrderedByMissingItem.compare(
                effectiveIndexAssociatedWithThisInteriorNode
              )
            ) {
              recurseOnLesserSubtree()
            } else {
              recurseOnGreaterSubtree()
            }
        }
      }
    }

    case object EmptySubtree extends BinaryTreeNode {
      val inclusiveLowerBoundForAllItemsInSubtree: Option[Nothing] = None

      val exclusiveUpperBoundForAllItemsInSubtree: Option[Nothing] = None

      val numberOfInteriorNodesInSubtree: Int = 0

      val numberOfItemsInSubtree: Int = 0

      def addNewItemInTheVacantSlotAtIndex(
          indexOfVacantSlotAsOrderedByMissingItem: Int,
          inclusiveLowerBound: Int,
          exclusiveUpperBound: Int
      ) = {
        require(indexOfVacantSlotAsOrderedByMissingItem >= 0)
        require(
          indexOfVacantSlotAsOrderedByMissingItem < numberOfSlots
        ) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
        // decremented for each move to a greater subtree. It does hold
        // however, so is left in as a last line of
        // defence sanity check.

        require(inclusiveLowerBound >= 0)
        require(inclusiveLowerBound < exclusiveUpperBound)
        require(exclusiveUpperBound <= numberOfSlots)

        val generatedItem =
          inclusiveLowerBound + indexOfVacantSlotAsOrderedByMissingItem

        assume(generatedItem < exclusiveUpperBound)

        new InteriorNode(generatedItem) -> generatedItem
      }
    }

    EmptySubtree
  }
}
