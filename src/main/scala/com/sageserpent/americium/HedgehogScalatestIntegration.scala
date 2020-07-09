package com.sageserpent.americium

import hedgehog.core._
import hedgehog.runner.Test
import hedgehog.{Gen, Property, Result, forTupled}
import org.scalatest.exceptions.DiscardedEvaluationException

trait HedgehogScalatestIntegration {
  def check(property: PropertyT[Result])(
      implicit configuration: PropertyConfig): Unit = {
    import org.scalatest.Assertions.fail

    val report = Property.check(
      configuration,
      property,
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

  def whenever[T](predicate: Boolean)(block: => T): T =
    if (predicate) block else throw new DiscardedEvaluationException

  private def lift[T](test: T => Unit)(data: T): PropertyT[Result] =
    try {
      test(data)
      Property.point(Result.success)
    } catch {
      case _: DiscardedEvaluationException => Property.fromGen(Gen.discard)
    }

  def check[T](generator: Gen[T])(test: T => Unit)(
      implicit configuration: PropertyConfig): Unit =
    check(generator.forAll.flatMap(lift(test)))

  def check[T1, T2](generator1: Gen[T1], generator2: Gen[T2])(
      test: (T1, T2) => Unit)(implicit configuration: PropertyConfig): Unit =
    check(forTupled(generator1, generator2).forAll.flatMap(lift(test.tupled)))

  def check[T1, T2, T3](generator1: Gen[T1],
                        generator2: Gen[T2],
                        generator3: Gen[T3])(test: (T1, T2, T3) => Unit)(
      implicit configuration: PropertyConfig): Unit =
    check(
      forTupled(generator1, generator2, generator3).forAll
        .flatMap(lift(test.tupled)))

  def check[T1, T2, T3, T4](
      generator1: Gen[T1],
      generator2: Gen[T2],
      generator3: Gen[T3],
      generator4: Gen[T4])(test: (T1, T2, T3, T4) => Unit)(
      implicit configuration: PropertyConfig): Unit =
    check(
      forTupled(generator1, generator2, generator3, generator4).forAll
        .flatMap(lift(test.tupled)))
}
