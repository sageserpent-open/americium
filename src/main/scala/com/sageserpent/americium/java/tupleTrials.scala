package com.sageserpent.americium.java

import cats.implicits.*
import cats.{Functor, Semigroupal}
import com.sageserpent.americium.java.{Trials as JavaTrials, TrialsScaffolding as JavaTrialsScaffolding}
import cyclops.data.tuple.{Tuple2 as JavaTuple2, Tuple3 as JavaTuple3, Tuple4 as JavaTuple4}
import cyclops.function.{Consumer3, Consumer4}

import _root_.java.util.Iterator as JavaIterator
import java.util.function.{BiConsumer, Consumer, Function as JavaFunction}

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

  class Tuple2Trials[Case1, Case2](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2]
  ) extends JavaTrialsScaffolding.Tuple2Trials[Case1, Case2] {
    private def trialsOfPairs: JavaTrials[JavaTuple2[Case1, Case2]] =
      (firstTrials, secondTrials).mapN(JavaTuple2.of[Case1, Case2])

    override def trials(): JavaTrials[JavaTuple2[Case1, Case2]] = trialsOfPairs

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
      self =>
      def supplyTo(biConsumer: BiConsumer[Case1, Case2]): Unit =
        supplyTo((pair: JavaTuple2[Case1, Case2]) =>
          biConsumer.accept(pair._1, pair._2)
        )

      override def withSeed(
          seed: Long
      ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
        override protected val supplyToSyntax
            : TrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          self.supplyToSyntax.withSeed(seed)
      }

      override def withComplexityLimit(
          complexityLimit: Int
      ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
        override protected val supplyToSyntax
            : TrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          self.supplyToSyntax.withComplexityLimit(complexityLimit)
      }

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
        override protected val supplyToSyntax
            : TrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          self.supplyToSyntax.withShrinkageAttemptsLimit(shrinkageAttemptsLimit)
      }

      override def withShrinkageStop(
          shrinkageStop: TrialsScaffolding.ShrinkageStop[
            _ >: JavaTuple2[Case1, Case2]
          ]
      ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
        override protected val supplyToSyntax
            : TrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          self.supplyToSyntax.withShrinkageStop(shrinkageStop)
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple2[Case1, Case2]]
      ): Unit = supplyToSyntax.supplyTo(consumer)

      override def asIterator: JavaIterator[JavaTuple2[Case1, Case2]] =
        supplyToSyntax.asIterator

      override def testIntegrationContexts
          : JavaIterator[TestIntegrationContext[JavaTuple2[Case1, Case2]]] =
        supplyToSyntax.testIntegrationContexts

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

    override def withStrategy(
        casesLimitStrategyFactory: JavaFunction[
          CaseSupplyCycle,
          CasesLimitStrategy
        ]
    ): SupplyToSyntaxTuple2 = new SupplyToSyntaxTuple2 {
      val supplyToSyntax
          : JavaTrialsScaffolding.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
        trialsOfPairs.withStrategy(casesLimitStrategyFactory)
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

    override def trials(): JavaTrials[JavaTuple3[Case1, Case2, Case3]] =
      trialsOfTriples

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
      self =>
      def supplyTo(triConsumer: Consumer3[Case1, Case2, Case3]): Unit =
        supplyTo((triple: JavaTuple3[Case1, Case2, Case3]) =>
          triConsumer.accept(triple._1, triple._2, triple._3)
        )

      override def withSeed(
          seed: Long
      ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple3[Case1, Case2, Case3]
        ] = self.supplyToSyntax.withSeed(seed)
      }

      override def withComplexityLimit(
          complexityLimit: Int
      ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple3[Case1, Case2, Case3]
        ] = self.supplyToSyntax.withComplexityLimit(complexityLimit)
      }

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple3[Case1, Case2, Case3]
        ] =
          self.supplyToSyntax.withShrinkageAttemptsLimit(shrinkageAttemptsLimit)
      }

      override def withShrinkageStop(
          shrinkageStop: TrialsScaffolding.ShrinkageStop[
            _ >: JavaTuple3[Case1, Case2, Case3]
          ]
      ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple3[Case1, Case2, Case3]
        ] = self.supplyToSyntax.withShrinkageStop(shrinkageStop)
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple3[Case1, Case2, Case3]]
      ): Unit = supplyToSyntax.supplyTo(consumer)

      override def asIterator: JavaIterator[JavaTuple3[Case1, Case2, Case3]] =
        supplyToSyntax.asIterator

      override def testIntegrationContexts: JavaIterator[
        TestIntegrationContext[JavaTuple3[Case1, Case2, Case3]]
      ] = supplyToSyntax.testIntegrationContexts

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

    override def withStrategy(
        casesLimitStrategyFactory: JavaFunction[
          CaseSupplyCycle,
          CasesLimitStrategy
        ]
    ): SupplyToSyntaxTuple3 = new SupplyToSyntaxTuple3 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple3[Case1, Case2, Case3]
      ] =
        trialsOfTriples.withStrategy(casesLimitStrategyFactory)
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

    override def trials(): JavaTrials[JavaTuple4[Case1, Case2, Case3, Case4]] =
      trialsOfQuadruples

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
      self =>
      def supplyTo(
          quadConsumer: Consumer4[Case1, Case2, Case3, Case4]
      ): Unit =
        supplyTo((quadruple: JavaTuple4[Case1, Case2, Case3, Case4]) =>
          quadConsumer.accept(
            quadruple._1,
            quadruple._2,
            quadruple._3,
            quadruple._4
          )
        )

      override def withSeed(
          seed: Long
      ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] = self.supplyToSyntax.withSeed(seed)
      }

      override def withComplexityLimit(
          complexityLimit: Int
      ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] = self.supplyToSyntax.withComplexityLimit(complexityLimit)
      }

      override def withShrinkageAttemptsLimit(
          shrinkageAttemptsLimit: Int
      ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] =
          self.supplyToSyntax.withShrinkageAttemptsLimit(shrinkageAttemptsLimit)
      }

      override def withShrinkageStop(
          shrinkageStop: TrialsScaffolding.ShrinkageStop[
            _ >: JavaTuple4[Case1, Case2, Case3, Case4]
          ]
      ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
        override protected val supplyToSyntax: TrialsScaffolding.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] = self.supplyToSyntax.withShrinkageStop(shrinkageStop)
      }

      override def supplyTo(
          consumer: Consumer[JavaTuple4[Case1, Case2, Case3, Case4]]
      ): Unit = supplyToSyntax.supplyTo(consumer)

      override def asIterator
          : JavaIterator[JavaTuple4[Case1, Case2, Case3, Case4]] =
        supplyToSyntax.asIterator

      override def testIntegrationContexts: JavaIterator[
        TestIntegrationContext[JavaTuple4[Case1, Case2, Case3, Case4]]
      ] = supplyToSyntax.testIntegrationContexts

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

    override def withStrategy(
        casesLimitStrategyFactory: JavaFunction[
          CaseSupplyCycle,
          CasesLimitStrategy
        ]
    ): SupplyToSyntaxTuple4 = new SupplyToSyntaxTuple4 {
      val supplyToSyntax: JavaTrialsScaffolding.SupplyToSyntax[
        JavaTuple4[Case1, Case2, Case3, Case4]
      ] =
        trialsOfQuadruples.withStrategy(casesLimitStrategyFactory)
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
