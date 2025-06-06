package com.sageserpent.americium

import cats.Eq
import cats.kernel.laws.discipline.*
import cats.laws.{IsEqArrow, discipline}
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers

import scala.collection.mutable

class TrialsLaws extends AnyFlatSpec with Checkers {
  private val api = Trials.api

  implicit def equality[X: Eq]: Eq[Trials[X]] =
    (first: Trials[X], second: Trials[X]) => {
      val capturesSizeLimit = 100

      def capturesOf(trials: Trials[X]): List[X] = {
        val captures = mutable.ListBuffer.empty[X]

        trials
          .withLimit(capturesSizeLimit)
          .withValidTrialsCheck(enabled = false)
          .supplyTo {
            captures += (_: X): Unit
          }

        captures.toList
      }

      capturesOf(first) == capturesOf(second)
    }

  implicit def arbitraryTrials[X: Arbitrary]: Arbitrary[Trials[X]] = {
    val genX      = implicitly[Arbitrary[X]].arbitrary
    val viaOnly   = genX.map(api.only)
    val viaChoose = for {
      first  <- genX
      second <- genX
    } yield api.choose(first, second)

    def viaAlternate: Gen[Trials[X]] =
      for {
        first  <- viaOnly
        second <- viaChoose
        third  <- byHookOrByCrook
      } yield api.alternate(first, second, third)

    def byHookOrByCrook: Gen[Trials[X]] =
      Gen.oneOf(viaOnly, viaChoose, viaAlternate)

    Arbitrary(byHookOrByCrook)
  }

  they should "be a monad" in
    check(
      Prop.all(
        (discipline
          .MonadTests[Trials]
          .monad[Int, Int, String]
          .all
          .properties ++ discipline
          .FunctorFilterTests[Trials]
          .functorFilter[Int, Int, String]
          .all
          .properties).map { case (label, property) =>
          label |: property
        }.toSeq: _*
      ),
      MinSuccessful(1000)
    )

  they should "have consistent semantics for `filter` and `mapFilter`" in
    check(
      (trials: Trials[Int]) => {
        trials.filter(1 == _ % 2).map(_.toDouble / 2) <-> trials
          .mapFilter(caze =>
            if (1 == caze % 2) Some(caze.toDouble / 2) else None
          )
      },
      MinSuccessful(1000)
    )
}
