package com.sageserpent.americium.java

import com.sageserpent.americium.Trials as ScalaTrials
import com.sageserpent.americium.generation.JavaPropertyNames.nondeterministicJavaProperty
import cyclops.control.Either as JavaEither
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.lang.Integer as JavaInteger
import java.util.function.{Consumer, Function as JavaFunction, Predicate}
import java.util.stream.IntStream
import java.util.{LinkedList as JavaLinkedList, List as JavaList, Optional}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait MockitoSessionSupport {
  protected def inMockitoSession[X](
      test: => Unit
  ): Unit = {
    val mockitoSession = Mockito.mockitoSession().startMocking()

    try {
      test
    } finally {
      mockitoSession.finishMocking()
    }
  }
}

class JavaApiSpec
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks
    with MockitoSessionSupport {

  val javaApi: TrialsApi = Trials.api()
  val limit: Int         = 350

  type TypeRequirementsToProtectCodeInStringsFromUnusedImportOptimisation =
    (Trials[_], JavaFunction[_, _], Predicate[_])

  "test driving the Java API" should "not produce smoke" in {
    val javaTrials = javaApi.choose(2, -4, 3)

    val flatMappedJavaTrials =
      javaTrials flatMap (integer => javaApi.only(1.1 * integer))

    flatMappedJavaTrials.withLimit(limit).supplyTo(println)

    val mappedJavaTrials = javaTrials map (_ * 2.5)

    mappedJavaTrials.withLimit(limit).supplyTo(println)

    javaApi
      .alternate(flatMappedJavaTrials, mappedJavaTrials)
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .choose((0 to 20).asJava)
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .alternate(Seq(flatMappedJavaTrials, mappedJavaTrials).asJava)
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .streamLegacy(_.toString)
      .withLimit(limit)
      .supplyTo(println)

    javaApi.bytes
      .withLimit(limit)
      .supplyTo(println)

    javaApi.integers
      .withLimit(limit)
      .supplyTo(println)

    javaApi.longs
      .withLimit(limit)
      .supplyTo(println)

    javaApi.doubles
      .withLimit(limit)
      .supplyTo(println)

    javaApi.booleans
      .withLimit(limit)
      .supplyTo(println)

    javaApi.characters
      .withLimit(limit)
      .supplyTo(println)

    javaApi.instants
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .characters()
      .strings()
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .characters('a', 'z', 'p')
      .strings()
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .choose(0, 1, 2, 5)
      .flatMap(size =>
        javaApi
          .characters('a', 'z')
          .stringsOfSize(size)
      )
      .withLimit(limit)
      .supplyTo(println)

    javaApi.integers.optionals
      .withLimit(limit)
      .supplyTo(println)

    javaApi.instants
      .or(javaApi.booleans)
      .withLimit(limit)
      .supplyTo(println)
  }

  "impossible trials via Java" should "produce no trials" in {
    val mockConsumer: Consumer[AnyRef] = mock(classOf[Consumer[AnyRef]])
    val sut: Trials[Any] = javaApi.integers().asInstanceOf[Trials[Any]]

    sut
      .flatMap((_: Any) => javaApi.impossible[AnyRef])
      .withLimit(limit)
      .withSeed(1L)
      .withValidTrialsCheck(false)
      .supplyTo(mockConsumer)

    verify(mockConsumer, never()).accept(any)
  }

  "mapping using a Java function" should "compile" in {
    assertCompiles("javaApi.only(1).map((_ + 1): JavaFunction[Int, Int])")
  }

  "flatmapping using a Java function" should "compile" in {
    assertCompiles(
      "javaApi.only(1).flatMap(value => javaApi.choose(value, 1.0 + value))"
    )
  }

  "filtering using a Java predicate" should "compile" in {
    assertCompiles("javaApi.only(1).filter(1 == _)")
  }

  "combination with Java `.or`" should "cover alternate finite choices" in {
    inMockitoSession {
      val mockConsumer: Consumer[JavaEither[String, Int]] =
        mock(classOf[Consumer[JavaEither[String, Int]]])

      val lefts: Trials[String] = javaApi.choose("Huey", "Duey", "Louie")

      val rights: Trials[Int] = javaApi.choose(
        IntStream
          .range(0, 10)
          .collect[JavaLinkedList[Int]](
            () => new JavaLinkedList[Int](),
            _.add(_),
            _.addAll(_)
          )
      )

      lefts
        .map(JavaEither.left[String, Int])
        .withLimit(limit)
        .supplyTo(left => doNothing().when(mockConsumer).accept(left))

      rights
        .map(JavaEither.right[String, Int])
        .withLimit(limit)
        .supplyTo(right => doNothing().when(mockConsumer).accept(right))

      (lefts or rights)
        .withLimit(limit)
        .supplyTo(mockConsumer)

      verifyNoMoreInteractions(mockConsumer)
    }
  }

  "lifting with Java `.optionals`" should "cover underlying finite choices and include `None`" in {
    inMockitoSession {
      val mockConsumer: Consumer[Optional[Int]] =
        mock(classOf[Consumer[Optional[Int]]])

      val underlyings: Trials[Int] =
        javaApi.choose(
          IntStream
            .range(0, 10)
            .collect[JavaLinkedList[Int]](
              () => new JavaLinkedList[Int](),
              _.add(_),
              _.addAll(_)
            )
        )

      underlyings
        .withLimit(limit)
        .supplyTo(underlyingCase =>
          doNothing().when(mockConsumer).accept(Optional.of(underlyingCase))
        )

      doNothing().when(mockConsumer).accept(Optional.empty)

      underlyings.optionals.withLimit(limit).supplyTo(mockConsumer)

      verifyNoMoreInteractions(mockConsumer)
    }
  }

  "combining Java trials via `sequence`" should "yield the Cartesian product of the elements" in {
    import com.sageserpent.americium.Trials as ScalaCoreTrials
    val elementMaximaTrials =
      ScalaCoreTrials.api.integers(1, 5).several[List[Int]]

    elementMaximaTrials.withLimit(20).supplyTo { elementMaxima =>
      def inRangeFromOneToMaximum(maximum: Int): Trials[Int] =
        javaApi.choose((1 to maximum).asJava)

      val trialsSequence = elementMaxima.map(inRangeFromOneToMaximum).asJava

      val cartesianProduct = javaApi.immutableLists(trialsSequence)

      val cartesianProductSize = elementMaxima.product

      val results = mutable.Set.empty[JavaList[Int]]

      cartesianProduct
        .withLimit(cartesianProductSize)
        .supplyTo { productMember =>
          results += productMember
        }

      results should have size cartesianProductSize
    }
  }

  "successive runs using Java trials" should "yield different results if nondeterminism is specified" in {
    val previousPropertyValue =
      Option(
        System.setProperty(nondeterministicJavaProperty, "true")
      )

    try {
      val sharedIrrelevantSeed = 1L

      val testCasesFromFirstRun = javaApi
        .integers()
        .withLimit(limit)
        .withSeed(sharedIrrelevantSeed)
        .asIterator()
        .asScala
        .toSeq

      val testCasesFromSecondRun = javaApi
        .integers()
        .withLimit(limit)
        .withSeed(sharedIrrelevantSeed)
        .asIterator()
        .asScala
        .toSeq

      testCasesFromSecondRun should not equal testCasesFromFirstRun
    } finally {
      previousPropertyValue.fold(ifEmpty =
        System.clearProperty(nondeterministicJavaProperty)
      )(
        System.setProperty(nondeterministicJavaProperty, _)
      )
    }
  }
}
