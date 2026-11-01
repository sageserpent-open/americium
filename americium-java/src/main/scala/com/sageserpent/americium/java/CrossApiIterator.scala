package com.sageserpent.americium.java

import java.util.Iterator as JavaIterator
import scala.collection.Iterator as ScalaIterator

object CrossApiIterator {
  def from[Element](
      underlying: ScalaIterator[Element]
  ): CrossApiIterator[Element] =
    new CrossApiIterator[Element] {
      override def hasNext: Boolean = underlying.hasNext
      override def next(): Element  = underlying.next()
    }
}

trait CrossApiIterator[Element]
    extends JavaIterator[Element]
    with ScalaIterator[Element] {
  source =>
  override def map[Transformed](
      transformation: Element => Transformed
  ): CrossApiIterator[Transformed] =
    new CrossApiIterator[Transformed] {
      override def hasNext: Boolean    = source.hasNext
      override def next(): Transformed = transformation(source.next())
    }
}
