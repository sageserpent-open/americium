package com.sageserpent.americium

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.List
import scalaz.std.stream

trait SeqEnrichment {
  implicit class RichSeq[Container[Item] <: Seq[Item], Item](
      items: Container[Item]) {
    def groupWhile(predicate: (Item, Item) => Boolean)(
        implicit cbf: CanBuildFrom[Container[Item], Item, Container[Item]])
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

    def zipN[InnerContainer[Element] <: Seq[Element], Element](
        implicit cbf: CanBuildFrom[InnerContainer[Element],
                                   Element,
                                   InnerContainer[Element]],
        evidence: Item <:< InnerContainer[Element])
      : Stream[InnerContainer[Element]] = {
      def linkAndRemainingInnerSequencesFrom(innerSequences: Seq[Seq[Element]])
        : Option[(InnerContainer[Element], Seq[Seq[Element]])] = {
        val nonEmptyInnerSequences = innerSequences filter (_.nonEmpty)
        if (nonEmptyInnerSequences.nonEmpty) {
          val (link, remainingInnerSequences) =
            (nonEmptyInnerSequences map (innerSequence =>
              innerSequence.head -> innerSequence.tail)).unzip
          val convertedLink = {
            val builder = cbf()
            link.foreach(builder += _)
            builder.result()
          }
          Some(convertedLink -> remainingInnerSequences)
        } else None
      }
      stream.unfold(items.asInstanceOf[Seq[Seq[Element]]])(
        linkAndRemainingInnerSequencesFrom)
    }
  }
}
