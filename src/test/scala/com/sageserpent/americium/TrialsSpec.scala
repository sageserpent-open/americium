package com.sageserpent.americium

import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class TrialsSpec
    extends FlatSpec
    with Matchers
    with MockFactory
    with TableDrivenPropertyChecks {
  /*
  1.   A constant case yields just one trial.
  2.   A constant case that provokes an exception will result in an exception referencing that case.
  3.   A choice yields all and only the cases given to the choice.
  4.   A choice that includes cases that provoke exceptions will result in an exception that references one of those cases.
  5.   An alternation yields all and only the cases that would be covered by the alternates in isolation.
  6.   An alternation that over trials that in isolation include cases that provoke exceptions will result in an exception that references one of those cases.
  7.   In general, trials yield the same cases.
  8.   In general, trials result in the same exception and referenced case (and recipe).
  9.   In general, a trial that results in an exception provides a recipe that will recreate the same case.
  10.  'Trials' is a functor.
  11.  'Trials' is a monad.
  12.  When a case provokes an exception, the exception yielded refers to a case that is either the same as the original or is simpler.
   */

  "only one case" should "yield just one trial" in
    forAll(Table("case", 1, "foo", 2.3, List(false, 0, true))) { dataCase =>
      val sut = Trials.only(dataCase)

      val mockConsumer = stubFunction[Any, Unit]

      sut.supplyTo(mockConsumer)

      mockConsumer.verify(dataCase)
    }

  "only one case that provokes an exception" should "result in an exception that references it" in
    forAll(Table("case", 1, "foo", 2.3, Seq(false, 0, true))) { dataCase =>
      val sut = Trials.only(dataCase)

      val problem = new RuntimeException("Test problem")

      val mockConsumer = stubFunction[Any, Unit]

      mockConsumer.when(dataCase).throwing(problem)

      val exception = intercept[sut.TrialException] {
        sut.supplyTo(mockConsumer)
      }

      exception.getCause should be(problem)
      exception.provokingCase should be(dataCase)
    }

  "a choice" should "yield all and only the cases given to it" in
    forAll(
      Table("possibleChoices",
            Seq.empty,
            1 to 10,
            -5 to 5 map (_.toString),
            Seq(true),
            Seq(4.3))) { possibleChoices =>
      val sut: Trials[Any] = Trials.choose(possibleChoices)

      val mockConsumer = stubFunction[Any, Unit]

      sut.supplyTo(mockConsumer)

      possibleChoices.foreach(possibleChoice =>
        mockConsumer.verify(possibleChoice))
    }

}
