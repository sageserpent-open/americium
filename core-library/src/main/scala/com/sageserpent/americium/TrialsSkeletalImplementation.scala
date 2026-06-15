package com.sageserpent.americium
import cats.implicits.*
import com.sageserpent.americium.TrialsApis.scalaApi
import com.sageserpent.americium.generation.FiltrationResult
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.tupleTrials.Tuple2Trials as ScalaTuple2Trials
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsScaffolding as ScalaTrialsScaffolding
}

import scala.collection.immutable.{SortedMap, SortedSet}

trait TrialsSkeletalImplementation[Case] extends ScalaTrials[Case] {
  override def map[TransformedCase](
      transform: Case => TransformedCase
  ): TrialsImplementation[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def filter(
      predicate: Case => Boolean
  ): TrialsImplementation[Case] = {
    flatMap(caze =>
      new TrialsImplementation(
        FiltrationResult(Some(caze).filter(predicate))
      )
    )
  }

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): TrialsImplementation[TransformedCase] =
    flatMap(caze =>
      new TrialsImplementation(
        FiltrationResult(filteringTransform(caze))
      )
    )

  override def flatMap[TransformedCase](
      step: Case => ScalaTrials[TransformedCase]
  ): TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def and[Case2](
      secondTrials: ScalaTrials[Case2]
  ): ScalaTrialsScaffolding.Tuple2Trials[Case, Case2] =
    new ScalaTuple2Trials(this, secondTrials)

  override def or[Case2](
      alternativeTrials: ScalaTrials[Case2]
  ): TrialsImplementation[Either[Case, Case2]] = scalaApi.alternate(
    this.map(Either.left[Case, Case2]),
    alternativeTrials.map(Either.right)
  )

  override def options: TrialsImplementation[Option[Case]] =
    scalaApi.alternate(scalaApi.only(None), this.map(Some.apply[Case]))

  override def collections[Container](implicit
      factory: collection.Factory[Case, Container]
  ): ScalaTrials[Container]

  override def several[Container](implicit
      factory: collection.Factory[Case, Container]
  ): ScalaTrials[Container] = collections(factory)

  override def nonEmptyCollections[Container](implicit
      factory: collection.Factory[Case, Container]
  ): ScalaTrials[Container]

  /** @deprecated
    *   Use [[nonEmptyCollections]] instead.
    */
  @deprecated("Use 'nonEmptyCollections' instead.")
  def nonEmptySeveral[Container](implicit
      factory: collection.Factory[Case, Container]
  ): ScalaTrials[Container] = nonEmptyCollections(factory)

  override def lists: ScalaTrials[List[Case]] = collections

  override def nonEmptyLists: ScalaTrials[List[Case]] = nonEmptyCollections

  override def sets: ScalaTrials[Set[Case]] = collections

  override def nonEmptySets: ScalaTrials[Set[Case]] = nonEmptyCollections

  override def sortedSets(implicit
      ordering: Ordering[Case]
  ): ScalaTrials[SortedSet[Case]] =
    lists.map(SortedSet.from[Case](_)(ordering))

  override def nonEmptySortedSets(implicit
      ordering: Ordering[Case]
  ): ScalaTrials[SortedSet[Case]] =
    nonEmptyLists.map(SortedSet.from[Case](_)(ordering))

  override def maps[Value](
      values: ScalaTrials[Value]
  ): ScalaTrials[Map[Case, Value]] =
    flatMap(key => values.map(key -> _)).collections[Map[Case, Value]]

  override def nonEmptyMaps[Value](
      values: ScalaTrials[Value]
  ): ScalaTrials[Map[Case, Value]] =
    flatMap(key => values.map(key -> _)).nonEmptyCollections[Map[Case, Value]]

  override def sortedMaps[Value](
      values: ScalaTrials[Value]
  )(implicit
      ordering: Ordering[Case]
  ): ScalaTrials[SortedMap[Case, Value]] =
    flatMap(key => values.map(key -> _)).lists
      .map(SortedMap.from[Case, Value](_)(ordering))

  override def nonEmptySortedMaps[Value](
      values: ScalaTrials[Value]
  )(implicit
      ordering: Ordering[Case]
  ): ScalaTrials[SortedMap[Case, Value]] =
    flatMap(key => values.map(key -> _)).nonEmptyLists
      .map(SortedMap.from[Case, Value](_)(ordering))

  override def lotsOfSize[Collection](size: Int)(implicit
      factory: collection.Factory[Case, Collection]
  ): ScalaTrials[Collection]

  override def listsOfSize(
      size: Int
  ): ScalaTrials[List[Case]] = lotsOfSize(
    size
  )
}
