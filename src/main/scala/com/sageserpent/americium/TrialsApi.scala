package com.sageserpent.americium

import cats.Traverse

import _root_.java.time.Instant
import scala.collection.Factory

trait TrialsApi {

  /** Helper to break direct recursion when implementing a recursively defined
    * trials. You need this when your definition either doesn't have a flatmap,
    * or the first argument (the 'left hand side') of a flatmap is where the
    * recursion takes place. You won't need this very often, if at all.
    *
    * @param delayed
    *   Some definition of a [[Trials]] instance that is typically a recursive
    *   step, so you don't want it to execute there and then to avoid infinite
    *   recursion.
    * @tparam Case
    * @return
    *   A safe form of the {@code delayed} trials instance that won't
    *   immediately execute, but will yield the same {@code Case} instances.
    */
  def delay[Case](delayed: => Trials[Case]): Trials[Case]

  /** Make a [[Trials]] instance that only ever yields a single instance of
    * {@code Case} . Typically used with alternation to mix in some important
    * special case with say, a bunch of streamed cases, and also used as a base
    * case for recursively-defined trials.
    *
    * @param onlyCase
    * @tparam Case
    * @return
    *   A [[Trials]] instance that only ever yields {@code onlyCase} .
    */
  def only[Case](onlyCase: Case): Trials[Case]

  /** Denote a situation where no cases are possible. This is obviously an
    * obscure requirement - it is intended for sophisticated composition of
    * several [[Trials]] instances via nested flat-mapping where a combination
    * of the parameters from the prior levels of flat-mapping cannot yield a
    * test-case, possibly because of a precondition violation or simply because
    * the combination is undesirable for testing.
    *
    * In this situation one can detect such bad combinations and substitute an
    * impossible [[Trials]] instance.
    *
    * @tparam Case
    * @return
    *   A [[Trials]] instance that never yields any cases.
    */
  def impossible[Case]: Trials[Case]

  /** Produce a [[Trials]] instance that chooses between several cases.
    *
    * @param firstChoice
    *   Mandatory first choice, so there is at least one [[Case]].
    * @param secondChoice
    *   Mandatory second choice, so there is always some element of choice.
    * @param otherChoices
    *   Optional further choices.
    * @return
    *   A [[Trials]] instance.
    * @note
    *   The peculiar signature is to avoid ambiguity with the overloads for an
    *   iterable / array of cases.
    */
  def choose[Case](
      firstChoice: Case,
      secondChoice: Case,
      otherChoices: Case*
  ): Trials[Case]

  def choose[Case](choices: Iterable[Case]): Trials[Case]

  def chooseWithWeights[Case](
      firstChoice: (Int, Case),
      secondChoice: (Int, Case),
      otherChoices: (Int, Case)*
  ): Trials[Case]

  def chooseWithWeights[Case](choices: Iterable[(Int, Case)]): Trials[Case]

  /** Produce a [[Trials]] instance that alternates between the cases of the
    * given alternatives. <p>
    *
    * @param firstAlternative
    *   Mandatory first alternative, so there is at least one [[Trials]].
    * @param secondAlternative
    *   Mandatory second alternative, so there is always some element of
    *   alternation.
    * @param otherAlternatives
    *   Optional further alternatives.
    * @return
    *   A [[Trials]] instance.
    * @note
    *   The peculiar signature is to avoid ambiguity with the overloads for an
    *   iterable / array of cases.
    */
  def alternate[Case](
      firstAlternative: Trials[Case],
      secondAlternative: Trials[Case],
      otherAlternatives: Trials[Case]*
  ): Trials[Case]

  def alternate[Case](alternatives: Iterable[Trials[Case]]): Trials[Case]

  def alternateWithWeights[Case](
      firstAlternative: (Int, Trials[Case]),
      secondAlternative: (Int, Trials[Case]),
      otherAlternatives: (Int, Trials[Case])*
  ): Trials[Case]

  def alternateWithWeights[Case](
      alternatives: Iterable[(Int, Trials[Case])]
  ): Trials[Case]

