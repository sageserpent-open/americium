package com.sageserpent.americium.java

import com.google.common.collect.{Ordering => _, _}
import com.sageserpent.americium.java.{Trials => JavaTrials}

import _root_.java.util.function.{
  Function => JavaFunction,
  Supplier => JavaSupplier
}
import _root_.java.util.{
  Comparator => JavaComparator,
  HashMap => JavaHashMap,
  Map => JavaMap
}

trait TrialsSkeletalImplementation[Case] extends JavaTrials[Case] {
  override def flatMap[TransformedCase](
      step: JavaFunction[Case, JavaTrials[TransformedCase]]
  ): TrialsSkeletalImplementation[TransformedCase]

  protected def lotsOfSize[Collection](
      size: Int,
      builderFactory: => Builder[Case, Collection]
  ): JavaTrials[Collection]

  protected def several[Collection](
      builderFactory: => Builder[Case, Collection]
  ): JavaTrials[Collection]

  override def collections[Collection](
      builderFactory: JavaSupplier[
        Builder[Case, Collection]
      ]
  ): JavaTrials[Collection] =
    several(builderFactory.get())

  override def immutableLists(): JavaTrials[ImmutableList[Case]] =
    several(new Builder[Case, ImmutableList[Case]] {
      private val underlyingBuilder = ImmutableList.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableList[Case] =
        underlyingBuilder.build()
    })

  override def immutableSets(): JavaTrials[ImmutableSet[Case]] =
    several(new Builder[Case, ImmutableSet[Case]] {
      private val underlyingBuilder = ImmutableSet.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSet[Case] =
        underlyingBuilder.build()
    })

  override def immutableSortedSets(
      elementComparator: JavaComparator[Case]
  ): JavaTrials[ImmutableSortedSet[Case]] =
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
  ): JavaTrials[ImmutableMap[Case, Value]] =
    flatMap(key => values.map(key -> _)).several(
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
      elementComparator: JavaComparator[Case],
      values: JavaTrials[Value]
  ): JavaTrials[ImmutableSortedMap[Case, Value]] =
    flatMap(key => values.map(key -> _)).several(
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
      builderFactory: JavaSupplier[
        Builder[Case, Collection]
      ]
  ): JavaTrials[Collection] =
    lotsOfSize(size, builderFactory.get())

  override def immutableListsOfSize(
      size: Int
  ): JavaTrials[ImmutableList[Case]] =
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
