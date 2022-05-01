package com.sageserpent.americium

import com.sageserpent.americium.TrialsImplementation.recipeHashJavaPropertyName
import com.sageserpent.americium.TrialsScaffolding.noShrinking
import com.sageserpent.americium.java.{
  Builder,
  CaseFactory,
  Trials as JavaTrials,
  TrialsApi as JavaTrialsApi
}
import cyclops.control.Either as JavaEither
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito
import org.mockito.Mockito.{atMost as mockitoAtMost, *}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import _root_.java.util.function.{Consumer, Predicate, Function as JavaFunction}
import _root_.java.util.stream.IntStream
import _root_.java.util.{Optional, UUID, LinkedList as JavaLinkedList}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try

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

object TrialsSpec {
  case class JackInABox[Caze](caze: Caze)

  case class ExceptionWithCasePayload[Case](caze: Case) extends RuntimeException

  case class ChoicesAndCriterion[X](choices: Seq[X], criterion: X => Boolean)

  sealed trait BinaryTree {
    def flatten: Vector[Int]
  }

  final case class Leaf(value: Int) extends BinaryTree {
    override def flatten: Vector[Int] = Vector(value)
  }

  final case class Branch(
      leftSubtree: BinaryTree,
      flag: Boolean,
      rightSubtree: BinaryTree
  ) extends BinaryTree {
    override def flatten: Vector[Int] =
      leftSubtree.flatten ++ rightSubtree.flatten
  }

  final case class BushyTree(growth: Either[List[BushyTree], Int]) {
    growth.fold(branches => require(branches.nonEmpty), _ => ())

    def flatten: Vector[Int] = growth.fold(
      branches => branches.map(_.flatten).reduce(_ ++ _),
      leafValue => Vector(leafValue)
    )
  }

  val api: TrialsApi         = Trials.api
  val javaApi: JavaTrialsApi = JavaTrials.api

  val limit: Int = 350

  def byteVectorTrials: Trials[Vector[Byte]] = {
    // FIXME: the need to do this shows that some kind of weighted distribution
    // is a good idea.
    api.alternateWithWeights(1 -> api.only(0.toByte), 10 -> api.bytes).several
  }

  def integerVectorTrials: Trials[Vector[Int]] = {
    // FIXME: the need to do this shows that some kind of weighted distribution
    // is a good idea.
    api.alternateWithWeights(1 -> api.only(0), 10 -> api.integers).several
  }

  def doubleVectorTrials: Trials[Vector[Double]] =
    // FIXME: the need to do this shows that some kind of weighted distribution
    // is a good idea.
    api.alternateWithWeights(1 -> api.only(0.0), 10 -> api.doubles).several

  def longVectorTrials: Trials[Vector[Long]] =
    // FIXME: the need to do this shows that some kind of weighted distribution
    // is a good idea.
    api.alternateWithWeights(1 -> api.only(0L), 10 -> api.longs).several

  def listTrials: Trials[List[Int]] =
    // FIXME: the need to do this shows that some kind of weighted distribution
    // is a good idea.
    api.alternateWithWeights(3 -> api.only(0), 10 -> api.integers).several

  def binaryTreeTrials: Trials[BinaryTree] = api.alternate(
    for {
      leftSubtree  <- api.delay(binaryTreeTrials)
      flag         <- api.booleans
      rightSubtree <- binaryTreeTrials
    } yield Branch(leftSubtree, flag, rightSubtree),
    // FIXME: the need to do this shows that some kind of weighted
    // distribution is a good idea.
    api
      .alternateWithWeights(3 -> api.only(0), 10 -> api.integers)
      .map(Leaf.apply)
  )

  def bushyTreeTrials: Trials[BushyTree] =
    api.complexities.flatMap(complexity =>
      api
        .alternateWithWeights(
          1 -> api
            .choose(1 to 10)
            .flatMap(positiveNumberOfBranches =>
              bushyTreeTrials
                .listsOfSize(positiveNumberOfBranches)
                .map(Left.apply)
            ),
          (1 max complexity) -> api
            // FIXME: the need to do this shows that some kind of weighted
            // distribution is a good idea.
            .alternateWithWeights(1 -> api.only(0), 10 -> api.integers)
            .map(Right.apply)
        )
        .map(BushyTree.apply)
    )
}

