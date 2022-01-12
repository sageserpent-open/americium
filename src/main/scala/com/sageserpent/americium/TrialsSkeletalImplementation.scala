package com.sageserpent.americium
import com.sageserpent.americium.{Trials => ScalaTrials}

import scala.collection.immutable.{SortedMap, SortedSet}

trait TrialsSkeletalImplementation[Case] extends ScalaTrials[Case] {
  override def lists: ScalaTrials[List[Case]] = several

  override def sets: ScalaTrials[Set[_ <: Case]] = several

  override def sortedSets(implicit
      ordering: Ordering[_ >: Case]
  ): ScalaTrials[SortedSet[_ <: Case]] =
    lists.map(SortedSet.from[Case](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def maps[Value](
      values: ScalaTrials[Value]
  ): ScalaTrials[Map[Case, Value]] =
    flatMap(key => values.map(key -> _)).several[Map[Case, Value]]

  override def sortedMaps[Value](values: ScalaTrials[Value])(implicit
      ordering: Ordering[_ >: Case]
  ): ScalaTrials[SortedMap[Case, Value]] = flatMap(key =>
    values.map(key -> _)
  ).lists
    .map(SortedMap.from[Case, Value](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def listsOfSize(size: Int): ScalaTrials[List[Case]] = lotsOfSize(
    size
  )
}
