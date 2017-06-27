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
  }

  implicit class RichSequenceOfSequences[Container[Item] <: Seq[Item] forSome {
    type Item
  }, InnerContainer[Subelement] <: Traversable[Subelement], Subelement](
      innerSequences: Container[InnerContainer[Subelement]]) {

    def zipN(
        implicit cbf: CanBuildFrom[InnerContainer[Subelement],
                                   Subelement,
                                   InnerContainer[Subelement]])
      : Stream[InnerContainer[Subelement]] = {
      def linkAndRemainingInnerSequencesFrom(
          innerSequences: Seq[Traversable[Subelement]]): Option[
        (InnerContainer[Subelement], Seq[Traversable[Subelement]])] = {
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
      stream.unfold(innerSequences.asInstanceOf[Seq[Traversable[Subelement]]])(
        linkAndRemainingInnerSequencesFrom)
    }
  }
}
