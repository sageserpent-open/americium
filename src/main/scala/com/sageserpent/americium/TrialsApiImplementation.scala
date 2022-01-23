package com.sageserpent.americium
import cats.Traverse
import cats.free.Free
import cats.free.Free.pure
import cats.implicits.*
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.TrialsImplementation.*
import com.sageserpent.americium.java.{
  CaseFactory,
  Trials as JavaTrials,
  TrialsApi as JavaTrialsApi
}
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsApi as ScalaTrialsApi
}

import _root_.java.lang.{
  Boolean as JavaBoolean,
  Byte as JavaByte,
  Character as JavaCharacter,
  Double as JavaDouble,
  Integer as JavaInteger,
  Iterable as JavaIterable,
  Long as JavaLong
}
import _root_.java.time.Instant
import _root_.java.util.function.{Supplier, Function as JavaFunction}
import _root_.java.util.{List as JavaList, Map as JavaMap}
import scala.annotation.varargs
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters.*
import scala.util.Random

object TrialsApiImplementation {
  abstract class CommonApi {

    def only[Case](onlyCase: Case): TrialsImplementation[Case] =
      TrialsImplementation(pure[GenerationOperation, Case](onlyCase))

    def stream[Case](
        caseFactory: CaseFactory[Case]
    ): TrialsImplementation[Case] = new TrialsImplementation(
      Factory(new CaseFactory[Case] {
        override def apply(input: Long): Case = {
          require(lowerBoundInput() <= input)
          require(upperBoundInput() >= input)
          caseFactory(input)
        }
        override def lowerBoundInput(): Long = caseFactory.lowerBoundInput()
        override def upperBoundInput(): Long = caseFactory.upperBoundInput()
        override def maximallyShrunkInput(): Long =
          caseFactory.maximallyShrunkInput()
      })
    )
  }

