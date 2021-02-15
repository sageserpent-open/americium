package com.sageserpent.americium

import cats.Eq
import cats.kernel.laws.discipline._
import cats.laws.{IsEqArrow, discipline}
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

import scala.collection.mutable

class TrialsLaws extends FlatSpec with Checkers {
  implicit def equality[X: Eq]: Eq[Trials[X]] =
    (first: Trials[X], second: Trials[X]) => {
      def capturesOf(trials: Trials[X]) = {
        val captures = mutable.ListBuffer.empty[X]

        trials.supplyTo {
          captures += (_: X): Unit
        }

        captures.toList
      }

      capturesOf(first) == capturesOf(second)
    }

  implicit def arbitraryTrials[X: Arbitrary]: Arbitrary[Trials[X]] = {
    val genX    = implicitly[Arbitrary[X]].arbitrary
    val viaOnly = genX.map(Trials.only)
    val viaChoose = for {
      first  <- genX
      second <- genX
    } yield Trials.choose(first, second)

    def viaAlternate: Gen[Trials[X]] =
      for {
        first  <- viaOnly
        second <- viaChoose
      } yield Trials.alternate(first, second)

    Arbitrary(Gen.oneOf(viaOnly, viaChoose, viaAlternate))
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
          .properties)
          .map { case (label, property) => label |: property }: _*))

  they should "have consistent semantics for `filter` and `mapFilter`" in
    check((trials: Trials[Int]) => {
      // TODO - all of these explicit type annotations are getting on my nerves...
      trials.filter(1 == (_: Int) % 2).map((_: Int).toDouble / 2) <-> trials
        .mapFilter((caze: Int) =>
          if (1 == caze % 2) Some(caze.toDouble / 2) else None)
    })
}
