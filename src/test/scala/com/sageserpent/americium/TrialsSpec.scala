package com.sageserpent.americium

import com.sageserpent.americium.Trials.RejectionByInlineFilter
import com.sageserpent.americium.java.{
  Trials => JavaTrials,
  TrialsApi => JavaTrialsApi
}
import org.scalamock.function.StubFunction1
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import _root_.java.util.UUID
import _root_.java.util.function.{Predicate, Function => JavaFunction}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Try

object TrialsSpec {
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

  val limit: Int = 3500

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
    api.alternateWithWeights(5 -> api.only(0), 10 -> api.integers).several

  def binaryTreeTrials: Trials[BinaryTree] = api.alternate(
    for {
      leftSubtree  <- api.delay(binaryTreeTrials)
      flag         <- api.booleans
      rightSubtree <- binaryTreeTrials
    } yield Branch(leftSubtree, flag, rightSubtree),
    // FIXME: the need to do this shows that some kind of weighted
    // distribution is a good idea.
    api
      .alternateWithWeights(5 -> api.only(0), 10 -> api.integers)
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
    with MockFactory
    with TableDrivenPropertyChecks {
  import TrialsSpec._

  autoVerify = false
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
  }

  "only one case" should "yield just one trial" in
    forAll(Table("case", 1, "foo", 2.3, List(false, 0, true))) { dataCase =>
      withExpectations {
        val sut = api.only(dataCase)

        val mockConsumer: StubFunction1[Any, Unit] = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        mockConsumer.verify(dataCase)
      }
    }

  "only one case that provokes an exception" should "result in an exception that references it" in
    forAll(Table("case", 1, "foo", 2.3, Seq(false, 0, true))) { dataCase =>
      withExpectations {
        val sut = api.only(dataCase)

        val problem = new RuntimeException("Test problem")

        val mockConsumer = stubFunction[Any, Unit]

        mockConsumer.when(dataCase).throwing(problem)

        val exception = intercept[sut.TrialException] {
          sut.withLimit(limit).supplyTo(mockConsumer)
        }

        exception.getCause should be(problem)
        exception.provokingCase should be(dataCase)
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
      withExpectations {
        val sut: Trials[Any] = api.choose(possibleChoices)

        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        possibleChoices.foreach(possibleChoice =>
          mockConsumer.verify(possibleChoice)
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
      withExpectations {
        val weightedChoices =
          possibleChoices.map(choice =>
            1 + choice.hashCode().abs % 10 -> choice
          )

        val sut: Trials[Any] = api.chooseWithWeights(weightedChoices)

        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        weightedChoices.foreach { case (weight, possibleChoice) =>
          mockConsumer.verify(possibleChoice).repeat(weight)
        }
      }
    }

  case class ExceptionWithCasePayload[Case](caze: Case) extends RuntimeException

  "a choice that includes exceptional cases" should "result in one of the corresponding exceptions" in {
    type ChoicesAndCriterion[X] = (Seq[X], X => Boolean)

    def testBodyInWildcardCapture[X](
        choicesAndCriterion: ChoicesAndCriterion[X]
    ) =
      withExpectations {
        choicesAndCriterion match {
          case (possibleChoices, exceptionCriterion) =>
            val sut = api.choose(possibleChoices)

            val complainingConsumer = { caze: X =>
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
      }

    forAll(
      Table[ChoicesAndCriterion[_]](
        "possibleChoices -> exceptionCriterion",
        (1 to 10, 0 == (_: Int) % 2),
        (-5 to 5 map (_.toString), (_: String).contains("5")),
        (Seq(false, true), identity[Boolean] _),
        (Seq(4.3), (_: Double) => true)
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
      withExpectations {
        val sut: Trials[Any] =
          api.alternate(alternatives map {
            case sequence: Seq[_] => api.choose(sequence)
            case singleton        => api.only(singleton)
          })

        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        alternatives
          .foreach {
            case several: Seq[_] =>
              several.foreach(possibleChoice =>
                mockConsumer.verify(possibleChoice)
              )
            case singleton => mockConsumer.verify(singleton)
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
      withExpectations {
        val weightedAlternatives =
          alternatives.map(choice => 1 + choice.hashCode().abs % 10 -> choice)

        val sut: Trials[Any] =
          api.alternateWithWeights(weightedAlternatives map {
            case (weight, sequence: Seq[_]) => weight -> api.choose(sequence)
            case (weight, singleton)        => weight -> api.only(singleton)
          })

        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        weightedAlternatives
          .foreach {
            case (weight, several: Seq[_]) =>
              several.foreach(possibleChoice =>
                mockConsumer.verify(possibleChoice).repeat(weight)
              )
            case (weight, singleton) =>
              mockConsumer.verify(singleton).repeat(weight)
          }
      }
    }

  "an alternation over streams" should "yield the cases that would be yielded by its alternatives in proportion to the given weights" in
    forAll(
      Table(
        "alternatives",
        Seq.empty,
        Seq((_: Long).toString),
        Seq((_: Long).toString, identity[Long] _, (_: Long) * 2),
        Seq((_: Long).toString, identity[Long] _, (_: Long) * 2, (_: Long) * 2)
      )
    ) { alternatives =>
      withExpectations {
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

        val mockConsumer = mockFunction[(Any, UUID), Unit]

        val invariantIdCounts = mutable.Map.empty[UUID, Int]

        mockConsumer.stubs(*) onCall ({ case (_, invariantId) =>
          invariantIdCounts.updateWith(invariantId) {
            case count @ Some(_) => count.map(1 + _)
            case None            => Some(1)
          }
        }: ((Any, UUID)) => Unit)

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
        Seq.empty,
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
      withExpectations {
        val alternativeInvariantIds = Vector.fill(alternatives.size) {
          UUID.randomUUID()
        }

        def predicateOnHash(caze: Any) = 0 == caze.hashCode() % 3

        val sut: Trials[(Any, UUID)] =
          api.alternate(alternatives map {
            case sequence: Seq[_] => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          } zip alternativeInvariantIds map {
            // Set up a unique invariant on each alternative - it should supply
            // pairs,
            // each of which has the same id component that denotes the
            // alternative. As
            // the id is unique, the implementation of `Trials.alternative`
            // cannot fake
            // the id values - so they must come from the alternatives somehow.
            // Furthermore,
            // the pair should satisfy a predicate on its hash.
            case (trials, invariantId) =>
              trials.map(_ -> invariantId).filter(predicateOnHash)
          })

        val mockConsumer = mockFunction[(Any, UUID), Unit]

        mockConsumer
          .expects(where { identifiedCase: (Any, UUID) =>
            alternativeInvariantIds.contains(
              identifiedCase._2
            ) && predicateOnHash(
              identifiedCase
            )
          })
          .anyNumberOfTimes()

        sut.withLimit(limit).supplyTo(mockConsumer)
      }
    }

  "collection trials" should "yield cases whose elements satisfy the same invariants as the values yielded by the base element trials" in
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
        Seq.empty,
        (_: Long).toString,
        identity[Long] _
      )
    ) { input =>
      withExpectations {
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
              // come
              // from the base trials somehow. Furthermore, the pair should
              // satisfy
              // a predicate on its hash.
              invariantId
          ).filter(predicateOnHash)
            .several

        val mockConsumer = mockFunction[List[(Any, UUID)], Unit]

        mockConsumer
          .expects(where {
            (_: List[(Any, UUID)]).forall(identifiedCase =>
              invariantId == identifiedCase._2 && predicateOnHash(
                identifiedCase
              )
            )
          })
          .anyNumberOfTimes()

        sut.withLimit(limit).supplyTo(mockConsumer)
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
      withExpectations {
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

        val mockConsumer = mockFunction[Any, Unit]

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

        cartesianProductMembers.foreach(product =>
          mockConsumer.expects(product)
        )

        sut.withLimit(limit).supplyTo(mockConsumer)
      }
    }

  "sized collection trials" should "yield cases even when the size is large" in
    forAll(
      Table(
        "input"                     -> "largeSize",
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
      withExpectations {
        println(s"largeSize: $largeSize")

        val sut: Trials[List[Any]] =
          (input match {
            case trials: Trials[_] => trials
            case sequence: Seq[_]  => api.choose(sequence)
            case factory: (Long => Any) =>
              api.streamLegacy(factory)
            case singleton => api.only(singleton)
          }).lotsOfSize(largeSize)

        val mockConsumer = mockFunction[List[Any], Unit]

        mockConsumer
          .expects(where(largeSize == (_: List[Any]).size))
          .atLeastOnce()
          .onCall((caze: List[Any]) => println(caze))

        sut.withLimit(1).supplyTo(mockConsumer)
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
        implicitly[Trials.Factory[Option[Int]]].trials
      )
    ) { sut =>
      withExpectations {
        val mockConsumer = mockFunction[Any, Unit]

        // Whatever cases are supplied set the expectations...
        sut.withLimit(limit).supplyTo(mockConsumer.expects(_: Any): Unit)

        // ... now let's see if we see the same cases.
        sut.withLimit(limit).supplyTo(mockConsumer)
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
        )                                                    -> 4,
        api.streamLegacy(identity)                           -> 200,
        implicitly[Trials.Factory[Either[Long, Int]]].trials -> 500
      )
    ) { (sut, limit) =>
      withExpectations {
        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        mockConsumer.verify(*).repeat(1 to limit)
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
        )                                                           -> 20,
        implicitly[Trials.Factory[Either[Boolean, Boolean]]].trials -> 4,
        api
          .choose(1 to 3)
          .flatMap(x => api.choose(1 to 10).map(x -> _))
          .filter { case (x, y) => 0 == (x * y) % 3 } -> (1 * 7 + 2 * 3 + 1 * 3)
      )
    ) { (sut, limit) =>
      withExpectations {
        val mockConsumer = stubFunction[Any, Unit]

        sut.withLimit(limit).supplyTo(mockConsumer)

        mockConsumer.verify(*).repeat(limit)
      }
    }

  case class JackInABox[Caze](caze: Caze)

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
        implicitly[Trials.Factory[Option[Int]]].trials.map {
          case None        => JackInABox(())
          case Some(value) => value
        }
      )
    ) { sut =>
      withExpectations {
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
      implicitly[Trials.Factory[Option[Int]]].trials.map {
        case None        => JackInABox(())
        case Some(value) => value
      }
    )
  ) { sut =>
    withExpectations {
      val surprisedConsumer: Any => Unit = {
        case JackInABox(caze) => throw ExceptionWithCasePayload(caze)
        case _                =>
      }

      val exception = Try {
        sut.withLimit(limit).supplyTo(surprisedConsumer)
      }.failed.get
        .asInstanceOf[sut.TrialException]

      val exceptionRecreatedViaRecipe = Try {
        sut.withRecipe(exception.recipe).supplyTo(surprisedConsumer)
      }.failed.get
        .asInstanceOf[sut.TrialException]

      exceptionRecreatedViaRecipe.provokingCase shouldBe exception.provokingCase
      exceptionRecreatedViaRecipe.recipe shouldBe exception.recipe
    }
  }

  it should "be shrunk to a simple case" in {
    type DescriptionTrialsAndCriterion[X] =
      (String, Trials[Vector[X]], Vector[X] => Boolean)

    def testBodyInWildcardCapture[X](
        trialsAndCriterion: DescriptionTrialsAndCriterion[X]
    ): Unit =
      withExpectations {
        trialsAndCriterion match {
          case (description, sut, exceptionCriterion) =>
            val complainingConsumer = { caze: Vector[X] =>
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

                try sut
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
      }

    forAll(
      Table[DescriptionTrialsAndCriterion[_]](
        "(description, trials, exceptionCriterion)",
        (
          // This first entry isn't expected to shrink the values, only the
          // length of the failing case.
          "Has more than one text item whose converted values sum to more than 7.",
          api.strings map (_.toVector) map (_.map(_.toInt)),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7
        ),
        (
          "Has more than one item and sums to more than 7.",
          doubleVectorTrials,
          (doubleVector: Vector[Double]) =>
            1 < doubleVector.size && doubleVector.sum > 7
        ),
        (
          "Has more than one item and sums to more than 7.",
          byteVectorTrials,
          (integerVector: Vector[Byte]) =>
            1 < integerVector.size && integerVector.sum > 7
        ),
        (
          "Has more than one item and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7
        ),
        (
          "Has more than one item, no zeroes and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7 && !integerVector
              .contains(0)
        ),
        (
          "Has more than one item, at least one zero and sums to more than 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && integerVector.sum > 7 && integerVector
              .contains(0)
        ),
        (
          "Has more than one item and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7
        ),
        (
          "Has more than one item, no zeroes and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7 && !longVector.contains(0)
        ),
        (
          "Has more than one item, at least one zero and sums to more than 7.",
          longVectorTrials,
          (longVector: Vector[Long]) =>
            1 < longVector.size && longVector.sum > 7 && longVector.contains(0)
        ),
        (
          "Has more than 7 items.",
          integerVectorTrials,
          (integerVector: Vector[Int]) => integerVector.size > 7
        ),
        (
          "Has more than one item, at least one non-zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .exists(0 != _)
        ),
        (
          "Has more than one item, no zeroes and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && !integerVector
              .contains(0)
        ),
        (
          "Has more than one item, at least one zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .contains(0)
        ),
        (
          "Has more than one item and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum
        ),
        (
          "Has more than one item, no zeroes and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum && !integerVector
              .contains(0)
        ),
        (
          "Has more than one item, at least one non-zero and sums to a positive multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 7 && 0 < integerVector.sum && integerVector
              .exists(0 != _)
        ),
        (
          "Flattened binary tree with more than one leaf that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum
        ),
        (
          "Flattened binary tree with more than one leaf and no zeroes that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum && !integerVector
              .contains(0)
        ),
        (
          "Flattened binary tree with more than one leaf and at least one zero that sums to a multiple of 19 greater than 19.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            1 < integerVector.size && 0 == integerVector.sum % 19 && 19 < integerVector.sum && integerVector
              .contains(0)
        ),
        (
          "Has more than five items, at least one non-zero and sums to a multiple of 7.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            5 < integerVector.size && 0 == integerVector.sum % 7 && integerVector
              .exists(0 != _)
        ),
        (
          "Has more than two items and is not sorted in ascending order.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && integerVector
              .zip(integerVector.tail)
              .exists { case (first, second) => first > second }
        ),
        (
          "Has more than two items and no duplicates.",
          integerVectorTrials,
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && integerVector.distinct == integerVector
        ),
        (
          "Flattened binary tree with more than two leaves whose odd-indexed leaves contain zeroes and even-indexed leaves contain non-zero values that sum to a multiple of 31 greater than 31.",
          binaryTreeTrials map (_.flatten),
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && 0 == integerVector.sum % 31 && 31 < integerVector.sum && (0 until integerVector.size forall (
              index => 0 == index % 2 ^ 0 == integerVector(index)
            ))
        ),
        (
          "List with more than two elements whose odd-indexed elements contain zeroes and even-indexed elements contain non-zero values that sum to a multiple of 31 greater than 31.",
          listTrials map (_.toVector),
          (integerVector: Vector[Int]) =>
            2 < integerVector.size && 0 == integerVector.sum % 31 && 31 < integerVector.sum && (0 until integerVector.size forall (
              index => 0 == index % 2 ^ 0 == integerVector(index)
            ))
        )
      )
    ) { trialsAndCriterion =>
      testBodyInWildcardCapture(trialsAndCriterion)
    }
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
    implicitly[Trials.Factory[Option[Int]]].trials
      .withLimit(limit)
      .supplyTo(println)

    implicitly[Trials.Factory[Either[(Boolean, Boolean), Double]]].trials
      .withLimit(limit)
      .supplyTo(println)
  }

  "test driving automatic implicit generation of a trials for a recursive data structure" should "not produce smoke" in {
    implicitly[Trials.Factory[List[Boolean]]].trials
      .withLimit(limit)
      .supplyTo(println)

    implicitly[Trials.Factory[BinaryTree]].trials
      .withLimit(limit)
      .supplyTo(println)
  }

  "inlined filtration" should "execute the controlled block if and only if the precondition holds" in {
    assertThrows[RejectionByInlineFilter] {
      Trials.whenever(satisfiedPrecondition = false) {
        fail(
          "If the precondition doesn't hold, the block should not be executed."
        )
      }
    }

    Trials.whenever(satisfiedPrecondition = true) {
      succeed
    }
  }

  private val oddHash = 1 == (_: Any).hashCode % 2

  it should "cover all the cases that would be covered by an explicit filtration over finite possibilities" in forAll(
    Table(
      "trials",
      api.only(2),
      api.only(15),
      api.choose(0 until 20),
      api.alternate(
        api.only(1),
        api.choose(0 until 20),
        api.only(15)
      ),
      api.choose(0 until 20).flatMap(x => api.choose(1, 3).map(x * _))
    )
  ) { trials =>
    withExpectations {
      val mockConsumer = mockFunction[Int, Unit]

      // Whatever cases are supplied by a monadic filtration set the
      // expectations...
      trials
        .filter(oddHash)
        .withLimit(limit)
        .supplyTo(mockConsumer.expects(_: Int): Unit)

      // ... now let's see if we see the same cases when we filter inline.
      trials
        .withLimit(limit)
        .supplyTo(caze =>
          Trials.whenever(oddHash(caze)) {
            mockConsumer(caze)
          }
        )
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
    withExpectations {
      // Count the cases supplied by a monadic filtration...

      val numberOfCasesViaMonadicFiltration = {
        var count = 0

        trials
          .filter(oddHash)
          .withLimit(limit)
          .supplyTo { _ => count = 1 + count }

        count
      }

      val mockConsumer = mockFunction[Int, Unit]

      // ... now let's see if we receive *exactly* the same number of cases when
      // we filter inline.

      mockConsumer
        .expects(*)
        .repeat(numberOfCasesViaMonadicFiltration): Unit

      trials
        .withLimit(limit)
        .supplyTo(caze =>
          Trials.whenever(oddHash(caze)) {
            mockConsumer(caze)
          }
        )
    }
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
}
