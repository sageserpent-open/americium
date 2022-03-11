package com.sageserpent.americium.java

import cats.implicits.*
import cats.{Functor, Semigroupal}
import com.sageserpent.americium.java.{
  Trials as JavaTrials,
  TrialsScaffolding as JavaTrialsScaffolding
}
import cyclops.data.tuple.{
  Tuple2 as JavaTuple2,
  Tuple3 as JavaTuple3,
  Tuple4 as JavaTuple4
}
import cyclops.function.{Consumer3, Consumer4}

import _root_.java.util.Iterator as JavaIterator
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
  // TODO: something equally as clever where these implementations use ByteBuddy
  // to delegate to the trials of tuple instances, and when that is done, we can
  // hoist *most* of the orphaned methods in `Trials` up to `TrialsScaffolding`
  // - well, all but the `and` combinator. I'm not going near that last one.
  class Tuple2Trials[Case1, Case2](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2]
  ) extends JavaTrialsScaffolding.Tuple2Trials[Case1, Case2] {
    private def trialsOfPairs: JavaTrials[JavaTuple2[Case1, Case2]] =
      (firstTrials, secondTrials).mapN(JavaTuple2.of[Case1, Case2])

    override def reproduce(
        recipe: String
    ): JavaTuple2[Case1, Case2] = trialsOfPairs.reproduce(recipe)

    override def and[Case3](
        thirdTrials: JavaTrials[Case3]
    ): JavaTrialsScaffolding.Tuple3Trials[Case1, Case2, Case3] =
      new Tuple3Trials(firstTrials, secondTrials, thirdTrials)

    trait SupplyToSyntaxTuple2
        extends JavaTrialsScaffolding.Tuple2Trials.SupplyToSyntaxTuple2[
          Case1,
          Case2
        ] {
      def supplyTo(biConsumer: BiConsumer[Case1, Case2]): Unit = {
        supplyTo((pair: JavaTuple2[Case1, Case2]) =>
          biConsumer.accept(pair._1, pair._2)
        )
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple2[Case1, Case2]]
      ): Unit = {
        supplyToSyntax.supplyTo(consumer)
      }

      override def asIterator: JavaIterator[JavaTuple2[Case1, Case2]] =
        supplyToSyntax.asIterator

      override def testIntegration: JavaTuple2[JavaIterator[
        JavaTuple2[Case1, Case2]
      ], InlinedCaseFiltration] = supplyToSyntax.testIntegration

      protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
        JavaTuple2[Case1, Case2]
      ]
    }

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withLimit(limit)
    }

    override def withLimit(
        limit: Int,
        complexityLimit: Int
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withLimit(limit, complexityLimit)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withLimits(casesLimit, optionalLimits)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits,
        shrinkageStop: TrialsScaffolding.ShrinkageStop[
          _ >: JavaTuple2[Case1, Case2]
        ]
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withLimits(casesLimit, optionalLimits, shrinkageStop)
    }

    override def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withRecipe(recipe)
    }
  }

  class Tuple3Trials[Case1, Case2, Case3](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2],
      thirdTrials: JavaTrials[Case3]
  ) extends JavaTrialsScaffolding.Tuple3Trials[Case1, Case2, Case3] {
    private def trialsOfTriples: JavaTrials[JavaTuple3[Case1, Case2, Case3]] =
      (firstTrials, secondTrials, thirdTrials).mapN(
        JavaTuple3.of[Case1, Case2, Case3]
      )

    override def reproduce(
        recipe: String
    ): JavaTuple3[Case1, Case2, Case3] = trialsOfTriples.reproduce(recipe)

    override def and[Case4](
        fourthTrials: JavaTrials[Case4]
    ): JavaTrialsScaffolding.Tuple4Trials[Case1, Case2, Case3, Case4] =
      new Tuple4Trials(firstTrials, secondTrials, thirdTrials, fourthTrials)

    trait SupplyToSyntaxTuple3
        extends JavaTrialsScaffolding.Tuple3Trials.SupplyToSyntaxTuple3[
          Case1,
          Case2,
          Case3
        ] {
      def supplyTo(triConsumer: Consumer3[Case1, Case2, Case3]): Unit = {
        supplyTo((triple: JavaTuple3[Case1, Case2, Case3]) =>
          triConsumer.accept(triple._1, triple._2, triple._3)
        )
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple3[Case1, Case2, Case3]]
      ): Unit = { supplyToSyntax.supplyTo(consumer) }

      override def asIterator: JavaIterator[JavaTuple3[Case1, Case2, Case3]] =
        supplyToSyntax.asIterator

      override def testIntegration: JavaTuple2[JavaIterator[
        JavaTuple3[Case1, Case2, Case3]
      ], InlinedCaseFiltration] = supplyToSyntax.testIntegration

      protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ]
    }

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ] =
        trialsOfTriples.withLimit(limit)
    }

    override def withLimit(
        limit: Int,
        complexityLimit: Int
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ] =
        trialsOfTriples.withLimit(limit, complexityLimit)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ] =
        trialsOfTriples.withLimits(casesLimit, optionalLimits)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits,
        shrinkageStop: TrialsScaffolding.ShrinkageStop[
          _ >: JavaTuple3[Case1, Case2, Case3]
        ]
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ] =
        trialsOfTriples.withLimits(casesLimit, optionalLimits, shrinkageStop)
    }

    override def withRecipe(
        recipe: String
    ): JavaTrialsScaffolding.Tuple3Trials.SupplyToSyntaxTuple3[
      Case1,
      Case2,
      Case3
    ] =
      new SupplyToSyntaxTuple3 {
        val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
          JavaTuple3[Case1, Case2, Case3]
        ] =
          trialsOfTriples.withRecipe(recipe)
      }
  }

  class Tuple4Trials[Case1, Case2, Case3, Case4](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2],
      thirdTrials: JavaTrials[Case3],
      fourthTrials: JavaTrials[Case4]
  ) extends JavaTrialsScaffolding.Tuple4Trials[Case1, Case2, Case3, Case4] {
    private def trialsOfQuadruples: JavaTrials[
      JavaTuple4[Case1, Case2, Case3, Case4]
    ] = (firstTrials, secondTrials, thirdTrials, fourthTrials).mapN(
      JavaTuple4.of[Case1, Case2, Case3, Case4]
    )

    override def reproduce(
        recipe: String
    ): JavaTuple4[Case1, Case2, Case3, Case4] =
      trialsOfQuadruples.reproduce(recipe)

    trait SupplyToSyntaxTuple4
        extends JavaTrialsScaffolding.Tuple4Trials.SupplyToSyntaxTuple4[
          Case1,
          Case2,
          Case3,
          Case4
        ] {
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

      override def testIntegration: JavaTuple2[JavaIterator[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ], InlinedCaseFiltration] = supplyToSyntax.testIntegration

      protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ]
    }

    override def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withLimit(limit)
    }

    override def withLimit(
        limit: Int,
        complexityLimit: Int
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withLimit(limit, complexityLimit)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withLimits(casesLimit, optionalLimits)
    }

    override def withLimits(
        casesLimit: Int,
        optionalLimits: TrialsScaffolding.OptionalLimits,
        shrinkageStop: TrialsScaffolding.ShrinkageStop[
          _ >: JavaTuple4[Case1, Case2, Case3, Case4]
        ]
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withLimits(casesLimit, optionalLimits, shrinkageStop)
    }

    override def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withRecipe(recipe)
    }
  }
}
