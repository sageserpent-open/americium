package com.sageserpent.americium

import org.scalamock.function.StubFunction1
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class TrialsSpec
    extends FlatSpec
    with Matchers
    with MockFactory
    with TableDrivenPropertyChecks {
  autoVerify = false
  /*
  6.   In general, trials yield the same cases.
  8.   In general, trials result in the same exception and referenced case (and recipe).
  9.   In general, a trial that results in an exception provides a recipe that will recreate the same case.
  10.  'Trials' is a functor.
  11.  'Trials' is a monad.
  12.  In general, when a case provokes an exception, the exception yielded refers to a case that is either the same as the original or is simpler.
  13.  In general, an alternation submits cases that must satisfy at least one alternative's case invariant.
  14.  In general, trials that result in an exception yield a case that satisfies the trials' invariant.
   */

  "only one case" should "yield just one trial" in
    forAll(Table("case", 1, "foo", 2.3, List(false, 0, true))) { dataCase =>
      withExpectations {
        val sut = Trials.only(dataCase)

        val mockConsumer: StubFunction1[Any, Unit] = stubFunction[Any, Unit]

        sut.supplyTo(mockConsumer)

        mockConsumer.verify(dataCase)
      }
    }

  "only one case that provokes an exception" should "result in an exception that references it" in
    forAll(Table("case", 1, "foo", 2.3, Seq(false, 0, true))) { dataCase =>
      withExpectations {
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
    }

  "a choice" should "yield all and only the cases given to it" in
    forAll(
      Table("possibleChoices",
            Seq.empty,
            1 to 10,
            -5 to 5 map (_.toString),
            Seq(true),
            Seq(4.3))) { possibleChoices =>
      withExpectations {
        val sut: Trials[Any] = Trials.choose(possibleChoices)

        val mockConsumer = stubFunction[Any, Unit]

        sut.supplyTo(mockConsumer)

        possibleChoices.foreach(possibleChoice =>
          mockConsumer.verify(possibleChoice))
      }
    }

  case class ExceptionWithCasePayload[Case](caze: Case) extends RuntimeException

  "a choice that includes exceptional cases" should "result in one of the corresponding exceptions" in {
    type ChoicesAndCriterion[X] = (Seq[X], X => Boolean)

    def testBodyInWildcardCapture[X](
        choicesAndCriterion: ChoicesAndCriterion[X]) =
      withExpectations {
        choicesAndCriterion match {
          case (possibleChoices, exceptionCriterion) =>
            val sut = Trials.choose(possibleChoices)

            val mockConsumer = { caze: X =>
              if (exceptionCriterion(caze))
                throw ExceptionWithCasePayload(caze)
            }

            val exception = intercept[sut.TrialException] {
              sut.supplyTo(mockConsumer)
            }

            val underlyingException = exception.getCause

            underlyingException shouldBe a[ExceptionWithCasePayload[_]]

            underlyingException match {
              case exceptionWithCasePayload: ExceptionWithCasePayload[_] =>
                exception.provokingCase should be(exceptionWithCasePayload.caze)

                exactly(1, possibleChoices) should be(
                  exceptionWithCasePayload.caze)
            }
        }
      }

    forAll(
      Table[ChoicesAndCriterion[_]](
        "possibleChoices -> exceptionCriterion",
        (1 to 10, 0 == (_: Int) % 2),
        (-5 to 5 map (_.toString), (_: String).contains("5")),
        (Seq(false, true), identity[Boolean] _),
        (Seq(4.3), (_: Double) => true)
      )) { choicesAndCriterion =>
      testBodyInWildcardCapture(choicesAndCriterion)
    }
  }

  "an alternation over finite alternatives" should "yield all and only the cases that would be yielded by its alternatives" in
    forAll(
      Table(
        "alternatives",
        Seq.empty,
        Seq(1 to 10),
        Seq(1 to 10, 20 to 30 map (_.toString)),
        Seq(1 to 10, Seq(true, false), 20 to 30),
        Seq(1, "3", 99),
        Seq(1 to 10, Seq(12), -3 to -1),
        Seq(Seq(0), 1 to 10, 13, -3 to -1)
      )) { alternatives =>
      withExpectations {
        val sut: Trials[Any] =
          Trials.alternate(alternatives map {
            case sequence: Seq[_] => Trials.choose(sequence)
            case singleton        => Trials.only(singleton)
          })

        val mockConsumer = stubFunction[Any, Unit]

        sut.supplyTo(mockConsumer)

        alternatives
          .flatMap {
            case several: Seq[_] => several
            case singleton       => Seq(singleton)
          }
          .foreach(possibleChoice => mockConsumer.verify(possibleChoice))
      }
    }

  "mapping using a Scala function" should "compile" in {
    assertCompiles("Trials.only(1).map((_ + 1): Int => Int)")
  }

  "flatmapping using a Scala function" should "compile" in {
    assertCompiles(
      "Trials.only(1).flatMap((value => Trials.choose(value, 1.0 + value)): Int => Trials[Double])")
  }

  "filtering using a Scala function" should "compile" in {
    val _ = Trials.only(1).filter((1 == _): Int => Boolean)
    assertCompiles("Trials.only(1).filter((1 == _): Int => Boolean)")
  }
}
