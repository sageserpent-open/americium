package com.sageserpent.americium.java

import cats.implicits._
import cats.{Functor, Semigroupal}
import com.sageserpent.americium.java.{Trials => JavaTrials}
import cyclops.data.tuple.{
  Tuple2 => JavaTuple2,
  Tuple3 => JavaTuple3,
  Tuple4 => JavaTuple4
}
import cyclops.function.{Consumer3, Consumer4}

import _root_.java.util.{Iterator => JavaIterator}
import java.util.function.{BiConsumer, Consumer}

object tupleTrials {
  implicit val functorInstance: Functor[JavaTrials] = new Functor[JavaTrials] {
    override def map[A, B](fa: JavaTrials[A])(
        f: A => B
    ): JavaTrials[B] = fa.map(f.apply _)
  }

  implicit val semigroupalInstance: Semigroupal[JavaTrials] =
    new Semigroupal[JavaTrials] {
      override def product[A, B](
          fa: JavaTrials[A],
          fb: JavaTrials[B]
      ): JavaTrials[(A, B)] = for {
        a <- fa
        b <- fb
      } yield a -> b
    }

  // TODO: something clever with Magnolia, `HList`, something else in Shapeless
  // or just raw macros that saves the bother of churning out lots of copied
  // boilerplate...
  class Tuple2Trials[Case1, Case2](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2]
  ) extends JavaTrials.Tuple2Trials[Case1, Case2] {
    override def and[Case3](
        thirdTrials: JavaTrials[Case3]
    ): JavaTrials.Tuple3Trials[Case1, Case2, Case3] =
      new Tuple3Trials(firstTrials, secondTrials, thirdTrials)

    trait SupplyToSyntaxTuple2
        extends JavaTrials.SupplyToSyntaxTuple2[Case1, Case2] {
      def supplyTo(biConsumer: BiConsumer[Case1, Case2]): Unit = {
        supplyTo((pair: JavaTuple2[Case1, Case2]) =>
          biConsumer.accept(pair._1, pair._2)
        )
      }

      def trialsOfPairs: JavaTrials[JavaTuple2[Case1, Case2]] =
        (firstTrials, secondTrials).mapN(JavaTuple2.of[Case1, Case2])

      override def supplyTo(
          consumer: Consumer[JavaTuple2[Case1, Case2]]
      ): Unit = {
        supplyToSyntax.supplyTo(consumer)
      }

      override def asIterator: JavaIterator[JavaTuple2[Case1, Case2]] =
        supplyToSyntax.asIterator

      protected val supplyToSyntax: Trials.SupplyToSyntax[
        JavaTuple2[Case1, Case2]
      ]
    }

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax: JavaTrials.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withLimit(limit)
    }

    override def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax: JavaTrials.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withRecipe(recipe)
    }
  }

  class Tuple3Trials[Case1, Case2, Case3](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2],
      thirdTrials: JavaTrials[Case3]
  ) extends JavaTrials.Tuple3Trials[Case1, Case2, Case3] {
    override def and[Case4](
        fourthTrials: JavaTrials[Case4]
    ): JavaTrials.Tuple4Trials[Case1, Case2, Case3, Case4] =
      new Tuple4Trials(firstTrials, secondTrials, thirdTrials, fourthTrials)

    trait SupplyToSyntaxTuple3
        extends JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] {
      def supplyTo(triConsumer: Consumer3[Case1, Case2, Case3]): Unit = {
        supplyTo((triple: JavaTuple3[Case1, Case2, Case3]) =>
          triConsumer.accept(triple._1, triple._2, triple._3)
        )
      }

      def trialsOfTriples: JavaTrials[JavaTuple3[Case1, Case2, Case3]] =
        (firstTrials, secondTrials, thirdTrials).mapN(
          JavaTuple3.of[Case1, Case2, Case3]
        )

      override def supplyTo(
          consumer: Consumer[JavaTuple3[Case1, Case2, Case3]]
      ): Unit = { supplyToSyntax.supplyTo(consumer) }

      override def asIterator: JavaIterator[JavaTuple3[Case1, Case2, Case3]] =
        supplyToSyntax.asIterator

      protected val supplyToSyntax: Trials.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ]
    }

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax
          : JavaTrials.SupplyToSyntax[JavaTuple3[Case1, Case2, Case3]] =
        trialsOfTriples.withLimit(limit)
    }

    override def withRecipe(
        recipe: String
    ): JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      new SupplyToSyntaxTuple3 {
        val supplyToSyntax
            : JavaTrials.SupplyToSyntax[JavaTuple3[Case1, Case2, Case3]] =
          trialsOfTriples.withRecipe(recipe)
      }
  }

  class Tuple4Trials[Case1, Case2, Case3, Case4](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2],
      thirdTrials: JavaTrials[Case3],
      fourthTrials: JavaTrials[Case4]
  ) extends JavaTrials.Tuple4Trials[Case1, Case2, Case3, Case4] {
    trait SupplyToSyntaxTuple4
        extends JavaTrials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] {
      def supplyTo(
          quadConsumer: Consumer4[Case1, Case2, Case3, Case4]
      ): Unit = {
        supplyTo((quadruple: JavaTuple4[Case1, Case2, Case3, Case4]) =>
          quadConsumer.accept(
            quadruple._1,
            quadruple._2,
            quadruple._3,
            quadruple._4
          )
        )
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple4[Case1, Case2, Case3, Case4]]
      ): Unit = { supplyToSyntax.supplyTo(consumer) }

      override def asIterator
          : JavaIterator[JavaTuple4[Case1, Case2, Case3, Case4]] =
        supplyToSyntax.asIterator

      protected val supplyToSyntax: Trials.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ]
    }

    def trialsOfQuadruples: JavaTrials[
      JavaTuple4[Case1, Case2, Case3, Case4]
    ] = (firstTrials, secondTrials, thirdTrials, fourthTrials).mapN(
      JavaTuple4.of[Case1, Case2, Case3, Case4]
    )

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrials.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withLimit(limit)
    }
    override def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrials.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withRecipe(recipe)
    }
  }
}
