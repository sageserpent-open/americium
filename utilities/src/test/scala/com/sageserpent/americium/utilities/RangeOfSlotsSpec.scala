package com.sageserpent.americium.utilities

import com.sageserpent.americium.utilities.randomEnrichment.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.util.Random

class RangeOfSlotsSpec extends AnyFlatSpec with Matchers {
  "filling out all vacant slots" should "produce a contiguous range of slot positions" in {
    val randomBehaviour = new Random(9834L)

    for (numberOfSlots <- 0 until 50) for (_ <- 1 to 20) {
      val filledSlots =
        List.unfold(RangeOfSlots.allSlotsAreVacant(numberOfSlots)) {
          rangeOfSlots =>
            Option.when(0 < rangeOfSlots.numberOfVacantSlots)(
              rangeOfSlots
                .fillVacantSlotAtIndex(
                  randomBehaviour.chooseAnyNumberFromZeroToOneLessThan(
                    rangeOfSlots.numberOfVacantSlots
                  )
                )
            )
        }

      filledSlots should contain theSameElementsAs (0 until numberOfSlots)
    }
  }

  it should "preserve the total number of slots" in {
    val randomBehaviour = new Random(9834L)

    for (numberOfSlots <- 0 until 50) for (_ <- 1 to 20) {
      val allSlotsAreVacant = RangeOfSlots.allSlotsAreVacant(numberOfSlots)

      allSlotsAreVacant.numberOfVacantSlots shouldBe numberOfSlots
      allSlotsAreVacant.numberOfFilledSlots shouldBe 0

      def verifySlotCountChanges(rangeOfSlots: RangeOfSlots): Unit =
        if (0 == rangeOfSlots.numberOfVacantSlots) {
          rangeOfSlots.numberOfFilledSlots shouldBe numberOfSlots
        } else {
          val (_, rangeOfSlotsWithFilledSlot) = rangeOfSlots
            .fillVacantSlotAtIndex(
              randomBehaviour.chooseAnyNumberFromZeroToOneLessThan(
                rangeOfSlots.numberOfVacantSlots
              )
            )

          rangeOfSlotsWithFilledSlot.numberOfFilledSlots shouldBe (1 + rangeOfSlots.numberOfFilledSlots)
          (1 + rangeOfSlotsWithFilledSlot.numberOfVacantSlots) shouldBe rangeOfSlots.numberOfVacantSlots

          verifySlotCountChanges(rangeOfSlotsWithFilledSlot)
        }

      verifySlotCountChanges(allSlotsAreVacant)
    }
  }

  "each unique interaction sequence" should "yield a unique sequence of slot positions" in {
    val randomBehaviour = new Random(9834L)

    for (numberOfSlots <- 0 until 50) {
      val slotPositionsKeyedByInteractionSequences
          : mutable.Map[Seq[Int], Seq[Int]] = mutable.Map.empty

      val interactionSequencesKeyedBySlotPositions
          : mutable.Map[Seq[Int], Seq[Int]] = mutable.Map.empty

      for (_ <- 1 to 20) {
        val allSlotsAreVacant = RangeOfSlots.allSlotsAreVacant(numberOfSlots)

        val interactionSequence: mutable.ListBuffer[Int] =
          mutable.ListBuffer.empty
        val slotPositions: mutable.ListBuffer[Int] = mutable.ListBuffer.empty

        def recordInteractionSequenceAndResultingSlotPositions(
            rangeOfSlots: RangeOfSlots
        ): Unit =
          if (0 == rangeOfSlots.numberOfVacantSlots) {
            rangeOfSlots.numberOfFilledSlots shouldBe numberOfSlots
          } else {
            val vacantSlotIndex =
              randomBehaviour.chooseAnyNumberFromZeroToOneLessThan(
                rangeOfSlots.numberOfVacantSlots
              )
            val (filledSlot, rangeOfSlotsWithFilledSlot) = rangeOfSlots
              .fillVacantSlotAtIndex(
                vacantSlotIndex
              )

            interactionSequence += vacantSlotIndex
            slotPositions += filledSlot

            recordInteractionSequenceAndResultingSlotPositions(
              rangeOfSlotsWithFilledSlot
            )
          }

        recordInteractionSequenceAndResultingSlotPositions(allSlotsAreVacant)

        val frozenInteractionSequence = interactionSequence.toSeq
        val frozenSlotPositions       = slotPositions.toSeq

        // NOTE: we allow for the same interaction sequence repeating; they are
        // randomly generated after all. However, the duplicates of an
        // interaction sequence should all associate to the same slot positions.
        slotPositionsKeyedByInteractionSequences.getOrElseUpdate(
          frozenInteractionSequence,
          frozenSlotPositions
        ) shouldBe frozenSlotPositions

        interactionSequencesKeyedBySlotPositions.getOrElseUpdate(
          frozenSlotPositions,
          frozenInteractionSequence
        ) shouldBe frozenInteractionSequence
      }
    }

  }
}
