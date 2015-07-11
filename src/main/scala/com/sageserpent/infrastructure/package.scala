package com.sageserpent

import scala.util.Random
import scala.collection.immutable.List

package object infrastructure {
  implicit def enrich(random: Random) = new RichRandom(random)
  
  implicit def enrich[X](list: List[X]) = new RichList(list)
}