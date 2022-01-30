package com.sageserpent.americium

import com.sageserpent.americium.TrialsSpec.{BinaryTree, limit}
import org.scalatest.flatspec.AnyFlatSpec

class TrialsSpecSpecificToScala2_13 extends AnyFlatSpec {
  // Currently for Scala 3.1.1, derivation of a recursively-defined type a)
  // requires workaround code by assigning the derived `Factory` instance due to
  // a known issue with Magnolia for Scala 3 and b) causes the Scala compiler to
  // fault with an assertion failure. Hence this test is only run for Scala
  // 2.13.
  // TODO: reinstate across both the Scala 2.13 and 3 build...
  "test driving automatic implicit generation of a trials for a recursive data structure" should "not produce smoke" in {
    implicitly[Factory[List[Boolean]]].trials
      .withLimit(limit)
      .supplyTo(println)

    implicitly[Factory[BinaryTree]].trials
      .withLimit(limit)
      .supplyTo(println)
  }
}
