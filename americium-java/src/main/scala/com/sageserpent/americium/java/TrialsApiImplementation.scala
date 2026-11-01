package com.sageserpent.americium.java
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.generation.Factory
import com.sageserpent.americium.java.Trials as JavaTrials
import com.sageserpent.americium.{CaseFactory as ScalaCaseFactory}

import _root_.java.lang.{
  Boolean as JavaBoolean,
  Byte as JavaByte,
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

trait TrialsApiImplementation extends TrialsApiWart {

  override def delay[Case](
      delayed: Supplier[JavaTrials[Case]]
  ): JavaTrials[Case] = TrialsImplementation(
    scalaApi.delay(delayed.get().scalaTrials)
  )

  override def only[Case](onlyCase: Case): JavaTrials[Case] =
    TrialsImplementation(scalaApi.only(onlyCase))

  override def impossible[Case](): JavaTrials[Case] =
    TrialsImplementation(scalaApi.impossible[Case])

  override def choose[Case](
      choices: JavaIterable[Case]
  ): JavaTrials[Case] =
    TrialsImplementation(scalaApi.choose(choices.asScala))

  override def chooseWithWeights[Case](
      firstChoice: JavaMap.Entry[JavaInteger, Case],
      secondChoice: JavaMap.Entry[JavaInteger, Case],
      otherChoices: JavaMap.Entry[JavaInteger, Case]*
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.chooseWithWeights(
        (firstChoice.getKey.toInt  -> firstChoice.getValue),
        (secondChoice.getKey.toInt -> secondChoice.getValue),
        otherChoices.map(entry => entry.getKey.toInt -> entry.getValue): _*
      )
    )

  override def chooseWithWeights[Case](
      choices: JavaIterable[JavaMap.Entry[JavaInteger, Case]]
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.chooseWithWeights(
        choices.asScala.map(entry => entry.getKey.toInt -> entry.getValue)
      )
    )

  override def chooseWithWeights[Case](
      choices: Array[JavaMap.Entry[JavaInteger, Case]]
  ): JavaTrials[Case] =
    chooseWithWeights(choices.toSeq.asJava)

  override def alternate[Case](
      firstAlternative: JavaTrials[_ <: Case],
      secondAlternative: JavaTrials[_ <: Case],
      otherAlternatives: JavaTrials[_ <: Case]*
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.alternate(
        firstAlternative.scalaTrials,
        secondAlternative.scalaTrials,
        otherAlternatives.map(_.scalaTrials): _*
      )
    )

  override def alternate[Case](
      alternatives: JavaIterable[JavaTrials[Case]]
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.alternate(alternatives.asScala.map(_.scalaTrials))
    )

  override def alternate[Case](
      alternatives: Array[JavaTrials[Case]]
  ): JavaTrials[Case] =
    alternate(alternatives.toSeq.asJava)

  override def alternateWithWeights[Case](
      firstAlternative: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]],
      secondAlternative: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]],
      otherAlternatives: JavaMap.Entry[JavaInteger, JavaTrials[_ <: Case]]*
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.alternateWithWeights(
        (firstAlternative.getKey.toInt -> firstAlternative.getValue.scalaTrials),
        (secondAlternative.getKey.toInt -> secondAlternative.getValue.scalaTrials),
        otherAlternatives.map(entry =>
          entry.getKey.toInt -> entry.getValue.scalaTrials
        ): _*
      )
    )

  override def alternateWithWeights[Case](
      alternatives: JavaIterable[JavaMap.Entry[JavaInteger, JavaTrials[Case]]]
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.alternateWithWeights(
        alternatives.asScala.map(entry =>
          entry.getKey.toInt -> entry.getValue.scalaTrials
        )
      )
    )

  override def alternateWithWeights[Case](
      alternatives: Array[JavaMap.Entry[JavaInteger, JavaTrials[Case]]]
  ): JavaTrials[Case] =
    alternateWithWeights(alternatives.toSeq.asJava)

  override def immutableLists[Element](
      trials: JavaList[JavaTrials[Element]]
  ): JavaTrials[ImmutableList[Element]] =
    TrialsImplementation(
      scalaApi
        .sequences[Element, List](
          trials.asScala.map(_.scalaTrials).toList
        )
        .map { sequence =>
          val builder = ImmutableList.builder[Element]()
          sequence.foreach(builder.add)
          builder.build()
        }
    )

  override def collections[Element, Collection](
      trials: JavaIterable[JavaTrials[Element]],
      builderFactory: Supplier[Builder[Element, Collection]]
  ): JavaTrials[Collection] = {
    TrialsImplementation(
      scalaApi
        .sequences[Element, List](
          trials.asScala.map(_.scalaTrials).toList
        )
        .map { sequence =>
          val builder = builderFactory.get()
          sequence.foreach(builder.add)
          builder.build()
        }
    )
  }

  override def complexities(): JavaTrials[JavaInteger] =
    TrialsImplementation(scalaApi.complexities.map(JavaInteger.valueOf))

  override def uniqueIds(): JavaTrials[JavaInteger] =
    TrialsImplementation(scalaApi.uniqueIds.map(JavaInteger.valueOf))

  override def stream[Case](
      caseFactory: CaseFactory[Case]
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.stream(new ScalaCaseFactory[Case] {
        override def apply(input: BigInt): Case =
          caseFactory(input.bigInteger)

        override def lowerBoundInput: BigInt =
          caseFactory.lowerBoundInput()

        override def upperBoundInput: BigInt =
          caseFactory.upperBoundInput()

        override def maximallyShrunkInput: BigInt =
          caseFactory.maximallyShrunkInput()
      })
    )

  override def streamLegacy[Case](
      factory: JavaFunction[JavaLong, Case]
  ): JavaTrials[Case] =
    TrialsImplementation(
      scalaApi.streamLegacy(factory.apply _ compose JavaLong.valueOf)
    )

  override def bytes(): JavaTrials[JavaByte] =
    TrialsImplementation(scalaApi.bytes.map(Byte.box))

  override def integers(): JavaTrials[JavaInteger] =
    integers(Int.MinValue, Int.MaxValue)

  override def integers(
      lowerBound: Int,
      upperBound: Int
  ): JavaTrials[JavaInteger] =
    integers(lowerBound, upperBound, 0 max lowerBound min upperBound)

  override def integers(
      lowerBound: Int,
      upperBound: Int,
      shrinkageTarget: Int
  ): JavaTrials[JavaInteger] =
    TrialsImplementation(
      scalaApi
        .integers(lowerBound, upperBound, shrinkageTarget)
        .map(Int.box)
    )

  override def nonNegativeIntegers(): JavaTrials[JavaInteger] =
    integers(0, Int.MaxValue)

  override def longs(): JavaTrials[JavaLong] =
    longs(Long.MinValue, Long.MaxValue)

  override def longs(
      lowerBound: Long,
      upperBound: Long
  ): JavaTrials[JavaLong] =
    longs(lowerBound, upperBound, 0L max lowerBound min upperBound)

  override def longs(
      lowerBound: Long,
      upperBound: Long,
      shrinkageTarget: Long
  ): JavaTrials[JavaLong] =
    TrialsImplementation(
      scalaApi
        .longs(lowerBound, upperBound, shrinkageTarget)
        .map(Long.box)
    )

  override def nonNegativeLongs(): JavaTrials[JavaLong] =
    longs(0, Long.MaxValue)

  override def bigIntegers(
      lowerBound: BigInteger,
      upperBound: BigInteger
  ): JavaTrials[BigInteger] =
    bigIntegers(
      lowerBound,
      upperBound,
      BigInteger.ZERO.max(lowerBound).min(upperBound)
    )

  override def bigIntegers(
      lowerBound: BigInteger,
      upperBound: BigInteger,
      shrinkageTarget: BigInteger
  ): JavaTrials[BigInteger] =
    TrialsImplementation(
      scalaApi
        .bigInts(lowerBound, upperBound, shrinkageTarget)
        .map(_.bigInteger)
    )

  override def doubles(): JavaTrials[JavaDouble] =
    TrialsImplementation(scalaApi.doubles.map(Double.box))

  override def doubles(
      lowerBound: Double,
      upperBound: Double
  ): JavaTrials[JavaDouble] =
    doubles(lowerBound, upperBound, 0.0 max lowerBound min upperBound)

  override def doubles(
      lowerBound: Double,
      upperBound: Double,
      shrinkageTarget: Double
  ): JavaTrials[JavaDouble] =
    TrialsImplementation(
      scalaApi
        .doubles(lowerBound, upperBound, shrinkageTarget)
        .map(Double.box)
    )

  override def bigDecimals(
      lowerBound: JavaBigDecimal,
      upperBound: JavaBigDecimal
  ): JavaTrials[JavaBigDecimal] =
    bigDecimals(
      lowerBound,
      upperBound,
      JavaBigDecimal.ZERO.max(lowerBound).min(upperBound)
    )

  override def bigDecimals(
      lowerBound: JavaBigDecimal,
      upperBound: JavaBigDecimal,
      shrinkageTarget: JavaBigDecimal
  ): JavaTrials[JavaBigDecimal] =
    TrialsImplementation(
      scalaApi
        .bigDecimals(lowerBound, upperBound, shrinkageTarget)
        .map(_.bigDecimal)
    )

  override def booleans(): JavaTrials[JavaBoolean] =
    TrialsImplementation(scalaApi.booleans.map(Boolean.box))

  override def characters(
      lowerBound: Char,
      upperBound: Char
  ): CharacterTrials =
    DelegatingTrials.delegateTo(
      classOf[CharacterTrials],
      TrialsImplementation(
        scalaApi.characters(lowerBound, upperBound).map(Char.box)
      )
    )

  override def characters(
      lowerBound: Char,
      upperBound: Char,
      shrinkageTarget: Char
  ): CharacterTrials =
    DelegatingTrials.delegateTo(
      classOf[CharacterTrials],
      TrialsImplementation(
        scalaApi
          .characters(lowerBound, upperBound, shrinkageTarget)
          .map(Char.box)
      )
    )

  override def characters(): CharacterTrials =
    DelegatingTrials.delegateTo(
      classOf[CharacterTrials],
      TrialsImplementation(scalaApi.characters.map(Char.box))
    )

  override def instants(): JavaTrials[Instant] =
    TrialsImplementation(scalaApi.instants)

  override def instants(
      lowerBound: Instant,
      upperBound: Instant
  ): JavaTrials[Instant] =
    instants(
      lowerBound,
      upperBound,
      if (Instant.EPOCH.isBefore(lowerBound)) lowerBound
      else if (Instant.EPOCH.isAfter(upperBound)) upperBound
      else Instant.EPOCH
    )

  override def instants(
      lowerBound: Instant,
      upperBound: Instant,
      shrinkageTarget: Instant
  ): JavaTrials[Instant] =
    TrialsImplementation(
      scalaApi.instants(lowerBound, upperBound, shrinkageTarget)
    )

  override def strings(): JavaTrials[String] =
    characters().strings

  override def nonEmptyStrings(): JavaTrials[String] =
    characters().nonEmptyStrings()

  override def indexPermutations(
      numberOfIndices: Int
  ): JavaTrials[JavaList[JavaInteger]] =
    TrialsImplementation(
      scalaApi
        .indexPermutations(numberOfIndices)
        .map(_.map(Int.box).asJava)
    )

  override def indexPermutations(
      numberOfIndices: Int,
      permutationSize: Int
  ): JavaTrials[JavaList[JavaInteger]] =
    TrialsImplementation(
      scalaApi
        .indexPermutations(numberOfIndices, permutationSize)
        .map(_.map(Int.box).asJava)
    )

  override def indexCombinations(
      numberOfIndices: Int,
      combinationSize: Int
  ): JavaTrials[JavaList[JavaInteger]] =
    TrialsImplementation(
      scalaApi
        .indexCombinations(numberOfIndices, combinationSize)
        .map(_.map(Int.box).asJava)
    )

  override def chooseSeveralOf[Element](
      candidates: JavaIterable[Element],
      numberToChoose: Int
  ): JavaTrials[JavaList[Element]] =
    TrialsImplementation(
      scalaApi
        .chooseSeveralOf(candidates.asScala, numberToChoose)
        .map(_.asJava)
    )

  override def pickAlternatelyFrom[Element](
      shrinkToRoundRobin: Boolean,
      iterables: JavaIterable[Element]*
  ): JavaTrials[JavaList[Element]] = {
    val scalaIterables: Seq[Iterable[Element]] = iterables.map(_.asScala)

    TrialsImplementation(
      scalaApi
        .pickAlternatelyFrom(shrinkToRoundRobin, scalaIterables: _*)
        .map(_.asJava)
    )
  }

  override def splitsIntoPieces[Element](
      items: JavaList[Element],
      numberOfPieces: Int
  ): JavaTrials[JavaList[JavaList[Element]]] =
    TrialsImplementation(
      scalaApi
        .splitsIntoPieces[Element, List](items.asScala.toList, numberOfPieces)
        .map(_.map(_.asJava).asJava)
    )

  override def splitsIntoNonEmptyPieces[Element](
      items: JavaList[Element],
      numberOfPieces: Int
  ): JavaTrials[JavaList[JavaList[Element]]] =
    TrialsImplementation(
      scalaApi
        .splitsIntoNonEmptyPieces[Element, List](
          items.asScala.toList,
          numberOfPieces
        )
        .map(_.map(_.asJava).asJava)
    )

  override def splitsIntoNonEmptyPieces[Element](
      items: JavaList[Element]
  ): JavaTrials[JavaList[JavaList[Element]]] =
    TrialsImplementation(
      scalaApi
        .splitsIntoNonEmptyPieces[Element, List](items.asScala.toList)
        .map(_.map(_.asJava).asJava)
    )

  override def shuffles[Element](
      items: JavaList[Element]
  ): JavaTrials[JavaList[Element]] =
    TrialsImplementation(
      scalaApi
        .shuffles[Element, List](items.asScala.toList)
        .map(_.asJava)
    )
}
