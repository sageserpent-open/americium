package com.sageserpent.americium

import com.sageserpent.americium.TrialsSpec.{BinaryTree, limit}
import org.scalatest.flatspec.AnyFlatSpec

class TrialsSpecSpecificToScala2_13 extends AnyFlatSpec {
  "test driving automatic implicit generation of a trials for a recursive data structure" should "not produce smoke" in {
    implicitly[Factory[List[Boolean]]].trials
      .withLimit(limit)
      .supplyTo(println)

    implicitly[Factory[BinaryTree]].trials
      .withLimit(limit)
      .supplyTo(println)
  }
}