  /** Combine a sequence of trials instances into a single trials instance that
    * yields sequences, where those sequences all have the size given by the
    * number of trials, and the element in each position in the sequence is
    * provided by the trials instance at the corresponding position within
    * {@code sequenceOfTrials} .
    *
    * The sequence type for each yielded case is based on the sequence type for
    * the parameter.
    *
    * @param sequenceOfTrials
    *   Several trials that act as sources for the elements of lists yielded by
    *   the resulting [[Trials]] instance.
    * @tparam Element
    *   The type of the list elements yielded by the resulting [[Trials]]
    *   instance.
    * @return
    *   A [[Trials]] instance that yields lists of the same size.
    */
  def sequences[Element, Sequence[_]: Traverse](
      sequenceOfTrials: Sequence[Trials[Element]]
  )(implicit
      factory: Factory[Element, Sequence[Element]]
  ): Trials[Sequence[Element]]

  /** This is for advanced usage, where there is a need to control how trials
    * instances are formulated to avoid hitting the complexity limit, or
    * alternatively to control the amount of potentially unbounded recursion
    * when trials are recursively flat-mapped. If you don't know what this
    * means, you probably don't need this.
    *
    * The notion of a complexity limit is described in
    * [[TrialsScaffolding.SupplyToSyntax.withComplexityLimit]]
    *
    * @return
    *   A [[Trials]] instance yielding the complexity associated with the
    *   definition's context, taking into account any flat-mapping this call is
    *   embedded in.
    */
  def complexities: Trials[Int]

  /** This yields integer ids that are guaranteed to be unique *within* a
    * complex test case. This uniqueness holds when multiple ids are combined by
    * flat-mapping, possibly in a recursively-formulated trials instance.
    *
    * @note
    *   The notion of uniqueness applies *within* the formulation of a test
    *   case, not between test cases. It is to be expected that successive test
    *   cases may re-use id values seen in earlier ones.
    *
    * @return
    *   A non-negative integer.
    */

  def uniqueIds: Trials[Int]

  /** Produce a trials instance that stream cases from a factory.
    *
    * This is used where we want to generate a supposedly potentially unbounded
    * number of cases, although there is an implied upper limit based on the
    * number of distinct long values in the factory's input domain.
    *
    * @param caseFactory
    *   Pure (in other words, stateless) function that produces a {@code Case}
    *   from a long value. Each call taking the same long value is expected to
    *   yield the same case.
    *
    * Rather than [[Function]], the type [[CaseFactory]] is used here - this
    * allows the factory to declare its domain of valid inputs, as well as the
    * input value in that domain that denotes a 'maximally shrunk' case.
    *
    * The factory is expected to be an injection, so it can be fed with any
    * potential long value from that domain. It is not expected to be a
    * surjection, so distinct long values may result in equivalent cases.
    *
    * It is expected that long values closer to the case factory's maximally
    * shrunk input yield 'smaller' cases, in whatever sense is appropriate to
    * either the actual type of the cases or their specific use as implemented
    * by the factory.
    * @return
    *   A [[Trials]] instance.
    */
  def stream[Case](factory: CaseFactory[Case]): Trials[Case]

  /** Produce a trials instance that stream cases from a factory.
    *
    * This is used where we want to generate a supposedly potentially unbounded
    * number of cases, although there is an implied upper limit based on the
    * number of distinct long values in practice.
    *
    * @param factory
    *   Pure (in other words, stateless) function that produces a {@code Case}
    *   from a long value. Each call taking the same long value is expected to
    *   yield the same case. The factory is expected to be an injection, so it
    *   can be fed with any potential long value, negative, zero or positive. It
    *   is not expected to be a surjection, even if there are at most as many
    *   possible values of {@code Case} as there are long values, so distinct
    *   long values may result in equivalent cases.
    *
    * It is expected that long values closer to zero yield 'smaller' cases, in
    * whatever sense is appropriate to either the actual type of the cases or
    * their specific use as encoded by the factory.
    * @return
    *   A [[Trials]] instance.
    */
  def streamLegacy[Case](factory: Long => Case): Trials[Case]

  def bytes: Trials[Byte]

  def integers: Trials[Int]

  def integers(lowerBound: Int, upperBound: Int): Trials[Int]

  def integers(
      lowerBound: Int,
      upperBound: Int,
      shrinkageTarget: Int
  ): Trials[Int]

  def nonNegativeIntegers: Trials[Int]

  def longs: Trials[Long]

  def longs(lowerBound: Long, upperBound: Long): Trials[Long]

