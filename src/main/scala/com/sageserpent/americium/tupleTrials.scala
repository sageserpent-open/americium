package com.sageserpent.americium

import cats.implicits._

object tupleTrials {
  class Tuple2Trials[+Case1, +Case2](
      firstTrials: Trials[Case1],
      secondTrials: Trials[Case2]
  ) extends Trials.Tuple2Trials[Case1, Case2] {
    def and[Case3](
        thirdTrials: Trials[Case3]
    ): Tuple3Trials[Case1, Case2, Case3] =
      new Tuple3Trials(firstTrials, secondTrials, thirdTrials)

    trait SupplyToSyntaxTuple2[+Case1, +Case2]
        extends Trials.SupplyToSyntaxTuple2[Case1, Case2] {
      def supplyTo(consumer: (Case1, Case2) => Unit): Unit = supplyTo(
        consumer.tupled
      )
    }

    def trialsOfPairs: Trials[(Case1, Case2)] =
      (firstTrials, secondTrials).mapN(Tuple2.apply)

    def withLimit(limit: Int): SupplyToSyntaxTuple2[Case1, Case2] =
      (consumer: ((Case1, Case2)) => Unit) =>
        trialsOfPairs.withLimit(limit).supplyTo(consumer)

    def withRecipe(recipe: String): SupplyToSyntaxTuple2[Case1, Case2] =
      (consumer: ((Case1, Case2)) => Unit) =>
        trialsOfPairs.withRecipe(recipe).supplyTo(consumer)
  }

  class Tuple3Trials[+Case1, +Case2, +Case3](
      firstTrials: Trials[Case1],
      secondTrials: Trials[Case2],
      thirdTrials: Trials[Case3]
  ) extends Trials.Tuple3Trials[Case1, Case2, Case3] {
    def and[Case4](
        fourthTrials: Trials[Case4]
    ): Tuple4Trials[Case1, Case2, Case3, Case4] =
      new Tuple4Trials(firstTrials, secondTrials, thirdTrials, fourthTrials)

    trait SupplyToSyntaxTuple3[+Case1, +Case2, +Case3]
        extends Trials.SupplyToSyntaxTuple3[Case1, Case2, Case3] {
      def supplyTo(consumer: (Case1, Case2, Case3) => Unit): Unit = supplyTo(
        consumer.tupled
      )
    }

    def trialsOfTriples: Trials[(Case1, Case2, Case3)] =
      (firstTrials, secondTrials, thirdTrials).mapN(Tuple3.apply)

    def withLimit(limit: Int): SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      (consumer: ((Case1, Case2, Case3)) => Unit) =>
        trialsOfTriples.withLimit(limit).supplyTo(consumer)

    def withRecipe(recipe: String): SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      (consumer: ((Case1, Case2, Case3)) => Unit) =>
        trialsOfTriples.withRecipe(recipe).supplyTo(consumer)
  }

  class Tuple4Trials[+Case1, +Case2, +Case3, +Case4](
      firstTrials: Trials[Case1],
      secondTrials: Trials[Case2],
      thirdTrials: Trials[Case3],
      fourthTrials: Trials[Case4]
  ) extends Trials.Tuple4Trials[Case1, Case2, Case3, Case4] {
    trait SupplyToSyntaxTuple4[+Case1, +Case2, +Case3, +Case4]
        extends Trials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] {
      def supplyTo(consumer: (Case1, Case2, Case3, Case4) => Unit): Unit =
        supplyTo(
          consumer.tupled
        )
    }

    val trialsOfQuadruples: Trials[(Case1, Case2, Case3, Case4)] =
      (firstTrials, secondTrials, thirdTrials, fourthTrials).mapN(Tuple4.apply)

    def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] =
      (consumer: ((Case1, Case2, Case3, Case4)) => Unit) =>
        trialsOfQuadruples.withLimit(limit).supplyTo(consumer)

    def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] =
      (consumer: ((Case1, Case2, Case3, Case4)) => Unit) =>
        trialsOfQuadruples.withRecipe(recipe).supplyTo(consumer)
  }
}
