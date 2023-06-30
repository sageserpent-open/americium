package com.sageserpent.americium.java

import java.util.Iterator as JavaIterator
import scala.collection.Iterator as ScalaIterator

object CrossApiIterator {
  def from[Element](
      underlying: ScalaIterator[Element]
  ): JavaIterator[Element] with ScalaIterator[Element] =
    new ScalaIterator[Element] with JavaIterator[Element] {
      override def hasNext: Boolean = underlying.hasNext
      override def next(): Element  = underlying.next()
    }
}
