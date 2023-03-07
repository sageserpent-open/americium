package com.sageserpent.americium.java
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.generation.Factory
import com.sageserpent.americium.java.Trials as JavaTrials
import com.sageserpent.americium.{
  CommonApi,
  Trials,
  TrialsImplementation,
  CaseFactory as ScalaCaseFactory
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
import _root_.java.math.{BigInteger, BigDecimal as JavaBigDecimal}
import _root_.java.util.function.{Supplier, Function as JavaFunction}
import _root_.java.util.{List as JavaList, Map as JavaMap}
import java.time.Instant
import scala.jdk.CollectionConverters.*

trait TrialsApiImplementation extends CommonApi with TrialsApiWart {

  override def delay[Case](
      delayed: Supplier[JavaTrials[Case]]
  ): JavaTrials[Case] = scalaApi.delay(delayed.get().scalaTrials)

  override def impossible[Case](): TrialsImplementation[Case] =
    scalaApi.impossible[Case]

  override def choose[Case](
      choices: JavaIterable[Case]
  ): TrialsImplementation[Case] =
    scalaApi.choose(choices.asScala)

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

  override def immutableLists[Case](
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

  override def collections[Case, Collection](
      iterableOfTrials: JavaIterable[JavaTrials[Case]],
      builderFactory: Supplier[Builder[Case, Collection]]
  ): JavaTrials[Collection] =
    // NASTY HACK: make a throwaway trials of type `TrialsImplementation` to
    // act as a springboard to flatmap the 'real' result into the correct
    // type.
    scalaApi
      .only(())
      .flatMap((_: Unit) =>
        scalaApi
          .sequences[Case, List](
            iterableOfTrials.asScala.map(_.scalaTrials).toList
          )
          .map { sequence =>
            val builder = builderFactory.get()
            sequence.foreach(builder.add)
            builder.build()
          }
      )

  override def complexities(): TrialsImplementation[JavaInteger] =
    scalaApi.complexities.map(Int.box)

  def stream[Case](
      caseFactory: CaseFactory[Case]
  ): TrialsImplementation[Case] = new TrialsImplementation(
    Factory(new ScalaCaseFactory[Case] {
      override def apply(input: BigInt): Case = {
        require(0 >= lowerBoundInput.compareTo(input))
        require(0 <= upperBoundInput.compareTo(input))
        caseFactory(input.bigInteger)
      }
      override def lowerBoundInput: BigInt = caseFactory.lowerBoundInput()
      override def upperBoundInput: BigInt = caseFactory.upperBoundInput()
      override def maximallyShrunkInput: BigInt =
        caseFactory.maximallyShrunkInput
    })
  )

  override def streamLegacy[Case](
      factory: JavaFunction[JavaLong, Case]
  ): TrialsImplementation[Case] = stream(
    new CaseFactory[Case] {
      override def apply(input: BigInteger): Case =
        factory(input.longValue())
      override val lowerBoundInput: BigInteger =
        BigInteger.valueOf(Long.MinValue)
      override val upperBoundInput: BigInteger =
        BigInteger.valueOf(Long.MaxValue)
      override val maximallyShrunkInput: BigInteger = BigInteger.ZERO
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

  override def bigIntegers(
      lowerBound: BigInteger,
      upperBound: BigInteger
  ): TrialsImplementation[BigInteger] =
    scalaApi
      .bigInts(
        BigInt.javaBigInteger2bigInt(lowerBound),
        BigInt.javaBigInteger2bigInt(upperBound)
      )
      .map(_.bigInteger)

  override def bigIntegers(
      lowerBound: BigInteger,
      upperBound: BigInteger,
      shrinkageTarget: BigInteger
  ): TrialsImplementation[BigInteger] =
    scalaApi
      .bigInts(
        BigInt.javaBigInteger2bigInt(lowerBound),
        BigInt.javaBigInteger2bigInt(upperBound),
        BigInt.javaBigInteger2bigInt(shrinkageTarget)
      )
      .map(_.bigInteger)

  override def doubles(): TrialsImplementation[JavaDouble] =
    scalaApi.doubles.map(Double.box)

  override def doubles(
      lowerBound: Double,
      upperBound: Double
  ): TrialsImplementation[JavaDouble] =
    scalaApi.doubles(lowerBound, upperBound).map(Double.box)

  override def doubles(
      lowerBound: Double,
      upperBound: Double,
      shrinkageTarget: Double
  ): TrialsImplementation[JavaDouble] =
    scalaApi.doubles(lowerBound, upperBound, shrinkageTarget).map(Double.box)

  override def bigDecimals(
      lowerBound: JavaBigDecimal,
      upperBound: JavaBigDecimal
  ): TrialsImplementation[JavaBigDecimal] =
    scalaApi
      .bigDecimals(
        BigDecimal.javaBigDecimal2bigDecimal(lowerBound),
        BigDecimal.javaBigDecimal2bigDecimal(upperBound)
      )
      .map(_.bigDecimal)

  override def bigDecimals(
      lowerBound: JavaBigDecimal,
      upperBound: JavaBigDecimal,
      shrinkageTarget: JavaBigDecimal
  ): TrialsImplementation[JavaBigDecimal] =
    scalaApi
      .bigDecimals(
        BigDecimal.javaBigDecimal2bigDecimal(lowerBound),
        BigDecimal.javaBigDecimal2bigDecimal(upperBound),
        BigDecimal.javaBigDecimal2bigDecimal(shrinkageTarget)
      )
      .map(_.bigDecimal)

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

  override def instants(
      lowerBound: Instant,
      upperBound: Instant
  ): TrialsImplementation[Instant] =
    scalaApi.instants(lowerBound, upperBound)

  override def instants(
      lowerBound: Instant,
      upperBound: Instant,
      shrinkageTarget: Instant
  ): TrialsImplementation[Instant] =
    scalaApi.instants(lowerBound, upperBound, shrinkageTarget)

  override def strings(): TrialsImplementation[String] =
    scalaApi.strings
}
