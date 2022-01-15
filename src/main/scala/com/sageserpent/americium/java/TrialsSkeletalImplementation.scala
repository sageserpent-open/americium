package com.sageserpent.americium.java

import com.google.common.collect.{Ordering => _, _}
import com.sageserpent.americium.TrialsImplementation
import com.sageserpent.americium.java.tupleTrials.{
  Tuple2Trials => JavaTuple2Trials
}
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsScaffolding => JavaTrialsScaffolding
}

import _root_.java.util.function.{Predicate, Supplier, Function => JavaFunction}
import _root_.java.util.{
  Comparator,
  Optional,
  HashMap => JavaHashMap,
  Map => JavaMap
}

trait TrialsSkeletalImplementation[Case] extends JavaTrials[Case] {
  override def scalaTrials(): TrialsImplementation[Case]

  override def map[TransformedCase](
      transform: JavaFunction[Case, TransformedCase]
  ): TrialsSkeletalImplementation[TransformedCase] =
    scalaTrials().map(transform.apply).javaTrials

  override def flatMap[TransformedCase](
      step: JavaFunction[Case, JavaTrials[TransformedCase]]
  ): TrialsSkeletalImplementation[TransformedCase] =
    scalaTrials().flatMap(step.apply _ andThen (_.scalaTrials)).javaTrials

  override def filter(
      predicate: Predicate[Case]
  ): TrialsSkeletalImplementation[Case] =
    scalaTrials().filter(predicate.test).javaTrials

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[Case, Optional[TransformedCase]]
  ): TrialsSkeletalImplementation[TransformedCase] =
    scalaTrials()
      .mapFilter(filteringTransform.apply _ andThen {
        case withPayload if withPayload.isPresent => Some(withPayload.get())
        case _                                    => None
      })
      .javaTrials

  override def and[Case2](
      secondTrials: JavaTrials[Case2]
  ): JavaTrialsScaffolding.Tuple2Trials[Case, Case2] =
    new JavaTuple2Trials(this, secondTrials)

  protected def lotsOfSize[Collection](
      size: Int,
      builderFactory: => Builder[Case, Collection]
  ): TrialsSkeletalImplementation[Collection]

  protected def several[Collection](
      builderFactory: => Builder[Case, Collection]
  ): TrialsSkeletalImplementation[Collection]

  override def collections[Collection](
      builderFactory: Supplier[
        Builder[Case, Collection]
      ]
  ): TrialsSkeletalImplementation[Collection] =
    several(builderFactory.get())

  override def immutableLists()
      : TrialsSkeletalImplementation[ImmutableList[Case]] =
    several(new Builder[Case, ImmutableList[Case]] {
      private val underlyingBuilder = ImmutableList.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableList[Case] =
        underlyingBuilder.build()
    })

  override def immutableSets()
      : TrialsSkeletalImplementation[ImmutableSet[Case]] =
    several(new Builder[Case, ImmutableSet[Case]] {
      private val underlyingBuilder = ImmutableSet.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSet[Case] =
        underlyingBuilder.build()
    })

  override def immutableSortedSets(
      elementComparator: Comparator[Case]
  ): TrialsSkeletalImplementation[ImmutableSortedSet[Case]] =
    several(new Builder[Case, ImmutableSortedSet[Case]] {
      private val underlyingBuilder: ImmutableSortedSet.Builder[Case] =
        new ImmutableSortedSet.Builder(elementComparator)

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSortedSet[Case] =
        underlyingBuilder.build()
    })

  override def immutableMaps[Value](
      values: JavaTrials[Value]
  ): TrialsSkeletalImplementation[ImmutableMap[Case, Value]] =
    flatMap(key => values.map(key -> _))
      .several(
        new Builder[(Case, Value), ImmutableMap[Case, Value]] {
          val accumulator: JavaMap[Case, Value] =
            new JavaHashMap()

          override def add(entry: (Case, Value)): Unit = {
            accumulator.put(entry._1, entry._2)
          }
          override def build(): ImmutableMap[Case, Value] = {
            ImmutableMap.copyOf(accumulator)
          }
        }
      )

  override def immutableSortedMaps[Value](
      elementComparator: Comparator[Case],
      values: JavaTrials[Value]
  ): TrialsSkeletalImplementation[ImmutableSortedMap[Case, Value]] =
    flatMap(key => values.map(key -> _))
      .several(
        new Builder[
          (Case, Value),
          ImmutableSortedMap[Case, Value]
        ] {
          val accumulator: JavaMap[Case, Value] =
            new JavaHashMap()

          override def add(entry: (Case, Value)): Unit = {
            accumulator.put(entry._1, entry._2)
          }
          override def build(): ImmutableSortedMap[Case, Value] = {
            ImmutableSortedMap.copyOf(accumulator, elementComparator)
          }
        }
      )

  override def collectionsOfSize[Collection](
      size: Int,
      builderFactory: Supplier[
        Builder[Case, Collection]
      ]
  ): TrialsSkeletalImplementation[Collection] =
    lotsOfSize(size, builderFactory.get())

  override def immutableListsOfSize(
      size: Int
  ): TrialsSkeletalImplementation[ImmutableList[Case]] =
    lotsOfSize(
      size,
      new Builder[Case, ImmutableList[Case]] {
        private val underlyingBuilder = ImmutableList.builder[Case]()

        override def add(caze: Case): Unit = {
          underlyingBuilder.add(caze)
        }

        override def build(): ImmutableList[Case] =
          underlyingBuilder.build()
      }
    )

}
