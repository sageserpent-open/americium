package com.sageserpent.infrastructure

import org.scalatest.Suite

object BargainBasement {
  def pairwise(iterable: Iterable[_]) = {
    for (Seq(first, second) <- iterable.sliding(2))
      yield first -> second
  }
  
  def isSorted[X <% Ordered[X]](iterable: Iterable[X]) = {
    iterable.sliding(2).forall {
      case Seq(first, second) => first <= second
      case _                  => true
    }
  }

  def memoize[X, Y](computation: X => Y) = {
    val cache = scala.collection.mutable.Map[X, Y]()

    input: X => {
      if (cache.contains(input)) {
        cache(input)
      } else {
        val result = computation(input)
        cache += (input -> result)
        result
      }
    }
  }
}

class TestSuite2 extends Suite {
  def testIsSorted() = {
    assert(BargainBasement.isSorted(List.empty))

    assert(BargainBasement.isSorted(List(1)))

    assert(BargainBasement.isSorted(List(1, 2)))

    assert(!BargainBasement.isSorted(List(2, 1)))

    assert(BargainBasement.isSorted(List(1, 2, 3)))

    assert(BargainBasement.isSorted(List(1, 2, 2)))

    assert(BargainBasement.isSorted(List(1, 1, 2)))

    assert(BargainBasement.isSorted(List(1, 2, 2, 3)))

    assert(!BargainBasement.isSorted(List(1, 2, 1)))

    assert(BargainBasement.isSorted(List(1, 2, 2, 4)))

    assert(BargainBasement.isSorted(List(1, 2, 3, 4)))

    assert(!BargainBasement.isSorted(List(3, 2, 2, 4)))

    assert(!BargainBasement.isSorted(List(1, 2, 2, 0)))

    assert(!BargainBasement.isSorted(List(1, 2, 4, 0)))
  }
}