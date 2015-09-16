package com.sageserpent.infrastructure

import com.sageserpent.infrastructure
import infrastructure.seqEnrichment$._

import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

import scala.collection.immutable.{HashBag, HashedBagConfiguration}

/**
 * Created by Gerard on 15/09/2015.
 */
class RichSeqSpec extends FlatSpec with Checkers {
  private val groupEverythingTogether: (Int, Int) => Boolean = {
    case (first, second) => true
  }

  private val groupNothingTogether: (Int, Int) => Boolean = {
    case (first, second) => false
  }

  private val groupEqualTogether: (Int, Int) => Boolean = _ == _

  private val predicateGenerator = Gen.oneOf(Gen.const(groupEverythingTogether) :| "Group everything together", Gen.const(groupNothingTogether) :| "Group nothing together", Gen.const(groupEqualTogether) :| "Group equal things together")

  private val inputSequenceGenerator = Gen.nonEmptyListOf(Arbitrary.arbitrary[Int])

  "groupWhile" should "result in an empty sequence of groups when presented with an empty input sequence" in {
    check(Prop.forAll(predicateGenerator)(predicate => (Seq.empty[Seq[Int]] === Seq.empty[Int].groupWhile(predicate)) :| "Seq.empty[Seq[Int]] === BargainBasement.groupWhile(Seq.empty[Int], predicate)"))
  }

  it should "yield non empty groups if the input sequence is not empty" in {
    check(Prop.forAll(predicateGenerator, inputSequenceGenerator)((predicate, inputSequence) => {
      val groups = inputSequence.groupWhile(groupEverythingTogether)
      Prop.all(groups map (_.nonEmpty :| s"Each group should be non-empty"): _*)
    }))
  }

  it should "preserve all items in the input sequence" in {
    val bagConfiguration = HashedBagConfiguration.compact[Int]
    val emptyBag = HashBag.empty(bagConfiguration)
    check(Prop.forAll(predicateGenerator, inputSequenceGenerator)((predicate, inputSequence) => {
      val expectedItemsAsBag = (emptyBag /: inputSequence)(_ + _)
      val actualItems = inputSequence.groupWhile(predicate) flatMap identity
      val actualItemsAsBag = (emptyBag /: actualItems)(_ + _)
      (expectedItemsAsBag === actualItemsAsBag) :| s"(${expectedItemsAsBag} === ${actualItemsAsBag})"
    }))
  }

  it should "preserve the order of items in the input sequence" in {
    check(Prop.forAll(predicateGenerator, inputSequenceGenerator)((predicate, inputSequence) => {
      val actualItems = inputSequence.groupWhile(predicate) flatMap identity
      (inputSequence === actualItems) :| s"(${inputSequence} === ${actualItems})"
    }))
  }

  it should "fragment the input sequence into single item groups if the predicate is always false" in {
    check(Prop.forAll(inputSequenceGenerator)(inputSequence => {
      val groups = inputSequence.groupWhile(groupNothingTogether)
      Prop.all(groups map (group => PartialFunction.cond(group) { case Seq(_) => true } :| s"${group} should contain one and only one item"): _*)
    }))
  }

  it should "reproduce the input sequence as a single group if the predicate is always true" in {
    check(Prop.forAll(inputSequenceGenerator)(inputSequence => {
      val groups = inputSequence.groupWhile(groupEverythingTogether)
      PartialFunction.cond(groups) { case Seq(allItems) => inputSequence === allItems } :| s"${groups} should consist of a single group that is ${inputSequence}"
    }))
  }

  it should "identify runs of adjacent duplicates if the predicate is equality" in {
    check(Prop.forAll(inputSequenceGenerator)(inputSequence => {
      val groups = inputSequence.groupWhile(groupEqualTogether)
      Prop.all(groups filter (1 < _.size) map (groupOfDuplicates => (1 == groupOfDuplicates.distinct.size) :| s"${groupOfDuplicates} should contain only duplicate items"): _*) &&
        Prop.all(groups zip groups.tail map (groupPair => groupPair match {
          case (Seq(first), Seq(second)) => (first != second) :| s"Items from adjacent single-item groups should be unequal"
          case _ => Prop(true)
        }): _ *)
    }))
  }
}
