package com.sageserpent.americium

import com.sageserpent.americium.seqEnrichment._
import hedgehog.core.{DiscardCount, PropertyConfig, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Range}
import org.scalatest.LoneElement._
import org.scalatest.enablers.Collecting._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.SortedSet

class RichSeqSpec
    extends FlatSpec
    with Matchers
    with HedgehogScalatestIntegration {
  implicit val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(5000), DiscardCount(1000), ShrinkLimit(100))

  private val groupEverythingTogether: (Int, Int) => Boolean = {
    case (_, _) => true
  }

  private val groupNothingTogether: (Int, Int) => Boolean = {
    case (_, _) => false
  }

  private val groupEqualTogether: (Int, Int) => Boolean = _ == _

  private val predicateGenerator = Gen.choice1(
    Gen.constant(groupEverythingTogether),
    Gen.constant(groupNothingTogether),
    Gen.constant(groupEqualTogether)
  )

  private val nonEmptyInputSequenceGenerator =
    Gen.list(Gen.int(Range.linear(0, 1000)), Range.linear(1, 100))

  private val possiblyEmptyInputSequenceGenerator =
    Gen.list(Gen.int(Range.linear(0, 1000)), Range.linear(0, 100))

  "groupWhile" should "respect the exact sequence type that it works on" in {
    "val groups: Seq[List[Int]] = List(1, 2, 2).groupWhile(groupEverythingTogether)" should compile
    "val groups: Seq[List[Int]] = Seq(1, 2, 2).groupWhile(groupEverythingTogether)" shouldNot typeCheck
  }

  it should "result in an empty sequence of groups when presented with an empty input sequence" in
    check(predicateGenerator) { predicate =>
      List
        .empty[Int]
        .groupWhile(predicate) should be(Seq.empty[List[Int]])
    }

  it should "yield non empty groups if the input sequence is not empty" in
    check(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val groups = inputSequence.groupWhile(groupEverythingTogether)
        all(groups) should not be empty
    }

  it should "preserve all items in the input sequence" in
    check(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val actualItems = inputSequence.groupWhile(predicate) flatMap identity
        actualItems should contain theSameElementsAs inputSequence
    }

  it should "preserve the order of items in the input sequence" in
    check(predicateGenerator, nonEmptyInputSequenceGenerator) {
      (predicate, inputSequence) =>
        val actualItems = inputSequence.groupWhile(predicate) flatMap identity
        actualItems should contain theSameElementsInOrderAs inputSequence
    }

  it should "fragment the input sequence into single item groups if the predicate is always false" in
    check(nonEmptyInputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupNothingTogether)
      all(groups map (_.loneElement))
    }

  it should "reproduce the input sequence as a single group if the predicate is always true" in
    check(nonEmptyInputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupEverythingTogether)
      groups.loneElement should contain theSameElementsInOrderAs inputSequence
    }

  it should "identify runs of adjacent duplicates if the predicate is equality" in
    check(nonEmptyInputSequenceGenerator) { inputSequence =>
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
    check(Gen.list(Gen.constant(List.empty[Int]), Range.linear(1, 50))) {
      emptyInnerSequences =>
        val links = emptyInnerSequences.zipN
        links should be(empty)
    }

  it should "yield non empty inner sequences if at least one of the input inner sequences is not empty" in
    check(Gen.list(nonEmptyInputSequenceGenerator, Range.linear(1, 50))) {
      innerSequences =>
        val links = innerSequences.zipN
        links should not be empty
        all(links) should not be empty
    }

  it should "preserve all items in the input inner sequences" in
    check(Gen.list(nonEmptyInputSequenceGenerator, Range.linear(1, 50))) {
      inputSequences =>
        val expectedItems = inputSequences.flatten
        val actualItems   = inputSequences.zipN.flatten
        actualItems should contain theSameElementsAs expectedItems
    }

  it should "preserve the order of items as they appear in their own input inner sequence" in
    check(Gen.list(
      possiblyEmptyInputSequenceGenerator map (_.sorted) map (_ map (_.toLong)),
      Range.linear(1, 50))) { inputMultiplierSequences =>
      val numberOfSequences = inputMultiplierSequences.length
      val inputSequences = inputMultiplierSequences.zipWithIndex.map {
        case (multipliers, sequenceMarker) =>
          multipliers map (sequenceMarker + numberOfSequences * _)
      }
      val actualItems = inputSequences.zipN.flatten
      for ((inputSequence, sequenceMarker) <- inputSequences zipWithIndex)
        actualItems filter (sequenceMarker == Math.floorMod(
          _,
          numberOfSequences)) should contain inOrderElementsOf (inputSequence)
    }

  it should "preserve the order of items as they appear across the input sequences, if the inner sequence type preserves the original order" in
    check(
      Gen.list(possiblyEmptyInputSequenceGenerator map (_ map (_.toLong)),
               Range.linear(1, 50))) { inputMultiplierSequences =>
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
    check(
      Gen
        .list(possiblyEmptyInputSequenceGenerator map (items =>
                SortedSet(items: _*)),
              Range.linear(1, 50))) { inputSequences =>
      val links = inputSequences.zipN
      all(links map (_.toSeq)) shouldBe sorted
    }
}
