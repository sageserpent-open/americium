package com.sageserpent.infrastructure

import scala.collection.immutable.List

trait ListEnrichment {
  implicit class RichList[Item](items: Seq[Item]) {
    def groupWhile(predicate: (Item, Item) => Boolean) = {
      if (items.isEmpty)
        Seq.empty[Seq[Item]]
      else
      {
        val Seq(head, tail @ _*) = items
        val reversedGroupsInReverse = tail.foldLeft (List(List(head))) ((groups, item) => {
          assert(groups.nonEmpty)
          groups match {
            case (headGroup @ (itemToMatch :: _)) :: tailGroups if predicate(itemToMatch, item) => (item :: headGroup) :: tailGroups
            case _ => List(item) :: groups
          }
        })
        reversedGroupsInReverse map (_.reverse) reverse
      }
    }
  }
}