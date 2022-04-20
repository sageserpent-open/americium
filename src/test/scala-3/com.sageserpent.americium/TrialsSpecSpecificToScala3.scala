package com.sageserpent.americium

import com.sageserpent.americium.TrialsSpec.{BinaryTree, limit}
import org.scalatest.flatspec.AnyFlatSpec

class TrialsSpecSpecificToScala3 extends AnyFlatSpec {
  "test driving automatic implicit generation of a trials for a recursive data structure" should "not produce smoke" in {
      {
        given evidence: Factory[List[Boolean]] = Factory.autoDerived

        implicitly[Factory[List[Boolean]]].trials
          .withLimit(limit)
          .supplyTo(println)
      }

      {
        given evidence: Factory[BinaryTree] = Factory.autoDerived

        implicitly[Factory[BinaryTree]].trials
          .withLimit(limit)
          .supplyTo(println)
      }
  }
}
