package com.sageserpent.americium

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.util.Random

trait RandomEnrichment {
  implicit class RichRandom(random: Random) {
    // TODO - throw all this rubbish out and use reservoir sampling!

    def chooseAnyNumberFromZeroToOneLessThan[X: Numeric](
        exclusiveLimit: X
    ): X = {
      val typeClass = implicitly[Numeric[X]]
      import typeClass.*
      fromInt(random.nextInt(exclusiveLimit.toInt))
    }

    def chooseAnyNumberFromOneTo[X: Numeric](inclusiveLimit: X): X = {
      val typeClass = implicitly[Numeric[X]]
      import typeClass.*
      one + chooseAnyNumberFromZeroToOneLessThan(inclusiveLimit)
    }

    def buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(
        exclusiveLimit: Int
    ): LazyList[Int] = {
      require(0 <= exclusiveLimit)

      abstract class BinaryTreeNode {
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
          require(exclusiveUpperBound <= exclusiveLimit)

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

        def addNewItemInTheVacantSlotAtIndex(
            indexOfVacantSlotAsOrderedByMissingItem: Int
        ): (BinaryTreeNode, Int) =
          addNewItemInTheVacantSlotAtIndex(
            indexOfVacantSlotAsOrderedByMissingItem,
            0,
            exclusiveLimit
          )
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
            indexOfVacantSlotAsOrderedByMissingItem < exclusiveLimit
          ) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
          // decremented for each move to a greater subtree. It does hold
          // however, so is left in as a last line of
          // defence sanity check.

          require(inclusiveLowerBound >= 0)
          require(inclusiveLowerBound < exclusiveUpperBound)
          require(exclusiveUpperBound <= exclusiveLimit)

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
                exclusiveUpperBound == exclusiveLimit
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
            indexOfVacantSlotAsOrderedByMissingItem < exclusiveLimit
          ) // This is a very loose upper bound, because 'indexOfVacantSlotAsOrderedByMissingItem' is progressively
          // decremented for each move to a greater subtree. It does hold
          // however, so is left in as a last line of
          // defence sanity check.

          require(inclusiveLowerBound >= 0)
          require(inclusiveLowerBound < exclusiveUpperBound)
          require(exclusiveUpperBound <= exclusiveLimit)

          val generatedItem =
            inclusiveLowerBound + indexOfVacantSlotAsOrderedByMissingItem

          assume(generatedItem < exclusiveUpperBound)

          new InteriorNode(generatedItem) -> generatedItem
        }
      }

      def chooseAndRecordUniqueItems(
          exclusiveLimitOnVacantSlotIndex: Int,
          previouslyChosenItemsAsBinaryTree: BinaryTreeNode
      ): LazyList[Int] = {
        if (0 == exclusiveLimitOnVacantSlotIndex) {
          LazyList.empty
        } else {
          val (chosenItemsAsBinaryTree, chosenItem) =
            previouslyChosenItemsAsBinaryTree.addNewItemInTheVacantSlotAtIndex(
              chooseAnyNumberFromZeroToOneLessThan(
                exclusiveLimitOnVacantSlotIndex
              )
            )

          chosenItem #:: chooseAndRecordUniqueItems(
            exclusiveLimitOnVacantSlotIndex - 1,
            chosenItemsAsBinaryTree
          )
        }
      }

      chooseAndRecordUniqueItems(exclusiveLimit, EmptySubtree)
    }

    def buildRandomSequenceOfDistinctCandidatesChosenFrom[X](
        candidates: Iterable[X]
    ): Seq[X] = {
      val candidatesWithRandomAccess = candidates.toIndexedSeq

      val numberOfCandidates = candidatesWithRandomAccess.length

      val swappedCandidates = scala.collection.mutable.Map[Int, X]()

      def chooseAndRecordUniqueCandidates(
          numberOfCandidatesAlreadyChosen: Int
      ): LazyList[X] = {
        if (numberOfCandidates == numberOfCandidatesAlreadyChosen) {
          LazyList.empty
        } else {
          val chosenCandidateIndex = numberOfCandidatesAlreadyChosen + this
            .chooseAnyNumberFromZeroToOneLessThan(
              numberOfCandidates - numberOfCandidatesAlreadyChosen
            )

          val candidatesWithSwapsApplied =
            swappedCandidates orElse candidatesWithRandomAccess

          val chosenCandidate = candidatesWithSwapsApplied(chosenCandidateIndex)

          if (numberOfCandidatesAlreadyChosen < chosenCandidateIndex) {
            swappedCandidates += chosenCandidateIndex -> candidatesWithSwapsApplied(
              numberOfCandidatesAlreadyChosen
            )
          }

          swappedCandidates -= numberOfCandidatesAlreadyChosen // Optimise memory usage - this index will never be revisited.

          chosenCandidate #:: chooseAndRecordUniqueCandidates(
            1 + numberOfCandidatesAlreadyChosen
          )
        }
      }

      chooseAndRecordUniqueCandidates(0)
    }

    def chooseSeveralOf[X](
        candidates: Iterable[X],
        numberToChoose: Int
    ): Seq[X] = {
      require(numberToChoose <= candidates.size)

      buildRandomSequenceOfDistinctCandidatesChosenFrom(candidates).take(
        numberToChoose
      )
    }

    def chooseOneOf[X](candidates: Iterable[X]): X = {
      // How does this algorithm work? It is a generalisation of the old trick
      // of choosing an item from a sequence working down the sequence,
      // either picking the head or recursing on to the tail of the sequence.
      // The probability of picking the head at each stage of recursion
      // increases in such a way that the cumulative product of the failure to
      // pick probabilities and the final successful pick probability
      // always comes out to be the same. That's the standard algorithm, the
      // generalisation here is to pick blocks rather than single items,
      // then to pick an exemplar from the chosen block as a final step. The
      // block sizes go up geometrically, so the algorithm gets greedier as it
      // carries
      // on through a potentially very large sequence. The point of this is to
      // avoid converting all of 'candidates' into a whopping great array-backed
      // data structure to avoid overly large memory allocations. When reading
      // this code, bear in mind that the algorithm actually traverses the
      // sequence from back to front - the blocks and their lazily-evaluated
      // exemplars are built up in forward order but picked from in reverse
      // order.
      @scala.annotation.tailrec
      def chooseExemplarsFromCandidateBlocks(
          candidates: Iterable[X],
          blockSize: Int,
          cumulativeNumberOfCandidatesPreviouslySeen: Int,
          exemplarTuples: List[(() => X, Int, Int)]
      ): List[(() => X, Int, Int)] = {
        val (candidateBlock, remainingCandidates) = candidates splitAt blockSize

        if (candidateBlock isEmpty) exemplarTuples
        else {
          val candidateBlockSize = candidateBlock size
          val exemplar = () =>
            buildRandomSequenceOfDistinctCandidatesChosenFrom(
              candidateBlock
            ).head

          chooseExemplarsFromCandidateBlocks(
            remainingCandidates,
            blockSize * 7 / 6,
            candidateBlockSize + cumulativeNumberOfCandidatesPreviouslySeen,
            (
              exemplar,
              candidateBlockSize,
              cumulativeNumberOfCandidatesPreviouslySeen
            ) :: exemplarTuples
          )
        }
      }

      val exemplars =
        chooseExemplarsFromCandidateBlocks(candidates, 100, 0, List.empty)

      @scala.annotation.tailrec
      def chooseASingleExemplar(exemplars: List[(() => X, Int, Int)]): X =
        exemplars match {
          case List((exemplar, _, _)) => exemplar()
          case (
                exemplar,
                blockSize,
                cumulativeNumberOfCandidatesPreviouslySeen
              ) :: remainingExemplars =>
            val numberOfCandidates =
              blockSize + cumulativeNumberOfCandidatesPreviouslySeen
            if (chooseAnyNumberFromOneTo(numberOfCandidates) <= blockSize)
              exemplar()
            else
              chooseASingleExemplar(remainingExemplars)
        }

      chooseASingleExemplar(exemplars)
    }

    def pickAlternatelyFrom[X](sequences: Iterable[Iterable[X]]): LazyList[X] =
      LazyList.unfold(sequences map (_.to(LazyList)) toList) { streams =>
        @tailrec
        def pickAnItem(
            streams: List[LazyList[X]]
        ): Option[(X, List[LazyList[X]])] = {
          if (streams.isEmpty) None
          else {
            val candidateStreamToPickFrom :: remainingStreams =
              random.shuffle(streams)
            candidateStreamToPickFrom match {
              case LazyList() =>
                pickAnItem(remainingStreams)
              case pickedItem #:: tailFromPickedStream =>
                Some(pickedItem -> (tailFromPickedStream :: remainingStreams))
            }
          }
        }

        pickAnItem(streams)
      }

    def splitIntoNonEmptyPieces[Container[X] <: Iterable[X], Item](
        items: Container[Item]
    ): LazyList[Container[Item]] = {
      val numberOfItems = items.size
      if (0 < numberOfItems) {
        val numberOfSplitsDesired = chooseAnyNumberFromOneTo(numberOfItems)
        val indicesToSplitAt =
          buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(
            numberOfItems
          ) map (1 + _) take numberOfSplitsDesired sorted

        def splits(
            indicesToSplitAt: LazyList[Int],
            items: Container[Item],
            indexOfPreviousSplit: Int
        ): LazyList[Container[Item]] =
          indicesToSplitAt match {
            case LazyList() =>
              if (items.isEmpty) LazyList.empty
              else LazyList(items)
            case indexToSplitAt #:: remainingIndicesToSplitAt =>
              val (
                splitPiece: Container[Item],
                remainingItems: Container[Item]
              ) =
                items splitAt (indexToSplitAt - indexOfPreviousSplit)
              splitPiece #:: splits(
                remainingIndicesToSplitAt,
                remainingItems,
                indexToSplitAt
              )
          }

        splits(indicesToSplitAt, items, 0)
      } else LazyList.empty
    }
  }
}
