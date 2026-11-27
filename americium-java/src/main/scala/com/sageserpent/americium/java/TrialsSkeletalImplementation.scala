package com.sageserpent.americium.java

import com.google.common.collect.{Ordering as _, *}
import com.sageserpent.americium.java.TrialsApis.javaApi
import com.sageserpent.americium.java.tupleTrials.Tuple2Trials as JavaTuple2Trials
import com.sageserpent.americium.java.{
  Trials as JavaTrials,
  TrialsScaffolding as JavaTrialsScaffolding
}
import com.sageserpent.americium.Trials as ScalaTrials
import cyclops.control.Either as JavaEither

import _root_.java.util.function.{Predicate, Supplier, Function as JavaFunction}
import _root_.java.util.{
  Comparator,
  Optional,
  HashMap as JavaHashMap,
  Map as JavaMap
}

trait TrialsSkeletalImplementation[Case] extends JavaTrials[Case] {
  override def scalaTrials(): ScalaTrials[Case]

  override def filter(
      predicate: Predicate[Case]
  ): JavaTrials[Case] =
    TrialsImplementation(scalaTrials().filter(predicate.test))

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[Case, Optional[TransformedCase]]
  ): JavaTrials[TransformedCase] =
    TrialsImplementation(
      scalaTrials().mapFilter(filteringTransform.apply _ andThen {
        case withPayload if withPayload.isPresent => Some(withPayload.get())
        case _                                    => None
      })
    )

  override def and[Case2](
      secondTrials: JavaTrials[Case2]
  ): JavaTrialsScaffolding.Tuple2Trials[Case, Case2] =
    new JavaTuple2Trials(this, secondTrials)

  override def or[Case2](
      alternativeTrials: JavaTrials[Case2]
  ): JavaTrials[
    JavaEither[Case, Case2]
  ] = javaApi.alternate(
    this.map(JavaEither.left[Case, Case2]),
    alternativeTrials.map(JavaEither.right[Case, Case2])
  )

  override def map[TransformedCase](
      transform: JavaFunction[Case, TransformedCase]
  ): JavaTrials[TransformedCase] =
    TrialsImplementation(scalaTrials().map(transform.apply))

  override def optionals(): JavaTrials[
    Optional[Case]
  ] = javaApi.alternate(
    javaApi.only(Optional.empty()),
    this.map(Optional.of[Case])
  )

  override def collections[Collection](
      builderFactory: Supplier[
        Builder[Case, Collection]
      ]
  ): JavaTrials[Collection] =
    severalImplementation(builderFactory.get())

  override def nonEmptyCollections[Collection](
      builderFactory: Supplier[
        Builder[Case, Collection]
      ]
  ): JavaTrials[Collection] =
    nonEmptySeveralImplementation(builderFactory.get())

  override def immutableLists(): JavaTrials[ImmutableList[Case]] =
    severalImplementation(new Builder[Case, ImmutableList[Case]] {
      private val underlyingBuilder = ImmutableList.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableList[Case] =
        underlyingBuilder.build()
    })

  override def nonEmptyImmutableLists(): JavaTrials[ImmutableList[Case]] =
    nonEmptySeveralImplementation(new Builder[Case, ImmutableList[Case]] {
      private val underlyingBuilder = ImmutableList.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableList[Case] =
        underlyingBuilder.build()
    })

  override def immutableSets(): JavaTrials[ImmutableSet[Case]] =
    severalImplementation(new Builder[Case, ImmutableSet[Case]] {
      private val underlyingBuilder = ImmutableSet.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSet[Case] =
        underlyingBuilder.build()
    })

  override def nonEmptyImmutableSets(): JavaTrials[ImmutableSet[Case]] =
    nonEmptySeveralImplementation(new Builder[Case, ImmutableSet[Case]] {
      private val underlyingBuilder = ImmutableSet.builder[Case]()

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSet[Case] =
        underlyingBuilder.build()
    })

  override def immutableSortedSets(
      elementComparator: Comparator[Case]
  ): JavaTrials[ImmutableSortedSet[Case]] =
    severalImplementation(new Builder[Case, ImmutableSortedSet[Case]] {
      private val underlyingBuilder: ImmutableSortedSet.Builder[Case] =
        new ImmutableSortedSet.Builder(elementComparator)

      override def add(caze: Case): Unit = {
        underlyingBuilder.add(caze)
      }

      override def build(): ImmutableSortedSet[Case] =
        underlyingBuilder.build()
    })

  override def nonEmptyImmutableSortedSets(
      elementComparator: Comparator[Case]
  ): JavaTrials[ImmutableSortedSet[Case]] =
    nonEmptySeveralImplementation(new Builder[Case, ImmutableSortedSet[Case]] {
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
  ): JavaTrials[ImmutableMap[Case, Value]] = {
    val paired: JavaTrials[(Case, Value)] = flatMap(key => values.map(key -> _))
    paired match {
      case skeletal: TrialsSkeletalImplementation[(Case, Value)] =>
        skeletal.severalImplementation(
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
      case other =>
        TrialsImplementation(other.scalaTrials).severalImplementation(
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
    }
  }

  override def nonEmptyImmutableMaps[Value](
      values: JavaTrials[Value]
  ): JavaTrials[ImmutableMap[Case, Value]] = {
    val paired: JavaTrials[(Case, Value)] = flatMap(key => values.map(key -> _))
    paired match {
      case skeletal: TrialsSkeletalImplementation[(Case, Value)] =>
        skeletal.nonEmptySeveralImplementation(
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
      case other =>
        TrialsImplementation(other.scalaTrials).nonEmptySeveralImplementation(
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
    }
  }

  override def nonEmptyImmutableSortedMaps[Value](
      elementComparator: Comparator[Case],
      values: JavaTrials[Value]
  ): JavaTrials[ImmutableSortedMap[Case, Value]] = {
    val paired: JavaTrials[(Case, Value)] = flatMap(key => values.map(key -> _))
    paired match {
      case skeletal: TrialsSkeletalImplementation[(Case, Value)] =>
        skeletal.nonEmptySeveralImplementation(
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
      case other =>
        TrialsImplementation(other.scalaTrials).nonEmptySeveralImplementation(
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
    }
  }

  override def immutableSortedMaps[Value](
      elementComparator: Comparator[Case],
      values: JavaTrials[Value]
  ): JavaTrials[ImmutableSortedMap[Case, Value]] = {
    val paired: JavaTrials[(Case, Value)] = flatMap(key => values.map(key -> _))
    paired match {
      case skeletal: TrialsSkeletalImplementation[(Case, Value)] =>
        skeletal.severalImplementation(
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
      case other =>
        TrialsImplementation(other.scalaTrials).severalImplementation(
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
    }
  }

  override def flatMap[TransformedCase](
      step: JavaFunction[Case, JavaTrials[TransformedCase]]
  ): JavaTrials[TransformedCase] =
    TrialsImplementation(
      scalaTrials().flatMap(step.apply _ andThen (_.scalaTrials))
    )

  override def collectionsOfSize[Collection](
      size: Int,
      builderFactory: Supplier[
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

  def lotsOfSize[Collection](
      size: Int,
      builderFactory: => Builder[Case, Collection]
  ): JavaTrials[Collection] =
    TrialsImplementation(
      scalaTrials() match {
        case impl: com.sageserpent.americium.TrialsImplementation[Case] =>
          impl.lotsOfSize(
            size,
            new com.sageserpent.americium.Builder[Case, Collection] {
              private val b                         = builderFactory
              override def add(element: Case): Unit = b.add(element)
              override def build(): Collection      = b.build()
            }
          )
        case other =>
          val bFactory = () => builderFactory
          val scalaBuilderFactory
              : com.sageserpent.americium.Builder[Case, Collection] =
            new com.sageserpent.americium.Builder[Case, Collection] {
              private val b                         = bFactory()
              override def add(element: Case): Unit = b.add(element)
              override def build(): Collection      = b.build()
            }
          // Fallback via Scala API complexities
          import com.sageserpent.americium.TrialsApis.scalaApi
          scalaApi.complexities.flatMap(complexity => {
            def addItems(
                numberOfItems: Int,
                partialResult: List[Case]
            ): ScalaTrials[Collection] =
              if (0 >= numberOfItems)
                scalaApi.only {
                  val builder = bFactory()
                  partialResult.foreach(builder.add)
                  builder.build()
                }
              else
                other.flatMap((item: Case) =>
                  (scalaApi.resetComplexity(complexity): ScalaTrials[Unit])
                    .flatMap((_: Unit) =>
                      addItems(
                        numberOfItems - 1,
                        item :: partialResult
                      )
                    )
                )

            addItems(size, Nil)
          })
      }
    )

  def severalImplementation[Collection](
      builderFactory: => Builder[Case, Collection]
  ): JavaTrials[Collection] =
    TrialsImplementation(
      scalaTrials() match {
        case impl: com.sageserpent.americium.TrialsImplementation[Case] =>
          impl.severalImplementation(
            new com.sageserpent.americium.Builder[Case, Collection] {
              private val b                         = builderFactory
              override def add(element: Case): Unit = b.add(element)
              override def build(): Collection      = b.build()
            }
          )
        case other =>
          import com.sageserpent.americium.TrialsApis.scalaApi
          val bFactory = () => builderFactory
          def addItems(partialResult: List[Case]): ScalaTrials[Collection] =
            scalaApi.alternate(
              scalaApi.only {
                val builder = bFactory()
                partialResult.foreach(builder.add)
                builder.build()
              },
              other.flatMap((item: Case) => addItems(item :: partialResult))
            )

          addItems(Nil)
      }
    )

  def nonEmptySeveralImplementation[Collection](
      builderFactory: => Builder[Case, Collection]
  ): JavaTrials[Collection] =
    TrialsImplementation(
      scalaTrials() match {
        case impl: com.sageserpent.americium.TrialsImplementation[Case] =>
          impl.nonEmptySeveralImplementation(
            new com.sageserpent.americium.Builder[Case, Collection] {
              private val b                         = builderFactory
              override def add(element: Case): Unit = b.add(element)
              override def build(): Collection      = b.build()
            }
          )
        case other =>
          other.flatMap((item: Case) => {
            val b = builderFactory
            b.add(item)
            severalImplementation(b).scalaTrials
          })
      }
    )
}
