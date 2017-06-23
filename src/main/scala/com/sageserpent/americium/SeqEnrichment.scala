package com.sageserpent.americium

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.List

trait SeqEnrichment {
  implicit class RichSeq[Container[Item] <: Seq[Item], Item](items: Container[Item]) {
    def groupWhile(predicate: (Item, Item) => Boolean)(
        implicit cbf: CanBuildFrom[List[Item], Item, Container[Item]])
      : Seq[Container[Item]] = {
      if (items.isEmpty)
        Seq.empty[Container[Item]]
      else {
        val Seq(head, tail @ _ *) = items
        val reversedGroupsInReverse =
          tail.foldLeft(List(List(head)))((groups, item) => {
            assert(groups.nonEmpty)
            groups match {
              case (headGroup @ (itemToMatch :: _)) :: tailGroups
                  if predicate(itemToMatch, item) =>
                (item :: headGroup) :: tailGroups
              case _ => List(item) :: groups
            }
          })
        reversedGroupsInReverse map (_.reverse) map { items =>
          val builder = cbf()
          items.foreach(builder += _)
          builder.result
        } reverse
      }
    }

    def zipN[InnerContainer[Element] <: Seq[Element], Element](implicit evidence: Item <:< InnerContainer[Element]): Stream[InnerContainer[Element]] = Stream.empty[InnerContainer[Element]]
  }
}
