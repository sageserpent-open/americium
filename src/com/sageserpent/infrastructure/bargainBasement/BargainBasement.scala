package com.sageserpent.infrastructure

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

