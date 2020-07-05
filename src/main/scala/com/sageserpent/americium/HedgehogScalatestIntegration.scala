package com.sageserpent.americium

import hedgehog.core._
import hedgehog.runner.Test
import hedgehog.{Gen, Property, Result, forTupled}
import org.scalatest.Assertion

trait HedgehogScalatestIntegration {
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

  def check[T](generator: Gen[T])(test: T => Assertion)(
      implicit configuration: PropertyConfig): Unit =
    check(generator.forAll.map(test))

  def check[T1, T2](generator1: Gen[T1], generator2: Gen[T2])(
      test: (T1, T2) => Assertion)(
      implicit configuration: PropertyConfig): Unit =
    check(forTupled(generator1, generator2).forAll.map(test.tupled))

  def check[T1, T2, T3](generator1: Gen[T1],
                        generator2: Gen[T2],
                        generator3: Gen[T3])(test: (T1, T2, T3) => Assertion)(
      implicit configuration: PropertyConfig): Unit =
    check(forTupled(generator1, generator2, generator3).forAll.map(test.tupled))

  def check[T1, T2, T3, T4](
      generator1: Gen[T1],
      generator2: Gen[T2],
      generator3: Gen[T3],
      generator4: Gen[T4])(test: (T1, T2, T3, T4) => Assertion)(
      implicit configuration: PropertyConfig): Unit =
    check(
      forTupled(generator1, generator2, generator3, generator4).forAll
        .map(test.tupled))
}
