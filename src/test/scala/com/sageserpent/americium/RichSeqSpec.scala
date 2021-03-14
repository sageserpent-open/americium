package com.sageserpent.americium

import com.sageserpent.americium.seqEnrichment._
import org.scalacheck.{Arbitrary, Gen, ShrinkLowPriority}
import org.scalatest.LoneElement._
import org.scalatest.enablers.Collecting._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.collection.immutable.SortedSet

class RichSeqSpec
    extends AnyFlatSpec
    with Matchers
    with ShrinkLowPriority
    with ScalaCheckDrivenPropertyChecks {
  private val groupEverythingTogether: (Int, Int) => Boolean = {
    case (_, _) => true
  }

  private val groupNothingTogether: (Int, Int) => Boolean = {
    case (_, _) => false
  }

  private val groupEqualTogether: (Int, Int) => Boolean = _ == _

  private val predicateGenerator = Gen.oneOf(
    Gen.const(groupEverythingTogether) :| "Group everything together",
    Gen.const(groupNothingTogether) :| "Group nothing together",
    Gen.const(groupEqualTogether) :| "Group equal things together"
  )

  private val nonEmptyInputSequenceGenerator =
    Gen.nonEmptyListOf(Arbitrary.arbInt.arbitrary)

  private val possiblyEmptyInputSequenceGenerator =
    Gen.listOf(Arbitrary.arbInt.arbitrary)

  "groupWhile" should "respect the exact sequence type that it works on" in {
    "val groups: Seq[List[Int]] = List(1, 2, 2).groupWhile(groupEverythingTogether)" should compile
    "val groups: Seq[List[Int]] = Seq(1, 2, 2).groupWhile(groupEverythingTogether)" shouldNot typeCheck
  }

  it should "result in an empty sequence of groups when presented with an empty input sequence" in
    forAll(predicateGenerator) { predicate =>
      List
        .empty[Int]
        .groupWhile(predicate) should be(Seq.empty[List[Int]])
    }

  it should "yield non empty groups if the input sequence is not empty" in
    forAll(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val groups = inputSequence.groupWhile(predicate)
        all(groups) should not be empty
    }

  it should "preserve all items in the input sequence" in
    forAll(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val actualItems = inputSequence.groupWhile(predicate).flatten
        actualItems should contain theSameElementsAs inputSequence
    }

  it should "preserve the order of items in the input sequence" in
    forAll(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val actualItems = inputSequence.groupWhile(predicate).flatten
        actualItems should contain theSameElementsInOrderAs inputSequence
    }

  it should "fragment the input sequence into single item groups if the predicate is always false" in
    forAll(nonEmptyInputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupNothingTogether)
      all(groups map (_.loneElement))
    }

  it should "reproduce the input sequence as a single group if the predicate is always true" in
    forAll(nonEmptyInputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupEverythingTogether)
      groups.loneElement should contain theSameElementsInOrderAs inputSequence
    }

  it should "identify runs of adjacent duplicates if the predicate is equality" in
    forAll(nonEmptyInputSequenceGenerator) { inputSequence =>
      val groups          = inputSequence.groupWhile(groupEqualTogether)
      val collapsedGroups = groups map (_.distinct)
      all(collapsedGroups map (_.loneElement))
      collapsedGroups zip collapsedGroups.tail foreach {
        case (predecessor, successor) =>
          withClue(
            "Comparing the single element from a previous group and the single element from the following group: ")(
            predecessor.loneElement should not be successor.loneElement)
      }
    }

  "zipN" should "respect the exact inner sequence types that it works on" in {
    "val stream: Stream[List[Int]] = Seq(List(1 , 2), List(3, 4), List.empty[Int]).zipN" should compile
    "val stream: Stream[List[Int]] = Seq(Seq(1 , 2), Seq(3, 4), Seq.empty[Int]).zipN" shouldNot typeCheck
  }

  it should "result in an empty stream for an empty input sequence" in {
    val links: Stream[List[Int]] = Seq.empty[List[Int]].zipN
    links should be(empty)
  }

  it should "result in an empty stream if all of the input inner sequences are empty" in
    forAll(Gen.nonEmptyListOf(Gen.const(List.empty[Int]))) {
      emptyInnerSequences =>
        val links = emptyInnerSequences.zipN
        links should be(empty)
    }

  it should "yield non empty inner sequences if at least one of the input inner sequences is not empty" in
    forAll(Gen.nonEmptyListOf(nonEmptyInputSequenceGenerator)) {
      innerSequences =>
        val links = innerSequences.zipN
        links should not be empty
        all(links) should not be empty
    }

  it should "preserve all items in the input inner sequences" in
    forAll(Gen.nonEmptyListOf(possiblyEmptyInputSequenceGenerator)) {
      inputSequences =>
        val expectedItems = inputSequences.flatten
        val actualItems   = inputSequences.zipN.flatten
        actualItems should contain theSameElementsAs expectedItems
    }

  it should "preserve the order of items as they appear in their own input inner sequence" in
    forAll(Gen.nonEmptyListOf(
      possiblyEmptyInputSequenceGenerator map (_.sorted) map (_ map (_.toLong)))) {
      inputMultiplierSequences =>
        val numberOfSequences = inputMultiplierSequences.length
        val inputSequences = inputMultiplierSequences.zipWithIndex.map {
          case (multipliers, sequenceMarker) =>
            multipliers map (sequenceMarker + numberOfSequences * _)
        }
        val actualItems = inputSequences.zipN.flatten
        for ((inputSequence, sequenceMarker) <- inputSequences.zipWithIndex)
          actualItems filter (sequenceMarker == Math.floorMod(
            _,
            numberOfSequences)) should contain inOrderElementsOf (inputSequence)
    }

  it should "preserve the order of items as they appear across the input sequences, if the inner sequence type preserves the original order" in
    forAll(
      Gen.nonEmptyListOf(
        possiblyEmptyInputSequenceGenerator map (_ map (_.toLong)))) {
      inputMultiplierSequences =>
        val numberOfSequences = inputMultiplierSequences.length
        val inputSequences = inputMultiplierSequences.zipWithIndex.map {
          case (multipliers, sequenceMarker) =>
            multipliers map (sequenceMarker + numberOfSequences * _)
        }
        val links = inputSequences.zipN
        withClue(
          "The markers from each input inner sequence should appear in sorted order in each link")(
          all(links map (_.map(Math.floorMod(_, numberOfSequences)))) shouldBe sorted)
    }

  it should "impose the inner sequence type's ordering on items taken from across the input sequence, if such an ordering is defined" in
    forAll(
      Gen
        .nonEmptyListOf(possiblyEmptyInputSequenceGenerator map (items =>
          SortedSet(items: _*)))) { inputSequences =>
      val links = inputSequences.zipN
      all(links map (_.toSeq)) shouldBe sorted
    }
}