  val javaApi = new CommonApi with JavaTrialsApi {
    override def delay[Case](
        delayed: Supplier[JavaTrials[Case]]
    ): JavaTrials[Case] = scalaApi.delay(delayed.get().scalaTrials)

    override def choose[Case <: AnyRef](
        firstChoice: Case,
        secondChoice: Case,
        otherChoices: Case*
    ): TrialsImplementation[Case] =
      scalaApi.choose(firstChoice +: secondChoice +: otherChoices)

    override def choose[Case](
        choices: JavaIterable[Case]
    ): TrialsImplementation[Case] =
      scalaApi.choose(choices.asScala)

    override def choose[Case <: AnyRef](
        choices: Array[Case]
    ): TrialsImplementation[Case] =
      scalaApi.choose(choices.toSeq)

    override def chooseWithWeights[Case](
        firstChoice: JavaMap.Entry[JavaInteger, Case],
        secondChoice: JavaMap.Entry[JavaInteger, Case],
        otherChoices: JavaMap.Entry[JavaInteger, Case]*
    ): TrialsImplementation[Case] =
      scalaApi.chooseWithWeights(
        (firstChoice +: secondChoice +: otherChoices) map (entry =>
          Int.unbox(entry.getKey) -> entry.getValue
        )
      )

    override def chooseWithWeights[Case](
        choices: JavaIterable[JavaMap.Entry[JavaInteger, Case]]
    ): TrialsImplementation[Case] =
      scalaApi.chooseWithWeights(
        choices.asScala.map(entry => Int.unbox(entry.getKey) -> entry.getValue)
      )

    override def chooseWithWeights[Case](
        choices: Array[JavaMap.Entry[JavaInteger, Case]]
    ): TrialsImplementation[Case] =
      scalaApi.chooseWithWeights(
        choices.toSeq.map(entry => Int.unbox(entry.getKey) -> entry.getValue)
      )

    override def alternate[Case](
        firstAlternative: JavaTrials[_ <: Case],
        secondAlternative: JavaTrials[_ <: Case],
        otherAlternatives: JavaTrials[_ <: Case]*
    ): TrialsImplementation[Case] =
      scalaApi.alternate(
        (firstAlternative +: secondAlternative +: otherAlternatives)
          .map(
            _.scalaTrials: Trials[Case]
            /* Need this type ascription to avoid the Scala 3 compiler treating
             * this as an error with a message that gibbers on about hygiene. */
          )
      )

    override def alternate[Case](
        alternatives: JavaIterable[JavaTrials[Case]]
    ): TrialsImplementation[Case] =
      scalaApi.alternate(alternatives.asScala.map(_.scalaTrials))

    override def alternate[Case](
        alternatives: Array[JavaTrials[Case]]
    ): TrialsImplementation[Case] =
      scalaApi.alternate(alternatives.toSeq.map(_.scalaTrials))

    override def alternateWithWeights[Case](
        firstAlternative: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]],
        secondAlternative: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]],
        otherAlternatives: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]]*
    ): TrialsImplementation[Case] = scalaApi.alternateWithWeights(
      (firstAlternative +: secondAlternative +: otherAlternatives)
        .map(entry => Int.unbox(entry.getKey) -> entry.getValue.scalaTrials)
    )

    override def alternateWithWeights[Case](
        alternatives: JavaIterable[JavaMap.Entry[JavaInteger, JavaTrials[Case]]]
    ): TrialsImplementation[Case] =
      scalaApi.alternateWithWeights(
        alternatives.asScala.map(entry =>
          Int.unbox(entry.getKey) -> entry.getValue.scalaTrials
        )
      )

    override def alternateWithWeights[Case](
        alternatives: Array[JavaMap.Entry[JavaInteger, JavaTrials[Case]]]
    ): TrialsImplementation[Case] = scalaApi.alternateWithWeights(
      alternatives.toSeq.map(entry =>
        Int.unbox(entry.getKey) -> entry.getValue.scalaTrials
      )
    )

    override def lists[Case](
        listOfTrials: JavaList[JavaTrials[Case]]
    ): TrialsImplementation[ImmutableList[Case]] =
      // NASTY HACK: make a throwaway trials of type `TrialsImplementation` to
      // act as a springboard to flatmap the 'real' result into the correct
      // type.
      scalaApi
        .only(())
        .flatMap((_: Unit) =>
          scalaApi
            .sequences[Case, List](
              listOfTrials.asScala.map(_.scalaTrials).toList
            )
            .map { sequence =>
              val builder = ImmutableList.builder[Case]()
              sequence.foreach(builder.add)
              builder.build()
            }
        )

    override def complexities(): TrialsImplementation[JavaInteger] =
      scalaApi.complexities.map(Int.box)

    override def streamLegacy[Case](
        factory: JavaFunction[JavaLong, Case]
    ): TrialsImplementation[Case] = stream(
      new CaseFactory[Case] {
        override def apply(input: Long): Case   = factory(input)
        override val lowerBoundInput: Long      = Long.MinValue
        override val upperBoundInput: Long      = Long.MaxValue
        override val maximallyShrunkInput: Long = 0L
      }
    )

    override def bytes(): JavaTrials[JavaByte] =
      scalaApi.bytes.map(Byte.box)

    override def integers(): TrialsImplementation[JavaInteger] =
      scalaApi.integers.map(Int.box)

    override def integers(
        lowerBound: Int,
        upperBound: Int
    ): TrialsImplementation[JavaInteger] =
      scalaApi.integers(lowerBound, upperBound).map(Int.box)

    override def integers(
        lowerBound: Int,
        upperBound: Int,
        shrinkageTarget: Int
    ): TrialsImplementation[JavaInteger] =
      scalaApi.integers(lowerBound, upperBound, shrinkageTarget).map(Int.box)

    override def nonNegativeIntegers(): TrialsImplementation[JavaInteger] =
      scalaApi.nonNegativeIntegers.map(Int.box)

    override def longs(): TrialsImplementation[JavaLong] =
      scalaApi.longs.map(Long.box)

    override def longs(
        lowerBound: Long,
        upperBound: Long
    ): TrialsImplementation[JavaLong] =
      scalaApi.longs(lowerBound, upperBound).map(Long.box)

    override def longs(
        lowerBound: Long,
        upperBound: Long,
        shrinkageTarget: Long
    ): TrialsImplementation[JavaLong] =
      scalaApi.longs(lowerBound, upperBound, shrinkageTarget).map(Long.box)

    override def nonNegativeLongs(): TrialsImplementation[JavaLong] =
      scalaApi.nonNegativeLongs.map(Long.box)

    override def doubles(): TrialsImplementation[JavaDouble] =
      scalaApi.doubles.map(Double.box)

    override def booleans(): TrialsImplementation[JavaBoolean] =
      scalaApi.booleans.map(Boolean.box)

    override def characters(): TrialsImplementation[JavaCharacter] =
      scalaApi.characters.map(Char.box)

    override def characters(
        lowerBound: Char,
        upperBound: Char
    ): TrialsImplementation[JavaCharacter] =
      scalaApi.characters(lowerBound, upperBound).map(Char.box)

    override def characters(
        lowerBound: Char,
        upperBound: Char,
        shrinkageTarget: Char
    ): TrialsImplementation[JavaCharacter] =
      scalaApi.characters(lowerBound, upperBound, shrinkageTarget).map(Char.box)

    override def instants(): TrialsImplementation[Instant] =
      scalaApi.instants

    override def strings(): TrialsImplementation[String] =
      scalaApi.strings
  }

  class ScalaTrialsApiImplementation extends CommonApi with ScalaTrialsApi {
    override def delay[Case](
        delayed: => ScalaTrials[Case]
    ): TrialsImplementation[Case] =
      TrialsImplementation(Free.defer(delayed.generation))

    override def choose[Case](
        firstChoice: Case,
        secondChoice: Case,
        otherChoices: Case*
    ): TrialsImplementation[Case] = choose(
      firstChoice +: secondChoice +: otherChoices
    )

    override def choose[Case](
        choices: Iterable[Case]
    ): TrialsImplementation[Case] =
      new TrialsImplementation(
        Choice(SortedMap.from(LazyList.from(1).zip(choices)))
      )

    override def chooseWithWeights[Case](
        firstChoice: (Int, Case),
        secondChoice: (Int, Case),
        otherChoices: (Int, Case)*
    ): TrialsImplementation[Case] =
      chooseWithWeights(firstChoice +: secondChoice +: otherChoices)

    override def chooseWithWeights[Case](
        choices: Iterable[(Int, Case)]
    ): TrialsImplementation[Case] =
      new TrialsImplementation(
        Choice(choices.unzip match {
          case (weights, plainChoices) =>
            SortedMap.from(
              weights
                .scanLeft(0) {
                  case (cumulativeWeight, weight) if 0 < weight =>
                    cumulativeWeight + weight
                  case (_, weight) =>
                    throw new IllegalArgumentException(
                      s"Weight $weight amongst provided weights of $weights must be greater than zero"
                    )
                }
                .drop(1)
                .zip(plainChoices)
            )
        })
      )

    override def alternate[Case](
        firstAlternative: ScalaTrials[Case],
        secondAlternative: ScalaTrials[Case],
        otherAlternatives: ScalaTrials[Case]*
    ): TrialsImplementation[Case] =
      alternate(
        firstAlternative +: secondAlternative +: otherAlternatives
      )

    override def alternate[Case](
        alternatives: Iterable[ScalaTrials[Case]]
    ): TrialsImplementation[Case] =
      choose(alternatives).flatMap(identity[ScalaTrials[Case]])

    override def alternateWithWeights[Case](
        firstAlternative: (Int, ScalaTrials[Case]),
        secondAlternative: (Int, ScalaTrials[Case]),
        otherAlternatives: (Int, ScalaTrials[Case])*
    ): TrialsImplementation[Case] = alternateWithWeights(
      firstAlternative +: secondAlternative +: otherAlternatives
    )

    override def alternateWithWeights[Case](
        alternatives: Iterable[
          (Int, ScalaTrials[Case])
        ]
    ): TrialsImplementation[Case] =
      chooseWithWeights(alternatives).flatMap(identity[ScalaTrials[Case]])

    override def sequences[Case, Sequence[_]: Traverse](
        sequenceOfTrials: Sequence[ScalaTrials[Case]]
    )(implicit
        factory: collection.Factory[Case, Sequence[Case]]
    ): ScalaTrials[Sequence[Case]] = sequenceOfTrials.sequence

    override def complexities: TrialsImplementation[Int] =
      new TrialsImplementation(NoteComplexity)

    def resetComplexity(complexity: Int): TrialsImplementation[Unit] =
      new TrialsImplementation(ResetComplexity(complexity))

    override def streamLegacy[Case](
        factory: Long => Case
    ): TrialsImplementation[Case] = stream(
      new CaseFactory[Case] {
        override def apply(input: Long): Case   = factory(input)
        override val lowerBoundInput: Long      = Long.MinValue
        override val upperBoundInput: Long      = Long.MaxValue
        override val maximallyShrunkInput: Long = 0L
      }
    )

    override def bytes: TrialsImplementation[Byte] =
      stream(new CaseFactory[Byte] {
        override def apply(input: Long): Byte     = input.toByte
        override def lowerBoundInput(): Long      = Byte.MinValue
        override def upperBoundInput(): Long      = Byte.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def integers: TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = Int.MinValue
        override def upperBoundInput(): Long      = Int.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def integers(
        lowerBound: Int,
        upperBound: Int
    ): TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int = input.toInt
        override def lowerBoundInput(): Long = lowerBound
        override def upperBoundInput(): Long = upperBound
        override def maximallyShrunkInput(): Long = if (0L > upperBound)
          upperBound
        else if (0L < lowerBound) lowerBound
        else 0L
      })

    override def integers(
        lowerBound: Int,
        upperBound: Int,
        shrinkageTarget: Int
    ): TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = lowerBound
        override def upperBoundInput(): Long      = upperBound
        override def maximallyShrunkInput(): Long = shrinkageTarget
      })

    override def nonNegativeIntegers: TrialsImplementation[Int] =
      stream(new CaseFactory[Int] {
        override def apply(input: Long): Int      = input.toInt
        override def lowerBoundInput(): Long      = 0L
        override def upperBoundInput(): Long      = Int.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def longs: TrialsImplementation[Long] = streamLegacy(identity)

    override def longs(
        lowerBound: Long,
        upperBound: Long
    ): TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long = input
        override def lowerBoundInput(): Long  = lowerBound
        override def upperBoundInput(): Long  = upperBound
        override def maximallyShrunkInput(): Long = if (0L > upperBound)
          upperBound
        else if (0L < lowerBound) lowerBound
        else 0L
      })

    override def longs(
        lowerBound: Long,
        upperBound: Long,
        shrinkageTarget: Long
    ): TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = lowerBound
        override def upperBoundInput(): Long      = upperBound
        override def maximallyShrunkInput(): Long = shrinkageTarget
      })

    override def nonNegativeLongs: TrialsImplementation[Long] =
      stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = 0L
        override def upperBoundInput(): Long      = Long.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })

    override def doubles: TrialsImplementation[Double] =
      streamLegacy { input =>
        val betweenZeroAndOne = new Random(input).nextDouble()
        Math.scalb(
          betweenZeroAndOne,
          (input.toDouble * JavaDouble.MAX_EXPONENT / Long.MaxValue).toInt
        )
      }
        .flatMap(zeroOrPositive =>
          booleans
            .map((negative: Boolean) =>
              if (negative) -zeroOrPositive else zeroOrPositive
            ): ScalaTrials[Double]
        )

    override def booleans: TrialsImplementation[Boolean] =
      choose(true, false)

    override def characters: TrialsImplementation[Char] =
      choose(Char.MinValue to Char.MaxValue)

    override def characters(
        lowerBound: Char,
        upperBound: Char
    ): TrialsImplementation[Char] = choose(lowerBound to upperBound)

    override def characters(
        lowerBound: Char,
        upperBound: Char,
        shrinkageTarget: Char
    ): TrialsImplementation[Char] = stream(new CaseFactory[Char] {
      override def apply(input: Long): Char     = input.toChar
      override def lowerBoundInput(): Long      = lowerBound.toLong
      override def upperBoundInput(): Long      = upperBound.toLong
      override def maximallyShrunkInput(): Long = shrinkageTarget.toLong
    })

    override def instants: TrialsImplementation[Instant] =
      longs.map(Instant.ofEpochMilli)

    override def strings: TrialsImplementation[String] = {
      characters.several[String]
    }
  }

  val scalaApi = new ScalaTrialsApiImplementation()
}