  def longs(
      lowerBound: Long,
      upperBound: Long,
      shrinkageTarget: Long
  ): Trials[Long]

  def nonNegativeLongs: Trials[Long]

  def bigInts(lowerBound: BigInt, upperBound: BigInt): Trials[BigInt]

  def bigInts(
      lowerBound: BigInt,
      upperBound: BigInt,
      shrinkageTarget: BigInt
  ): Trials[BigInt]

  def doubles: Trials[Double]

  def doubles(lowerBound: Double, upperBound: Double): Trials[Double]

  def doubles(
      lowerBound: Double,
      upperBound: Double,
      shrinkageTarget: Double
  ): Trials[Double]

  def bigDecimals(
      lowerBound: BigDecimal,
      upperBound: BigDecimal
  ): Trials[BigDecimal]

  def bigDecimals(
      lowerBound: BigDecimal,
      upperBound: BigDecimal,
      shrinkageTarget: BigDecimal
  ): Trials[BigDecimal]

  def booleans: Trials[Boolean]

  def characters: Trials[Char]

  def characters(lowerBound: Char, upperBound: Char): Trials[Char]

  def characters(
      lowerBound: Char,
      upperBound: Char,
      shrinkageTarget: Char
  ): Trials[Char]

  def instants: Trials[Instant]

  def instants(lowerBound: Instant, upperBound: Instant): Trials[Instant]

  def instants(
      lowerBound: Instant,
      upperBound: Instant,
      shrinkageTarget: Instant
  ): Trials[Instant]

  def strings: Trials[String]

  /** Produce a trials instance whose cases can be used to permute elements of
    * indexed collections, or as permutations of integers in their own right.
    *
    * @param numberOfIndices
    *   The size of the set of indices ''[0; numberOfIndices)'' that
    *   permutations are generated from: also the size of the permutations
    *   themselves.
    * @return
    *   A [[Trials]] instance whose cases are permutations of the integer range
    *   ''[0; numberOfIndices)''.
    */
  def indexPermutations(numberOfIndices: Int): Trials[Vector[Int]]

  /** Produce a trials instance whose cases can be used to permute elements of
    * indexed collections, or as permutations of integers in their own right.
    *
    * @param numberOfIndices
    *   The size of the set of indices ''[0; numberOfIndices)'' that
    *   permutations are generated from.
    * @param permutationSize
    *   The size of the generated permutations; must be in the range ''[0;
    *   numberOfIndices]''.
    * @return
    *   A [[Trials]] instance whose cases are permutations of the integer range
    *   ''[0; numberOfIndices)''.
    */
  def indexPermutations(
      numberOfIndices: Int,
      permutationSize: Int
  ): Trials[Vector[Int]]

  /** Produce a trials instance whose cases can be used to select combinations
    * of elements of indexed collections, or as combinations of integers in
    * their own right.
    *
    * @param numberOfIndices
    *   The size of the set of indices ''[0; numberOfIndices)'' that
    *   combinations are generated from.
    * @param combinationSize
    *   The size of the generated combinations; must be in the range ''[0;
    *   numberOfIndices]''.
    * @return
    *   A [[Trials]] instance whose cases are combinations of the integer range
    *   ''[0; numberOfIndices)''.
    * @note
    *   The indices in the combinations are sorted in ascending order.
    */
  def indexCombinations(
      numberOfIndices: Int,
      combinationSize: Int
  ): Trials[Vector[Int]]

  /** Produce a trials instance whose cases are vectors containing elements
    * picked alternately from {@code iterables} . The order of elements
    * contributed by any given iterable is preserved in the yielded vectors, but
    * there can be arbitrary alternation across the iterables.
    * @param shrinkToRoundRobin
    *   If true, then shrinkage will try to proceed to a systematic alternation
    *   across the iterables, draining them in round-robin fashion. Otherwise,
    *   shrinkage will try to proceed towards concatenating the iterables,
    *   draining each one completely in sequence.
    * @param iterables
    *   Sources of elements.
    * @tparam Element
    *   The type of the vector elements yielded by the resulting [[Trials]]
    *   instance.
    * @return
    *   A [[Trials]] instance whose cases are vectors of {@code Element} .
    */
  def pickAlternatelyFrom[Element](
      shrinkToRoundRobin: Boolean,
      iterables: Iterable[Element]*
  ): Trials[Vector[Element]]
}
