package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConventionalScalaPerson(val name: String, val age: Int)

class ConventionalScalaTree(
    val value: Int,
    val left: ConventionalScalaTree,
    val right: ConventionalScalaTree
) {
  def this(value: Int) = this(value, null, null)
}

class InstancesScalaTest extends AnyFlatSpec with Matchers {
  "Trials.api.instances" should "derive trials for conventional OO Scala classes" in {
    val personTrials: Trials[ConventionalScalaPerson] =
      Trials.api.instances(classOf[ConventionalScalaPerson])

    personTrials.withLimit(20).supplyTo { person =>
      person.name should not be null
    }
  }

  it should "derive trials for recursive conventional OO Scala classes" in {
    val treeTrials: Trials[ConventionalScalaTree] =
      Trials.api.instances(classOf[ConventionalScalaTree])

    treeTrials.withLimit(30).supplyTo { tree =>
      tree should not be null
    }
  }
}
