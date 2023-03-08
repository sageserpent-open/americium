package com.sageserpent.americium

/** Represents a range of slots, where each slot is either vacant or occupied by
  * an integer whose value corresponds to the slot position, taken
  * zero-relative.
  *
  * The idea is to start with an initial instance whose slots are all vacant,
  * then to add items in at some vacant slot without knowing the exact slot
  * position; instead we specify the vacant slot's index wrt the rest of the
  * vacant slots without caring about the ones already filled.
  */
trait RangeOfSlots {

  /** Fill in a vacant slot with a value that denotes the slot's position.
    * @param indexOfVacantSlotAsCountedByVacanciesOnly
    *   Picks out a *vacant* slot, the value must range from 0 to one less than
    *   the number of vacant slots.
    * @note
    *   We don't specify or expect to know the exact slot position, rather we
    *   work in terms of how many vacant slots there are.
    * @return
    *   `this` with the given vacant slot filled and the position of the filled
    *   slot.
    */
  def fillVacantSlotAtIndex(
      indexOfVacantSlotAsCountedByVacanciesOnly: Int
  ): (RangeOfSlots, Int)

  def numberOfVacantSlots: Int

  def numberOfFilledSlots: Int
}

object RangeOfSlots {
  def allSlotsAreVacant(numberOfSlots: Int): RangeOfSlots = {
    require(0 <= numberOfSlots)

    // NOTE: have to make this an abstract class and *not* a trait to avoid a
    // code-generation bug seen in Scala 2.13 when this code is executed.
    sealed abstract class BinaryTreeNode {

      def inclusiveLowerBoundForAllItemsInSubtree: Option[Int]

      def exclusiveUpperBoundForAllItemsInSubtree: Option[Int]

      def numberOfInteriorNodesInSubtree: Int

      def numberOfFilledSlots: Int

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

        exclusiveUpperBound - inclusiveLowerBound - numberOfFilledSlots
      }

      def fillVacantSlotAtIndex(
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

      val numberOfFilledSlots: Int =
        numberOfItemsInRange + lesserSubtree.numberOfFilledSlots + greaterSubtree.numberOfFilledSlots

      def fillVacantSlotAtIndex(
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
            lesserSubtree.fillVacantSlotAtIndex(
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
            greaterSubtree.fillVacantSlotAtIndex(
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

      val numberOfFilledSlots: Int = 0

      def fillVacantSlotAtIndex(
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

    case class Implementation(binaryTreeNode: BinaryTreeNode)
        extends RangeOfSlots {
      override def fillVacantSlotAtIndex(
          indexOfVacantSlotAsCountedByVacanciesOnly: Int
      ): (RangeOfSlots, Int) = {
        val (node, filledSlot) = binaryTreeNode.fillVacantSlotAtIndex(
          indexOfVacantSlotAsCountedByVacanciesOnly,
          0,
          numberOfSlots
        )

        Implementation(node) -> filledSlot
      }

      override def numberOfVacantSlots: Int =
        numberOfSlots - numberOfFilledSlots

      override def numberOfFilledSlots: Int = binaryTreeNode.numberOfFilledSlots
    }

    Implementation(EmptySubtree)
  }
}
