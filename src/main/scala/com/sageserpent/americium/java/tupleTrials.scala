package com.sageserpent.americium.java

import com.sageserpent.americium.java.{Trials => JavaTrials}
import cyclops.data.tuple.{
  Tuple => JavaTuple,
  Tuple2 => JavaTuple2,
  Tuple3 => JavaTuple3,
  Tuple4 => JavaTuple4
}

object tupleTrials {
  // TODO: something clever with Magnolia, or `HList` or just raw macros that
  // saves the bother of churning out lots of copied boilerplate...
  class Tuple2Trials[Case1, Case2](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2]
  ) extends JavaTrials.Tuple2Trials[Case1, Case2] {
    val trialsOfPairs: JavaTrials[JavaTuple2[Case1, Case2]] = {
      // NASTY HACK: have to use an explicit flatmap here, rather than an
      // applicative construct, to ensure the result is a
      // `TrialsImplementation`.
      for {
        first  <- firstTrials
        second <- secondTrials
      } yield JavaTuple.tuple(first, second)
    }

    override def and[Case3](
        thirdTrials: JavaTrials[Case3]
    ): JavaTrials.Tuple3Trials[Case1, Case2, Case3] = {
      new Tuple3Trials(firstTrials, secondTrials, thirdTrials)
    }

    override def withLimit(
        limit: Int
    ): JavaTrials.SupplyToSyntaxTuple2[Case1, Case2] =
      new JavaTrials.SupplyToSyntaxTuple2[Case1, Case2] {
        val supplyToSyntax
            : JavaTrials.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          trialsOfPairs.withLimit(limit)
      }
    override def withRecipe(
        recipe: String
    ): JavaTrials.SupplyToSyntaxTuple2[Case1, Case2] =
      new JavaTrials.SupplyToSyntaxTuple2[Case1, Case2] {
        val supplyToSyntax
            : JavaTrials.SupplyToSyntax[JavaTuple2[Case1, Case2]] =
          trialsOfPairs.withRecipe(recipe)
      }
  }

  class Tuple3Trials[Case1, Case2, Case3](
      firstTrials: JavaTrials[Case1],
      secondTrials: JavaTrials[Case2],
      thirdTrials: JavaTrials[Case3]
  ) extends JavaTrials.Tuple3Trials[Case1, Case2, Case3] {
    val trialsOfTriples: JavaTrials[JavaTuple3[Case1, Case2, Case3]] = {
      // NASTY HACK: have to use an explicit flatmap here, rather than an
      // applicative construct, to ensure the result is a
      // `TrialsImplementation`.
      for {
        first  <- firstTrials
        second <- secondTrials
        third  <- thirdTrials
      } yield JavaTuple.tuple(first, second, third)
    }

    override def and[Case4](
        fourthTrials: JavaTrials[Case4]
    ): JavaTrials.Tuple4Trials[Case1, Case2, Case3, Case4] = {
      new Tuple4Trials(firstTrials, secondTrials, thirdTrials, fourthTrials)
    }

    override def withLimit(
        limit: Int
    ): JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      new JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] {
        val supplyToSyntax
            : JavaTrials.SupplyToSyntax[JavaTuple3[Case1, Case2, Case3]] =
          trialsOfTriples.withLimit(limit)
      }

    override def withRecipe(
        recipe: String
    ): JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      new JavaTrials.SupplyToSyntaxTuple3[Case1, Case2, Case3] {
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
    val trialsOfQuadruples: JavaTrials[
      JavaTuple4[Case1, Case2, Case3, Case4]
    ] = {
      // NASTY HACK: have to use an explicit flatmap here, rather than an
      // applicative construct, to ensure the result is a
      // `TrialsImplementation`.
      for {
        first  <- firstTrials
        second <- secondTrials
        third  <- thirdTrials
        fourth <- fourthTrials
      } yield JavaTuple.tuple(first, second, third, fourth)
    }

    override def withLimit(
        limit: Int
    ): JavaTrials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] =
      new JavaTrials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] {
        val supplyToSyntax: JavaTrials.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] =
          trialsOfQuadruples.withLimit(limit)
      }
    override def withRecipe(
        recipe: String
    ): JavaTrials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] =
      new JavaTrials.SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4] {
        val supplyToSyntax: JavaTrials.SupplyToSyntax[
          JavaTuple4[Case1, Case2, Case3, Case4]
        ] =
          trialsOfQuadruples.withRecipe(recipe)
      }
  }
}
