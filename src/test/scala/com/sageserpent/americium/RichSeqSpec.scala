package com.sageserpent.americium

import com.sageserpent.americium.seqEnrichment._
import org.scalatest.enablers.Collecting._
import org.scalatest.LoneElement._
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.collection.immutable.{HashBag, HashedBagConfiguration}

/**
  * Created by Gerard on 15/09/2015.
  */
class RichSeqSpec
    extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks {
  private val groupEverythingTogether: (Int, Int) => Boolean = {
    case (first, second) => true
  }

  private val groupNothingTogether: (Int, Int) => Boolean = {
    case (first, second) => false
  }

  private val groupEqualTogether: (Int, Int) => Boolean = _ == _

  private val predicateGenerator = Gen.oneOf(
    Gen.const(groupEverythingTogether) :| "Group everything together",
    Gen.const(groupNothingTogether) :| "Group nothing together",
    Gen.const(groupEqualTogether) :| "Group equal things together"
  )

  private val inputSequenceGenerator =
    Gen.nonEmptyListOf(Arbitrary.arbitrary[Int])


  "groupWhile" should "respect the exact sequence type that it works on" in {
    "val groups: Seq[List[Int]] = List(1, 2, 2).groupWhile(groupEverythingTogether)" should compile
    "val groups: Seq[List[Int]] = Seq(1, 2, 2).groupWhile(groupEverythingTogether)" shouldNot typeCheck
  }

  it should "result in an empty sequence of groups when presented with an empty input sequence" in {
    forAll(predicateGenerator) { predicate =>
      List
        .empty[Int]
        .groupWhile(predicate) should be(Seq.empty[List[Int]])
    }
  }

  it should "yield non empty groups if the input sequence is not empty" in
    forAll(predicateGenerator, inputSequenceGenerator) {
      (predicate, inputSequence) =>
        val groups = inputSequence.groupWhile(groupEverythingTogether)
        all(groups) should not be empty
    }

  it should "preserve all items in the input sequence" in {
    val bagConfiguration = HashedBagConfiguration.compact[Int]
    val emptyBag         = HashBag.empty(bagConfiguration)
    forAll(predicateGenerator, inputSequenceGenerator) {
      (predicate, inputSequence) =>
        val expectedItemsAsBag = (emptyBag /: inputSequence)(_ + _)
        val actualItems        = inputSequence.groupWhile(predicate) flatMap identity
        val actualItemsAsBag   = (emptyBag /: actualItems)(_ + _)
        actualItemsAsBag should contain theSameElementsAs expectedItemsAsBag
    }
  }

  it should "preserve the order of items in the input sequence" in
    forAll(predicateGenerator, inputSequenceGenerator) {
      (predicate, inputSequence) =>
        val actualItems = inputSequence.groupWhile(predicate) flatMap identity
        actualItems should contain theSameElementsInOrderAs inputSequence
    }

  it should "fragment the input sequence into single item groups if the predicate is always false" in
    forAll(inputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupNothingTogether)
      all(groups map (_.loneElement))
    }

  it should "reproduce the input sequence as a single group if the predicate is always true" in
    forAll(inputSequenceGenerator) { inputSequence =>
      val groups = inputSequence.groupWhile(groupEverythingTogether)
      groups.loneElement should contain theSameElementsInOrderAs inputSequence
    }

  it should "identify runs of adjacent duplicates if the predicate is equality" in
    forAll(inputSequenceGenerator) { inputSequence =>
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
}