class TrialsSpec
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks
    with MockitoSessionSupport {
  import TrialsSpec.*

  type TypeRequirementsToProtectCodeInStringsFromUnusedImportOptimisation =
    (JavaTrials[_], JavaFunction[_, _], Predicate[_])

  "test driving the Scala API" should "not produce smoke" in {
    val trials = api.choose(2, -4, 3)

    val flatMappedTrials = trials flatMap (integer => api.only(1.1 * integer))

    flatMappedTrials.withLimit(limit).supplyTo(println)

    val mappedTrials = trials map (_ * 2.5)

    mappedTrials.withLimit(limit).supplyTo(println)

    api
      .alternate(flatMappedTrials, mappedTrials)
      .withLimit(limit)
      .supplyTo(println)

    api
      .choose(0 to 20)
      .withLimit(limit)
      .supplyTo(println)

    api
      .alternate(Seq(flatMappedTrials, mappedTrials))
      .withLimit(limit)
      .supplyTo(println)

    api
      .choose(Array(1, 2, 3))
      .withLimit(limit)
      .supplyTo(println)

    api
      .streamLegacy(_.toString)
      .withLimit(limit)
      .supplyTo(println)

    api.bytes
      .withLimit(limit)
      .supplyTo(println)

    api.integers
      .withLimit(limit)
      .supplyTo(println)

    api.longs
      .withLimit(limit)
      .supplyTo(println)

    api.doubles
      .withLimit(limit)
      .supplyTo(println)

    api.booleans
      .withLimit(limit)
      .supplyTo(println)

    api.integers.sets
      .withLimit(limit)
      .supplyTo(println)

    api.integers.sortedSets
      .withLimit(limit)
      .supplyTo(println)

    api.integers
      .maps(api.strings)
      .withLimit(limit)
      .supplyTo(println)

    api.integers
      .sortedMaps(api.strings)
      .withLimit(limit)
      .supplyTo(println)

    api.integers
      .several[Vector[_]]
      .withLimit(limit)
      .supplyTo(println)

    api.characters
      .withLimit(limit)
      .supplyTo(println)

    api.instants
      .withLimit(limit)
      .supplyTo(println)

    api.strings
      .withLimit(limit)
      .supplyTo(println)

    api
      .characters('a', 'z', 'p')
      .strings
      .withLimit(limit)
      .supplyTo(println)

    api
      .choose(0, 1, 2, 5)
      .flatMap(size => api.characters('a', 'z').stringsOfSize(size))
      .withLimit(limit)
      .supplyTo(println)

    api.integers.options
      .withLimit(limit)
      .supplyTo(println)

    api.instants
      .or(api.booleans)
      .withLimit(limit)
      .supplyTo(println)
  }

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

    javaApi.strings
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .characters('a', 'z', 'p')
      .collections(Builder.stringBuilder _)
      .withLimit(limit)
      .supplyTo(println)

    javaApi
      .choose(0, 1, 2, 5)
      .flatMap(size =>
        javaApi
          .characters('a', 'z')
          .collectionsOfSize(size, Builder.stringBuilder _)
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

  "only one case" should "yield just one trial" in
    forAll(Table("case", 1, "foo", 2.3, List(false, 0, true))) { dataCase =>
      inMockitoSession {
        val sut = api.only(dataCase)

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        verify(mockConsumer).apply(dataCase)
      }
    }

  "only one case that provokes an exception" should "result in an exception that references it" in
    forAll(Table("case", 1, "foo", 2.3, Seq(false, 0, true))) { dataCase =>
      inMockitoSession {
        val sut = api.only(dataCase)

        val problem = new RuntimeException("Test problem")

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        doThrow(problem).when(mockConsumer).apply(dataCase)

        val exception = intercept[sut.TrialException] {
          sut.withLimit(limit).supplyTo(mockConsumer)
        }

        exception.getCause should be(problem)
        val provokingCase: Any = exception.provokingCase
        provokingCase should be(dataCase)
      }
    }

  it should "result in an exception that references it even when trials are joined with `and`" in
    forAll(
      Table(
        "case tuple",
        (1, 2, 3),
        ("foo", false, 2.3),
        (2.3, -1, Set.empty),
        (Seq(false, 0, true), List.empty, true)
      )
    ) { case (dataCase1, dataCase2, dataCase3) =>
      inMockitoSession {
        val sut =
          api.only(dataCase1) and api.only(dataCase2) and api.only(dataCase3)

        val problem = new RuntimeException("Test problem")

        val mockConsumer: ((Any, Any, Any)) => Unit =
          mock(classOf[((Any, Any, Any)) => Unit])

        doThrow(problem)
          .when(mockConsumer)
          .apply((dataCase1, dataCase2, dataCase3))

        val exception = intercept[sut.TrialException] {
          sut.withLimit(limit).supplyTo(mockConsumer)
        }

        exception.getCause should be(problem)
        val provokingCase: (Any, Any, Any) = exception.provokingCase
        provokingCase should be((dataCase1, dataCase2, dataCase3))
      }
    }

  "a choice" should "yield all and only the cases given to it" in
    forAll(
      Table(
        "possibleChoices",
        Seq.empty,
        1 to 10,
        -5 to 5 map (_.toString),
        Seq(true),
        Seq(4.3)
      )
    ) { possibleChoices =>
      inMockitoSession {
        val sut: Trials[Any] = api.choose(possibleChoices)

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        possibleChoices.foreach(possibleChoice =>
          verify(mockConsumer).apply(possibleChoice)
        )
      }
    }

  it should "yield all and only the cases given to it in the given weights" in
    forAll(
      Table(
        "possibleChoices",
        Seq.empty,
        1 to 10,
        -5 to 5 map (_.toString),
        Seq(true),
        Seq(4.3)
      )
    ) { possibleChoices =>
      inMockitoSession {
        val weightedChoices =
          possibleChoices.map(choice =>
            1 + choice.hashCode().abs % 10 -> choice
          )

        val sut: Trials[Any] = api.chooseWithWeights(weightedChoices)

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        weightedChoices.foreach { case (weight, possibleChoice) =>
          verify(mockConsumer, times(weight)).apply(possibleChoice)
        }
      }
    }

  private val isomorphismCaseFactoryTable = Table(
    "isomorphism case factories",
    new CaseFactory[String] {
      override def apply(input: Long): String   = "Singleton"
      override def lowerBoundInput(): Long      = 434
      override def upperBoundInput(): Long      = 434
      override def maximallyShrunkInput(): Long = 434
    },
    new CaseFactory[String] {
      override def apply(input: Long): String   = "Singleton"
      override def lowerBoundInput(): Long      = 0
      override def upperBoundInput(): Long      = 0
      override def maximallyShrunkInput(): Long = 0
    },
    new CaseFactory[Boolean] {
      override def apply(input: Long): Boolean = input match {
        case -1L => false
        case 0L  => true
      }
      override def lowerBoundInput(): Long      = -1L
      override def upperBoundInput(): Long      = 0L
      override def maximallyShrunkInput(): Long = 0L
    },
    new CaseFactory[Boolean] {
      override def apply(input: Long): Boolean = input match {
        case 0L => false
        case 1L => true
      }
      override def lowerBoundInput(): Long      = 0L
      override def upperBoundInput(): Long      = 1L
      override def maximallyShrunkInput(): Long = 0L
    },
    new CaseFactory[Boolean] {
      override def apply(input: Long): Boolean = input match {
        case -1L => true
        case 0L  => false
      }
      override def lowerBoundInput(): Long      = -1L
      override def upperBoundInput(): Long      = 0L
      override def maximallyShrunkInput(): Long = -1L
    },
    new CaseFactory[Boolean] {
      override def apply(input: Long): Boolean = input match {
        case 0L => true
        case 1L => false
      }
      override def lowerBoundInput(): Long      = 0L
      override def upperBoundInput(): Long      = 1L
      override def maximallyShrunkInput(): Long = 1L
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = 2
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = -3
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = 0
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = -3
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = -3
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = 5
      override def maximallyShrunkInput(): Long = 5
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = 0
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = Byte.MaxValue / 2
    },
    new CaseFactory[Byte] {
      override def apply(input: Long): Byte     = input.toByte
      override def lowerBoundInput(): Long      = Byte.MinValue
      override def upperBoundInput(): Long      = Byte.MaxValue
      override def maximallyShrunkInput(): Long = Byte.MinValue / 3
    }
  )

  "a stream based on an isomorphism case factory" should "eventually cover all the inputs from the lower bound to the upper bound inclusive" in
    forAll(isomorphismCaseFactoryTable) { isomorphismCaseFactory =>
      inMockitoSession {
        val sut: Trials[Any] = api.stream(isomorphismCaseFactory)

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        val rangeOfCases = isomorphismCaseFactory
          .lowerBoundInput() to isomorphismCaseFactory
          .upperBoundInput() map isomorphismCaseFactory.apply

        rangeOfCases.foreach { expectedCase =>
          verify(mockConsumer, atLeastOnce()).apply(expectedCase)
        }
      }
    }

  it should "result in the maximally shrunk case being reported when all cases fail" in
    forAll(isomorphismCaseFactoryTable) { isomorphismCaseFactory =>
      val sut: Trials[Any] = api.stream(isomorphismCaseFactory)

      val angryConsumer: Any => Unit =
        _ => throw new RuntimeException("Disgusted, Tunbridge Wells!")

      val exception = intercept[sut.TrialException] {
        sut.withLimit(limit).supplyTo(angryConsumer)
      }

      exception.provokingCase shouldBe (isomorphismCaseFactory.apply(
        isomorphismCaseFactory.maximallyShrunkInput()
      ))
    }

  "a choice that includes exceptional cases" should "result in one of the corresponding exceptions" in {

    def testBodyInWildcardCapture[X](
        choicesAndCriterion: ChoicesAndCriterion[X]
    ) = choicesAndCriterion match {
      case ChoicesAndCriterion(possibleChoices, exceptionCriterion) =>
        val sut = api.choose(possibleChoices)

        val complainingConsumer = { (caze: X) =>
          if (exceptionCriterion(caze))
            throw ExceptionWithCasePayload(caze)
        }

        val exception = intercept[sut.TrialException] {
          sut.withLimit(limit).supplyTo(complainingConsumer)
        }

        val underlyingException = exception.getCause

        underlyingException shouldBe a[ExceptionWithCasePayload[_]]

        underlyingException match {
          case exceptionWithCasePayload: ExceptionWithCasePayload[_] =>
            exception.provokingCase should be(exceptionWithCasePayload.caze)

            exactly(1, possibleChoices) should be(
              exceptionWithCasePayload.caze
            )
        }
    }

    forAll(
      Table[ChoicesAndCriterion[_]](
        "possibleChoices -> exceptionCriterion",
        ChoicesAndCriterion(1 to 10, 0 == (_: Int) % 2),
        ChoicesAndCriterion(
          -5 to 5 map (_.toString),
          (_: String).contains("5")
        ),
        ChoicesAndCriterion(Seq(false, true), identity[Boolean] _),
        ChoicesAndCriterion(Seq(4.3), (_: Double) => true)
      )
    ) { choicesAndCriterion =>
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
        Seq(1, "3", 2 to 4),
        Seq(1 to 10, Seq(12), -3 to -1),
        Seq(Seq(0), 1 to 10, 13, -3 to -1)
      )
    ) { alternatives =>
      inMockitoSession {
        val sut: Trials[Any] =
          api.alternate(alternatives map {
            case sequence: Seq[_] => api.choose(sequence)
            case singleton        => api.only(singleton)
          })

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        alternatives
          .foreach {
            case several: Seq[_] =>
              several.foreach(possibleChoice =>
                verify(mockConsumer).apply(possibleChoice)
              )
            case singleton => verify(mockConsumer).apply(singleton)
          }
      }
    }

  it should "yield all and only the cases that would be yielded by its alternatives in the given weights" in
    forAll(
      Table(
        "alternatives",
        Seq.empty,
        Seq(1 to 10),
        Seq(1 to 10, 20 to 30 map (_.toString)),
        Seq(1 to 10, Seq(true, false), 20 to 30),
        Seq(1, "3", 99),
        Seq(1, "3", 2 to 4),
        Seq(1 to 10, Seq(12), -3 to -1),
        Seq(Seq(0), 1 to 10, 13, -3 to -1)
      )
    ) { alternatives =>
      inMockitoSession {
        val weightedAlternatives =
          alternatives.map(choice => 1 + choice.hashCode().abs % 10 -> choice)

        val sut: Trials[Any] =
          api.alternateWithWeights(weightedAlternatives map {
            case (weight, sequence: Seq[_]) => weight -> api.choose(sequence)
            case (weight, singleton)        => weight -> api.only(singleton)
          })

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        weightedAlternatives
          .foreach {
            case (weight, several: Seq[_]) =>
              several.foreach(possibleChoice =>
                verify(mockConsumer, times(weight)).apply(possibleChoice)
              )
            case (weight, singleton) =>
              verify(mockConsumer, times(weight)).apply(singleton)
          }
      }
    }

  "an alternation over streams" should "yield the cases that would be yielded by its alternatives in proportion to the given weights" in
    forAll(
      Table(
        "alternatives",
        Seq((_: Long).toString),
        Seq((_: Long).toString, identity[Long] _, (_: Long) * 2),
        Seq((_: Long).toString, identity[Long] _, (_: Long) * 2, (_: Long) * 2)
      )
    ) { alternatives =>
      inMockitoSession {
        val alternativeInvariantIds = Vector.fill(alternatives.size) {
          UUID.randomUUID()
        }

        val weightedAlternatives =
          alternatives
            .map(choice => 1 + choice.hashCode().abs % 10 -> choice)
            .toMap

        val sut: Trials[(Any, UUID)] =
          api.alternateWithWeights(
            weightedAlternatives.zip(alternativeInvariantIds) map {
              case ((weight, factory: (Long => Any)), invariantId) =>
                weight -> api.streamLegacy(factory).map(_ -> invariantId)
            }
          )

        val mockConsumer: ((Any, UUID)) => Unit =
          mock(classOf[((Any, UUID)) => Unit])

        val invariantIdCounts = mutable.Map.empty[UUID, Int]

        doAnswer(invocation =>
          invocation.getArgument[(Any, UUID)](0) match {
            case (_, invariantId) =>
              invariantIdCounts.updateWith(invariantId) {
                case count @ Some(_) => count.map(1 + _)
                case None            => Some(1)
              }: Unit
          }
        ).when(mockConsumer).apply(any(classOf[(Any, UUID)]))

        sut.withLimit(limit).supplyTo(mockConsumer)

        val totalNumberOfCalls = invariantIdCounts.values.sum

        val weights = weightedAlternatives.keys

        val totalWeight = weights.sum

        alternativeInvariantIds.zip(weights).foreach {
          case (invariantId, weight) =>
            val expectedCallCount =
              Math
                .round((totalNumberOfCalls.toDouble * weight) / totalWeight)
                .toInt
            invariantIdCounts(invariantId) should be(expectedCallCount +- 10)
        }
      }
    }

  "an alternation" should "yield cases that satisfy an invariant of one of its alternatives" in
    forAll(
      Table(
        "alternatives",
        Seq(1 to 10),
        Seq(1 to 10, 20 to 30 map (_.toString)),
        Seq(1 to 10, Seq(true, false), 20 to 30),
        Seq(1, "3", 99),
        Seq(1, "3", 2 to 4),
        Seq(1 to 10, Seq(12), -3 to -1),
        Seq(Seq(0), 1 to 10, 13, -3 to -1),
        Seq((_: Long).toString),
        Seq(Seq(0), 1 to 10, 13, identity[Long] _, -3 to -1)
      )
    ) { alternatives =>
      inMockitoSession {
        val alternativeInvariantIds = Vector.fill(alternatives.size) {
          UUID.randomUUID()
        }

        val predicateOnHash: ((Any, UUID)) => Boolean = {
          case (value: Any, _) =>
            0 == value.hashCode() % 3
        }

        val sut: Trials[(Any, UUID)] =
          api.alternate(alternatives map {
            case sequence: Seq[_] => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          } zip alternativeInvariantIds map {
            // Set up a unique invariant on each alternative - it should supply
            // pairs, each of which has the same id component that denotes the
            // alternative. As the id is unique, the implementation of
            // `Trials.alternative` cannot fake the id values - so they must
            // come from the alternatives somehow. Furthermore, the pair should
            // satisfy a predicate on the hash of its second component.
            case (trials, invariantId) =>
              trials.map(_ -> invariantId).filter(predicateOnHash)
          })

        val mockConsumer: ((Any, UUID)) => Unit =
          mock(classOf[((Any, UUID)) => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        verify(mockConsumer, atLeastOnce())
          .apply(argThat { (identifiedCase: (Any, UUID)) =>
            alternativeInvariantIds.contains(
              identifiedCase._2
            ) && predicateOnHash(
              identifiedCase
            )
          })

        verifyNoMoreInteractions(mockConsumer)
      }
    }

  "collection trials" should "yield cases whose elements satisfy the same invariants as the values yielded by the base element trials" in
    forAll(
      Table(
        "input",
        Seq.empty,
        1 to 10,
        20 to 30 map (_.toString),
        Seq(true, false),
        1,
        "3",
        99,
        Seq(12),
        Seq.empty,
        (_: Long).toString,
        identity[Long] _
      )
    ) { input =>
      inMockitoSession {
        val invariantId = UUID.randomUUID()

        def predicateOnHash(caze: Any) = 0 == caze.hashCode() % 3

        val sut: Trials[List[(Any, UUID)]] =
          (input match {
            case sequence: Seq[_] => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          }).map(
            _ ->
              // Set up an invariant - it should supply pairs, each of which has
              // the same id component. As the id is unique, the implementation
              // of `Trials.several` cannot fake the id values - so they must
              // come from the base trials somehow. Furthermore, the pair should
              // satisfy a predicate on its hash.
              invariantId
          ).filter(predicateOnHash)
            .several

        val mockConsumer: (List[(Any, UUID)]) => Unit =
          mock(classOf[List[(Any, UUID)] => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        verify(
          mockConsumer,
          atLeastOnce()
        ).apply(argThat {
          (_: List[(Any, UUID)]).forall(identifiedCase =>
            invariantId == identifiedCase._2 && predicateOnHash(
              identifiedCase
            )
          )
        })
      }
    }

  they should "yield members of the Cartesian product when the base elements trials are finite choices" in
    forAll(
      Table(
        "input",
        1 to 10,
        20 to 30 map (_.toString),
        Seq(true, false),
        1,
        "3",
        99,
        Seq(12),
        Seq.empty
      )
    ) { input =>
      inMockitoSession {
        val maximumLengthOfACartesianProductMember = 3

        def cartesianProductSizeLimitation(caze: List[Any]) =
          maximumLengthOfACartesianProductMember >= caze.size

        val sut: Trials[List[Any]] =
          (input match {
            case sequence: Seq[_] => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          }).several[List[_]]
            .filter(cartesianProductSizeLimitation)

        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        val elements = input match {
          case sequence: Seq[Any] =>
            sequence.toSet
          case singleton => Set(singleton)
        }

        val cartesianProductMembers: Set[List[Any]] = if (elements.nonEmpty) {
          def cartesianProduct(
              subProducts: LazyList[List[Any]]
          ): LazyList[List[Any]] = subProducts.lazyAppendedAll(
            cartesianProduct(
              subProducts.flatMap(subProduct => elements.map(_ :: subProduct))
            )
          )

          cartesianProduct(LazyList(Nil))
            .takeWhile(cartesianProductSizeLimitation)
            .toSet
        } else Set(Nil)

        val limit = 1500

        sut.withLimit(limit).supplyTo(mockConsumer)

        cartesianProductMembers.foreach(product =>
          verify(mockConsumer).apply(product)
        )
      }
    }

  "sized collection trials" should "yield cases even when the size is large" in
    forAll(
      Table(
        "input" -> "largeSize",
        // Slip in the empty and singleton cases too...
        (0 to 5)                    -> 0,
        listTrials                  -> 1,
        (1 to 10)                   -> 1000,
        (20 to 30 map (_.toString)) -> 10000,
        (Seq(true, false))          -> 30000,
        1                           -> 1000,
        "3"                         -> 10000,
        99                          -> 30000,
        Seq(12)                     -> 1000,
        ((_: Long).toString)        -> 20000,
        identity[Long] _            -> 1000,
        listTrials                  -> 30000,
        bushyTreeTrials             -> 2000
      )
    ) { (input, largeSize) =>
      inMockitoSession {
        println(s"largeSize: $largeSize")

        val sut: Trials[List[Any]] =
          (input match {
            case trials: Trials[_] => trials
            case sequence: Seq[_]  => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          }).lotsOfSize(largeSize)

        val mockConsumer: List[Any] => Unit = mock(classOf[List[Any] => Unit])

        sut.withLimit(1).supplyTo(mockConsumer)

        verify(mockConsumer).apply(argThat(largeSize == (_: List[Any]).size))

        verifyNoMoreInteractions(mockConsumer)
      }
    }

  "trials" should "yield repeatable cases" in
    forAll(
      Table(
        "trials",
        api.only(1),
        api.choose(1, false, 99),
        api.alternate(
          api.choose(0 until 10 map (_.toString)),
          api.choose(-10 until 0)
        ),
        api.streamLegacy(_ * 1.46),
        api.alternate(
          api.streamLegacy(_ * 1.46),
          api.choose(0 until 10 map (_.toString)),
          api.choose(-10 until 0)
        ),
        implicitly[Factory[Option[Int]]].trials
      )
    ) { sut =>
      inMockitoSession {
        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        // Whatever cases are supplied set the expectations...
        sut
          .withLimit(limit)
          .supplyTo(expectedCase =>
            doReturn(()).when(mockConsumer).apply(expectedCase)
          )

        // ... now let's see if we see the same cases.
        sut.withLimit(limit).supplyTo(mockConsumer)

        verifyNoMoreInteractions(mockConsumer)
      }
    }

  they should "not invoke stoppage if no failure is found" in {
    inMockitoSession {
      val sut = api.only(())

      def explodingStoppage(): Any => Boolean = {
        val mockPredicate: Any => Boolean = mock(classOf[Any => Boolean])

        doAnswer(_ => fail("The stoppage should not have been invoked."))
          .when(mockPredicate)
          .apply(any())

        mockPredicate
      }

      sut.withLimits(1, shrinkageStop = explodingStoppage).supplyTo { _ => }
    }
  }

  they should "produce no more than the limiting number of cases" in
    forAll(
      Table(
        "trials"                 -> "limit",
        api.only(1)              -> 1,
        api.choose(1, false, 99) -> 3,
        api.alternate(
          api.choose(0 until 10 map (_.toString)),
          api.choose(-10 until 0)
        )                                             -> 4,
        api.streamLegacy(identity)                    -> 200,
        implicitly[Factory[Either[Long, Int]]].trials -> 500
      )
    ) { (sut, limit) =>
      inMockitoSession {
        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        verify(mockConsumer, mockitoAtMost(limit)).apply(any())
      }
    }

  they should "produce the limiting number of cases if feasible" in
    forAll(
      Table(
        "trials"                 -> "limit",
        api.only(1)              -> 1,
        api.choose(1, false, 99) -> 3,
        api.alternate(
          api.choose(0 until 10 map (_.toString)),
          api.choose(-10 until 0)
        )                                                    -> 20,
        implicitly[Factory[Either[Boolean, Boolean]]].trials -> 4,
        api
          .choose(1 to 3)
          .flatMap(x => api.choose(1 to 10).map(x -> _))
          .filter { case (x, y) => 0 == (x * y) % 3 } -> (1 * 7 + 2 * 3 + 1 * 3)
      )
    ) { (sut, limit) =>
      inMockitoSession {
        val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

        sut.withLimit(limit).supplyTo(mockConsumer)

        verify(mockConsumer, times(limit)).apply(any())
      }
    }

  they should "yield repeatable exceptions" in
    forAll(
      Table(
        "trials",
        api.only(JackInABox(1)),
        api.choose(1, false, JackInABox(99)),
        api.alternate(
          api.only(true),
          api.choose(0 until 10 map (_.toString) map JackInABox.apply),
          api.choose(-10 until 0)
        ),
        api.streamLegacy({
          case value if 0 == value % 3 => JackInABox(value)
          case value => value
        }),
        api.alternate(
          api.only(true),
          api.choose(-10 until 0),
          api.streamLegacy(JackInABox.apply)
        ),
        implicitly[Factory[Option[Int]]].trials.map {
          case None        => JackInABox(())
          case Some(value) => value
        }
      )
    ) { sut =>
      val surprisedConsumer: Any => Unit = {
        case JackInABox(caze) => throw ExceptionWithCasePayload(caze)
        case _                =>
      }

      val exception = Try {
        sut.withLimit(limit).supplyTo(surprisedConsumer)
      }.failed.get
        .asInstanceOf[sut.TrialException]

      val exceptionFromSecondAttempt = Try {
        sut.withLimit(limit).supplyTo(surprisedConsumer)
      }.failed.get
        .asInstanceOf[sut.TrialException]

      exceptionFromSecondAttempt.provokingCase shouldBe exception.provokingCase
    }

  "an exceptional case" should "be reproduced via its recipe" in forAll(
    Table(
      "trials",
      api.only(JackInABox(1)),
      api.choose(1, false, JackInABox(99)),
      api.alternate(
        api.only(true),
        api.choose(0 until 10 map (_.toString) map JackInABox.apply),
        api.choose(-10 until 0)
      ),
      api.alternate(
        api.only(true),
        api.choose(-10 until 0),
        api.alternate(api.choose(-99 to -50), api.only(JackInABox(-2)))
      ),
      api.alternate(
        api.only(true),
        api.alternate(
          api.choose(-99 to -50),
          api.choose("Red herring", false, JackInABox(-2))
        ),
        api.choose(-10 until 0)
      ),
      api.streamLegacy({
        case value if 0 == value % 3 => JackInABox(value)
        case value => value
      }),
      api.alternate(
        api.only(true),
        api.streamLegacy({
          case value if 0 == value % 3 => JackInABox(value)
          case value => value
        }),
        api.choose(-10 until 0)
      ),
      implicitly[Factory[Option[Int]]].trials.map {
        case None        => JackInABox(())
        case Some(value) => value
      }
    )
  ) { sut =>
    val surprisedConsumer: Any => Unit = {
      case JackInABox(caze) => throw ExceptionWithCasePayload(caze)
      case _                =>
    }

    val exception = intercept[sut.TrialException](
      sut.withLimit(limit).supplyTo(surprisedConsumer)
    )

    val exceptionRecreatedViaRecipe = intercept[sut.TrialException](
      sut.withLimit(limit).supplyTo(surprisedConsumer)
    )

    exceptionRecreatedViaRecipe.provokingCase shouldBe exception.provokingCase
    exceptionRecreatedViaRecipe.recipe shouldBe exception.recipe
    exceptionRecreatedViaRecipe.recipeHash shouldBe exception.recipeHash
  }

  case class DescriptionTrialsCriterionAndLimit[X](
      description: String,
      sut: Trials[Vector[X]],
      exceptionCriterion: Vector[X] => Boolean,
      limit: Int
  )

  it should "be shrunk to a simple case" in {
    def testBodyInWildcardCapture[X](
        trialsAndCriterion: DescriptionTrialsCriterionAndLimit[X]
    ): Unit = trialsAndCriterion match {
      case DescriptionTrialsCriterionAndLimit(
            description,
            sut,
            exceptionCriterion,
            limit
          ) =>
        val complainingConsumer = { (caze: Vector[X]) =>
          if (exceptionCriterion(caze))
            throw ExceptionWithCasePayload(caze)
        }

        val exception = intercept[sut.TrialException] {
          sut.withLimit(limit).supplyTo(complainingConsumer)
        }

        val underlyingException = exception.getCause

        underlyingException shouldBe a[ExceptionWithCasePayload[_]]

        underlyingException match {
          case exceptionWithCasePayload: ExceptionWithCasePayload[
                Vector[X]
              ] =>
            val provokingCase = exception.provokingCase

            println(s"Provoking case for '$description': $provokingCase")

            provokingCase should be(exceptionWithCasePayload.caze)

            val sizeOfProvokingCase = provokingCase.size

            try
              sut
                .filter(_ != provokingCase)
                .withLimit(limit)
                .supplyTo(complainingConsumer)
            catch {
              case exceptionFromFilteredTrials: Throwable =>
                exceptionFromFilteredTrials.getCause match {
                  case exceptionWithAtLeastAsLargeCasePayload: ExceptionWithCasePayload[
                        Vector[X]
                      ] =>
                    exceptionWithAtLeastAsLargeCasePayload.caze.size should be >= sizeOfProvokingCase
                }
            }

            0 until sizeOfProvokingCase foreach { excisionIndex =>
              val smallerCase =
                provokingCase.patch(excisionIndex, Vector.empty, 1)

              noException should be thrownBy complainingConsumer(
                smallerCase
              )
            }
        }
    }

    forAll(
      Table[DescriptionTrialsCriterionAndLimit[_]](
        "(description, trials, exceptionCriterion)",
        DescriptionTrialsCriterionAndLimit(
          // This first entry isn't expected to shrink the values, only the
          // length of the failing case.
          "Has more than one text item whose converted values sum to more than 7.",
          api.strings map (_.toVector) map (_.map(_.toInt)),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has either four or five characters.",
          api.characters.several[Vector[Char]],
          (characterVector: Vector[Char]) =>
            4 to 5 contains characterVector.size,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has either four or five characters - variation.",
          api
            .integers(4, 10)
            .flatMap(api.characters.lotsOfSize[Vector[Char]](_)),
          (characterVector: Vector[Char]) =>
            4 to 5 contains characterVector.size,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has either four or five characters - variation with shrinkable character range.",
          api
            .integers(4, 10)
            .flatMap(api.characters('a', 'z', 'q').lotsOfSize[Vector[Char]](_)),
          (characterVector: Vector[Char]) =>
            4 to 5 contains characterVector.size,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has either four or five characters - this used to be a pathologically slow example.",
          api
            .integers(0, 10)
            .flatMap(api.characters.lotsOfSize[Vector[Char]](_)),
          (characterVector: Vector[Char]) =>
            4 to 5 contains characterVector.size,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item and sums to more than 7.",
          doubleVectorTrials,
          (doubleVector: Vector[Double]) =>
            1 < doubleVector.size && doubleVector.sum > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item and sums to more than 7.",
          byteVectorTrials,
          (integerVector: Vector[Byte]) =>
            1 < integerVector.size && integerVector.sum > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, no zeroes and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7 && !integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, at least one zero and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7 && integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, no zeroes and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7 && !longVector.contains(
              0
            ),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, at least one zero and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7 && longVector.contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than 7 items.",
          integerVectorTrials,
          (integerVector: Vector[Int]) => integerVector.size > 7,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, at least one non-zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .exists(0 != _),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, no zeroes and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && !integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, at least one zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, no zeroes and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum && !integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than one item, at least one non-zero and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum && integerVector
              .exists(0 != _),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Flattened binary tree with more than one leaf that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Flattened binary tree with more than one leaf and no zeroes that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum && !integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Flattened binary tree with more than one leaf and at least one zero that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum && integerVector
              .contains(0),
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than five items, at least one non-zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            5 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .exists(0 != _),
          500
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than five items and sums to a multiple of 7 less than -7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            5 < integerVector.size && 0 == integerVector.sum % 7 && -7 > integerVector.sum && integerVector
              .exists(0 != _),
          750
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than two items and is not sorted in ascending order.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && integerVector
              .zip(integerVector.tail)
              .exists { case (first, second) => first > second },
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Has more than two items and no duplicates.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && integerVector.distinct == integerVector,
          limit
        ),
        DescriptionTrialsCriterionAndLimit(
          "Flattened binary tree with more than two leaves whose odd-indexed leaves contain zeroes and even-indexed leaves contain non-zero values that sum to a multiple of 31 greater than 31.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && 0 == integerVector.sum % 31 && 31 < integerVector.sum && (0 until integerVector.size forall (
              index => 0 == index % 2 ^ 0 == integerVector(index)
            )),
          5000
        ),
        DescriptionTrialsCriterionAndLimit(
          "List with more than two elements whose odd-indexed elements contain zeroes and even-indexed elements contain non-zero values that sum to a multiple of 31 greater than 31.",
          listTrials map (_.toVector),
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && 0 == integerVector.sum % 31 && 31 < integerVector.sum && (0 until integerVector.size forall (
              index => 0 == index % 2 ^ 0 == integerVector(index)
            )),
          5000
        )
      )
    ) { trialsAndCriterion =>
      testBodyInWildcardCapture(trialsAndCriterion)
    }
  }

  it should "have its shrinkage stopped by a stopping condition" in {
    def shouldProvokeFailure(caze: Long): Boolean = {
      1 == caze % 2
    }

    class FailureCounter {
      var numberOfFailures: Int = 0

      def consume(caze: Long): Unit = {
        if (shouldProvokeFailure(caze)) {
          numberOfFailures += 1
          throw ExceptionWithCasePayload(caze)
        }
      }
    }

    val failureCounterWithNoStoppingCondition = new FailureCounter

    val sut = api.longs

    val shrunkCase = intercept[sut.TrialException](
      sut
        .withLimits(limit, shrinkageStop = TrialsScaffolding.noStopping)
        .supplyTo(failureCounterWithNoStoppingCondition.consume)
    ).getCause match {
      case exception: ExceptionWithCasePayload[Long] => exception.caze
    }

    val halfTheFailuresSeen =
      failureCounterWithNoStoppingCondition.numberOfFailures / 2

    // This might fail, but not due to what we are testing for, so it is
    // expressed as a self-check of the test logic.
    assume(0 < halfTheFailuresSeen)

    def shrinkageStop(): Long => Boolean = {
      var countDown = halfTheFailuresSeen

      (caze: Long) =>
        shouldProvokeFailure(caze) should be(true)
        caze should be > shrunkCase

        if (0 == countDown) true
        else {
          countDown -= 1
          false
        }
    }

    val failureCounterWithStoppingCondition = new FailureCounter

    val shrunkCaseWithStoppage = intercept[sut.TrialException](
      sut
        .withLimits(limit, shrinkageStop = shrinkageStop)
        .supplyTo(failureCounterWithStoppingCondition.consume _)
    ).getCause match {
      case exception: ExceptionWithCasePayload[Long] => exception.caze
    }

    shrunkCaseWithStoppage should be > shrunkCase

    failureCounterWithStoppingCondition.numberOfFailures should (be > 1 and be < failureCounterWithNoStoppingCondition.numberOfFailures)

    val failureCounterWithoutAnyShrinkage = new FailureCounter

    val shrunkCaseWithoutAnyShrinkage = intercept[sut.TrialException](
      sut
        .withLimits(limit, shrinkageStop = noShrinking)
        .supplyTo(failureCounterWithoutAnyShrinkage.consume _)
    ).getCause match {
      case exception: ExceptionWithCasePayload[Long] => exception.caze
    }

    shrunkCaseWithoutAnyShrinkage should be > shrunkCaseWithStoppage

    failureCounterWithoutAnyShrinkage.numberOfFailures should be(1)
  }

  "test driving a trials for a recursive data structure" should "not produce smoke" in {
    listTrials
      .withLimit(limit)
      .supplyTo(println)

    binaryTreeTrials
      .withLimit(limit)
      .supplyTo(println)

    bushyTreeTrials
      .withLimit(limit)
      .supplyTo(println)
  }

  "test driving automatic implicit generation of a trials" should "not produce smoke" in {
    implicitly[Factory[Option[Int]]].trials
      .withLimit(limit)
      .supplyTo(println)

    implicitly[Factory[Either[(Boolean, Boolean), Double]]].trials
      .withLimit(limit)
      .supplyTo(println)
  }

  "inlined filtration" should "execute the controlled block if and only if the precondition holds" in {
    Trials.whenever(satisfiedPrecondition = false) {
      fail(
        "If the precondition doesn't hold, the block should not be executed."
      )
    }

    Trials.whenever(satisfiedPrecondition = true) {}
  }

  private val oddHash = 1 == (_: Any).hashCode % 2

  it should "cover all the cases that would be covered by an explicit filtration over finite possibilities" in forAll(
    Table(
      "trials",
      api.only(2),
      api.only(15),
      api.choose(0 until 20),
      api.alternate(
        api.only(99),
        api.choose(0 until 20),
        api.only(127)
      ),
      api.choose(1 until 20).flatMap(x => api.choose(1, 3).map(_ + 4 * x))
    )
  ) { trials =>
    inMockitoSession {
      val mockConsumer: Int => Unit = mock(classOf[Int => Unit])

      // Whatever cases are supplied by a monadic filtration set the
      // expectations...
      trials
        .filter(oddHash)
        .withLimit(limit)
        .supplyTo(expectedCase =>
          doNothing().when(mockConsumer).apply(expectedCase)
        )

      // ... now let's see if we see the same cases when we filter inline.
      trials
        .withLimit(limit)
        .supplyTo(caze =>
          Trials.whenever(oddHash(caze)) {
            mockConsumer(caze)
          }
        )

      verifyNoMoreInteractions(mockConsumer)
    }
  }

  it should "cover the same number of cases that would be covered by an explicit filtration over infinite possibilities" in forAll(
    Table(
      "trials",
      api.integers,
      api.alternate(
        api.integers,
        api.only(1),
        api.choose(0 until 20)
      ),
      api.integers.flatMap(x => api.choose(1, 3).map(x * _))
    )
  ) { trials =>
    inMockitoSession {
      // Count the cases supplied by a monadic filtration...

      val numberOfCasesViaMonadicFiltration = {
        var count = 0

        trials
          .filter(oddHash)
          .withLimit(limit)
          .supplyTo { _ => count = 1 + count }

        count
      }

      val mockConsumer: Int => Unit = mock(classOf[Int => Unit])

      // ... now let's see if we receive *exactly* the same number of cases when
      // we filter inline.

      trials
        .withLimit(limit)
        .supplyTo(caze =>
          Trials.whenever(oddHash(caze)) {
            mockConsumer(caze)
          }
        )

      verify(mockConsumer, times(numberOfCasesViaMonadicFiltration)).apply(
        any()
      )
    }
  }

  it should "be minimise failures to the same failing case as via explicit filtration - Scala" in {
    val sets: Trials[Set[_ <: Int]] = api.integers.sets

    def predicate(set: Set[_ <: Int]): Boolean = 0 == set.hashCode() % 2

    val trouble = new RuntimeException("Trouble!")

    def troublesomeConsumer(set: Set[_ <: Int]): Unit =
      if (1 == set.size % 3) {
        println(set)
        throw trouble
      }

    val explicitlyFiltered = sets.filter(predicate)

    val exceptionViaExplicitFiltration =
      intercept[explicitlyFiltered.TrialException](
        explicitlyFiltered.withLimit(limit).supplyTo(troublesomeConsumer)
      )

    val exceptionViaInlinedFiltration = intercept[sets.TrialException](
      sets
        .withLimit(limit)
        .supplyTo(caze =>
          Trials.whenever(predicate(caze)) {
            troublesomeConsumer(caze)
          }
        )
    )

    exceptionViaInlinedFiltration.getCause should be(
      exceptionViaExplicitFiltration.getCause
    )

    exceptionViaInlinedFiltration.provokingCase should be(
      exceptionViaExplicitFiltration.provokingCase
    )
  }

  "mapping using a Java function" should "compile" in {
    assertCompiles("javaApi.only(1).map((_ + 1): JavaFunction[Int, Int])")
  }

  "mapping using a Scala function" should "compile" in {
    assertCompiles("api.only(1).map(_ + 1)")
  }

  "flatmapping using a Java function" should "compile" in {
    assertCompiles(
      "javaApi.only(1).flatMap(value => javaApi.choose(value, 1.0 + value))"
    )
  }

  "flatmapping using a Scala function" should "compile" in {
    assertCompiles(
      "api.only(1).flatMap(value => api.choose(value, 1.0 + value))"
    )
  }

  "filtering using a Java predicate" should "compile" in {
    assertCompiles("javaApi.only(1).filter(1 == _)")
  }

  "filtering using a Scala function" should "compile" in {
    assertCompiles("api.only(1).filter(1 == _)")
  }

  "this rho problem" should "be minimised" in {
    // Inspired by:
    // https://buttondown.email/hillelwayne/archive/fd1f0758-ae31-4e83-9138-33721cbd5ce3
    // and https://notebook.drmaciver.com/posts/2020-12-28-16:19.html

    // The following predicate is based on this Python diff:
    // format:off
    //    def f(x, y, z):
    //      out = 0
    //      for i in range(10):
    //        out = out * x + abs(y*z - i**2)
    //        x, y, z = y+1, z, x
    //    - return abs(out)%100 < 10
    //    + return abs(out)%100 < 9
    // format:on

    def predicate(
        threshold: BigInt
    )(x: BigInt, y: BigInt, z: BigInt): Boolean = {
      val (out, _, _, _) = (0 until 10).foldLeft((BigInt(0L), x, y, z)) {
        case ((partial, x, y, z), i) =>
          (partial * x + (y * z - BigInt(i).pow(2)).abs, y + 1, z, x)
      }
      out.abs % 100 < threshold
    }

    val suts = api.longs and api.longs and api.longs

    val trialException = {
      // NOTE: have to crank the limit up to 50 to get such a nice minimisation,
      // although 40 yields a pretty decent result too.
      intercept[suts.TrialException](suts.withLimit(50).supplyTo {
        case (x, y, z) =>
          predicate(10)(x, y, z) shouldEqual predicate(9)(x, y, z)
      })
    }

    println(trialException.provokingCase)
  }

  "combination with Scala `.or`" should "cover alternate finite choices" in {
    inMockitoSession {
      val mockConsumer: Either[String, Int] => Unit =
        mock(classOf[Either[String, Int] => Unit])

      val lefts: Trials[String] = api.choose("Huey", "Duey", "Louie")

      val rights: Trials[Int] = api.choose(0 until 10)

      lefts
        .map(Left.apply)
        .withLimit(limit)
        .supplyTo(left => doReturn(()).when(mockConsumer).apply(left))

      rights
        .map(Right.apply)
        .withLimit(limit)
        .supplyTo(right => doReturn(()).when(mockConsumer).apply(right))

      (lefts or rights)
        .withLimit(limit)
        .supplyTo(mockConsumer)

      verifyNoMoreInteractions(mockConsumer)
    }
  }

  "combination with Java `.or`" should "cover alternate finite choices" in {
    inMockitoSession {
      val mockConsumer: Consumer[JavaEither[String, Int]] =
        mock(classOf[Consumer[JavaEither[String, Int]]])

      val lefts: JavaTrials[String] = javaApi.choose("Huey", "Duey", "Louie")

      val rights: JavaTrials[Int] = javaApi.choose(
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

  "lifting with Scala `.options`" should "cover underlying finite choices and include `None`" in {
    inMockitoSession {
      val mockConsumer: Option[Int] => Unit =
        mock(classOf[Option[Int] => Unit])

      val underlyings: Trials[Int] = api.choose(0 to 10)

      underlyings
        .withLimit(limit)
        .supplyTo(underlyingCase =>
          doReturn(()).when(mockConsumer).apply(Some(underlyingCase))
        )

      doReturn(()).when(mockConsumer).apply(None)

      underlyings.options.withLimit(limit).supplyTo(mockConsumer)

      verifyNoMoreInteractions(mockConsumer)
    }
  }

  "lifting with Java `.optionals`" should "cover underlying finite choices and include `None`" in {
    inMockitoSession {
      val mockConsumer: Consumer[Optional[Int]] =
        mock(classOf[Consumer[Optional[Int]]])

      val underlyings: JavaTrials[Int] =
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
}

class TrialsSpecInQuarantineDueToUseOfSystemProperty
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks
    with MockitoSessionSupport {
  import TrialsSpec.*

  it should "be reproduced by its recipe hash" in forAll(
    Table(
      "trials",
      api.only(JackInABox(1)),
      api.choose(1, false, JackInABox(99)),
      api.alternate(
        api.only(true),
        api.choose(0 until 10 map (_.toString) map JackInABox.apply),
        api.choose(-10 until 0)
      ),
      api.alternate(
        api.only(true),
        api.choose(-10 until 0),
        api.alternate(api.choose(-99 to -50), api.only(JackInABox(-2)))
      ),
      api.alternate(
        api.only(true),
        api.alternate(
          api.choose(-99 to -50),
          api.choose("Red herring", false, JackInABox(-2))
        ),
        api.choose(-10 until 0)
      ),
      api.streamLegacy({
        case value if 0 == value % 3 => JackInABox(value)
        case value => value
      }),
      api.alternate(
        api.only(true),
        api.streamLegacy({
          case value if 0 == value % 3 => JackInABox(value)
          case value => value
        }),
        api.choose(-10 until 0)
      ),
      implicitly[Factory[Option[Int]]].trials.map {
        case None        => JackInABox(())
        case Some(value) => value
      }
    )
  ) { sut =>
    inMockitoSession {
      val surprisedConsumer: Any => Unit = {
        case JackInABox(caze) => throw ExceptionWithCasePayload(caze)
        case _                =>
      }

      val exception = intercept[sut.TrialException](
        sut.withLimit(limit).supplyTo(surprisedConsumer)
      )

      val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

      doAnswer(invocation =>
        throw ExceptionWithCasePayload(
          invocation.getArgument[JackInABox[_]](0).caze
        )
      ).when(mockConsumer).apply(any[JackInABox[_]]())

      val exceptionRecreatedViaRecipeHash = {
        val previousPropertyValue =
          Option(
            System.setProperty(recipeHashJavaPropertyName, exception.recipeHash)
          )

        try {
          intercept[sut.TrialException](
            sut.withLimit(limit).supplyTo(mockConsumer)
          )
        } finally {
          previousPropertyValue.fold(ifEmpty =
            System.clearProperty(recipeHashJavaPropertyName)
          )(
            System.setProperty(recipeHashJavaPropertyName, _)
          )
        }
      }

      exceptionRecreatedViaRecipeHash.provokingCase shouldBe exception.provokingCase
      exceptionRecreatedViaRecipeHash.recipe shouldBe exception.recipe
      exceptionRecreatedViaRecipeHash.recipeHash shouldBe exception.recipeHash

      verify(mockConsumer).apply(any())
    }
  }
}

class TrialsSpecInQuarantineDueToTheTestBeingLongRunning
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks
    with MockitoSessionSupport {
  import TrialsSpec.*

  "lots of cases" should "be possible" in forAll(
    Table(
      "factory"    -> "limit",
      api.integers -> 10000,
      api.doubles  -> 10000,
      api.stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = Long.MinValue
        override def upperBoundInput(): Long      = Long.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })           -> 10000,
      api.integers -> 100000,
      api.doubles  -> 100000,
      api.stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = Long.MinValue
        override def upperBoundInput(): Long      = Long.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      })           -> 100000,
      api.integers -> 500000,
      api.doubles  -> 500000,
      api.stream(new CaseFactory[Long] {
        override def apply(input: Long): Long     = input
        override def lowerBoundInput(): Long      = Long.MinValue
        override def upperBoundInput(): Long      = Long.MaxValue
        override def maximallyShrunkInput(): Long = 0L
      }) -> 500000
    )
  ) { case (factory, limit) =>
    inMockitoSession {
      val mockConsumer: Any => Unit = mock(classOf[Any => Unit])

      factory.withLimit(limit).supplyTo(mockConsumer)

      verify(mockConsumer, times(limit)).apply(any())
    }
  }
}
