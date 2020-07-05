package com.sageserpent.americium

import hedgehog.core._
import hedgehog.runner.Test
import hedgehog.{Property, Result}
import org.scalatest.Assertion

trait HedgehogIntegration {
  def check(property: PropertyT[Assertion])(
      implicit configuration: PropertyConfig): Unit = {
    import org.scalatest.Assertions.fail

    val report = Property.check(
      configuration,
      property.map(_ => Result.success),
      Seed.fromLong(1L)
    )

    report.status match {
      case OK =>
      case GaveUp =>
        fail(
          s"Gave up after only ${report.tests.value} passed test. ${report.discards.value} were discarded")
      case Failed(shrinks, log) =>
        fail(
          (s"Falsified after ${report.tests.value} passed tests, did ${shrinks.value} shrinks" :: log
            .map(Test.renderLog)).mkString("\n"))
    }
  }
}
