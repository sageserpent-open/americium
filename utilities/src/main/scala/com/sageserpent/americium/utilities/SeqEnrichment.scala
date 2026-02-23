package com.sageserpent.americium.utilities

import scala.collection.BuildFrom
import scala.language.postfixOps

trait SeqEnrichment {
  implicit class RichSeq[Container[X] <: Seq[X], Item](
      items: Container[Item]
  ) {
    def groupWhile(predicate: (Item, Item) => Boolean)(implicit
        bf: BuildFrom[Container[Item], Item, Container[Item]]
    ): Seq[Container[Item]] = {
      if (items.isEmpty)
        Seq.empty[Container[Item]]
      else {
        val Seq(head, tail @ _*)    = items: @unchecked
        val reversedGroupsInReverse =
          tail.foldLeft(List(List(head)))((groups, item) => {
            assert(groups.nonEmpty)
            groups match {
              case (headGroup @ itemToMatch :: _) :: tailGroups
                  if predicate(itemToMatch, item) =>
                (item :: headGroup) :: tailGroups
              case _ => List(item) :: groups
            }
          })
        reversedGroupsInReverse map (_.reverse) map { group =>
          val builder = bf.newBuilder(items)
          group.foreach(builder += _)
          builder.result
        } reverse
      }
    }
  }

  implicit class RichSequenceOfSequences[
      Container[_],
      InnerContainer[Subelement] <: Iterable[Subelement],
      Subelement
  ](innerSequences: Container[InnerContainer[Subelement]]) {

    def zipN(implicit
        bf: BuildFrom[InnerContainer[Subelement], Subelement, InnerContainer[
          Subelement
        ]]
    ): LazyList[InnerContainer[Subelement]] = {
      def linkAndRemainingInnerSequencesFrom(
          innerSequences: Seq[InnerContainer[Subelement]]
      ): LazyList[InnerContainer[Subelement]] = {
        val nonEmptyInnerSequences: Seq[InnerContainer[Subelement]] =
          innerSequences filter (_.nonEmpty)
        if (nonEmptyInnerSequences.nonEmpty) {
          val (link, remainingInnerSequences) =
            (nonEmptyInnerSequences map (innerSequence =>
              innerSequence.head -> innerSequence.tail
                .asInstanceOf[InnerContainer[Subelement]]
            )).unzip
          val convertedLink = {
            val builder = bf.newBuilder(innerSequences.head)
            link.foreach(builder += _)
            builder.result()
          }
          convertedLink #:: linkAndRemainingInnerSequencesFrom(
            remainingInnerSequences
          )
        } else LazyList.empty
      }
      linkAndRemainingInnerSequencesFrom(
        innerSequences.asInstanceOf[Seq[InnerContainer[Subelement]]]
      )
    }
  }
}
