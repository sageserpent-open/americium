package com.sageserpent.americium

import cats.*
import com.sageserpent.americium.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.TrialsScaffolding.Tuple2Trials
import com.sageserpent.americium.java.Trials as JavaTrials

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.Factory
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.language.implicitConversions
import scala.util.DynamicVariable

object Trials {

  /** Start here: this yields a [[TrialsApi]] instance that is the gateway to
    * creating various kinds of [[Trials]] instances via its factory methods.
    *
    * @return
    *   A stateless [[TrialsApi]] instance.
    * @note
    *   All the methods defined in [[Trials]] itself are either ways of
    *   transforming and building up more complex trials, or for putting them to
    *   work by running test code.
    */
  def api: TrialsApi = TrialsApis.scalaApi

  private[americium] val throwInlineFilterRejection
      : DynamicVariable[() => Unit] =
    new DynamicVariable(() => {})

  /** This is an alternative to calling [[Trials.filter]] - the idea here is to
    * embed calls to this method in the test itself as an enclosing guard
    * condition prior to executing the core testing code. Usually the guard
    * precondition would involve some check on the supplied case and possibly
    * some other inputs that come from elsewhere. Like the use of filtration,
    * this approach will interact correctly with the shrinkage mechanism, which
    * is why it is provided.
    *
    * @param guardPrecondition
    *   A precondition that must be satisfied to run the test code.
    * @param block
    *   The core testing code, lazily evaluated.
    */
  def whenever(guardPrecondition: Boolean)(
      block: => Unit
  ): Unit =
    if (guardPrecondition) block
    else reject()

  /** Reject the test case that has been supplied to the currently executing
    * trial; this aborts the trial, but more test cases can still be supplied.
    * Like the use of filtration, this approach will interact correctly with the
    * shrinkage mechanism, which is why it is provided.
    *
    * @note
    *   This method will abort a trial's execution by throwing a private
    *   exception handled by the framework implementation. If it is called
    *   outside a trial, then it returns control as a no-operation.
    */
  def reject(): Unit = {
    throwInlineFilterRejection.value.apply()
  }

  implicit class CharacterTrialsSyntax(val characterTrials: Trials[Char]) {
    def strings: Trials[String] = characterTrials.several

    def stringsOfSize(size: Int): Trials[String] =
      characterTrials.lotsOfSize(size)
  }

  implicit val monadInstance: Monad[Trials] = new Monad[Trials]
    with StackSafeMonad[Trials] {
    override def pure[A](x: A): Trials[A] = api.only(x)

    override def flatMap[A, B](fa: Trials[A])(f: A => Trials[B]): Trials[B] =
      fa.flatMap(f)
  }

  implicit val functorFilterInstance: FunctorFilter[Trials] =
    new FunctorFilter[Trials] {
      override def functor: Functor[Trials] = monadInstance

      override def mapFilter[A, B](fa: Trials[A])(
          f: A => Option[B]
      ): Trials[B] = fa.mapFilter(f)
    }
}

trait Trials[+Case]
    extends TrialsScaffolding[Case]
    with GenerationSupport[Case] {
  override type SupplySyntaxType <: TrialsScaffolding.SupplyToSyntax[Case]

  /** This is mostly just for implementation purposes, as the Java incarnation
    * [[com.sageserpent.americium.java.Trials]] is effectively a wrapper around
    * the Scala incarnation [[Trials]]. However, if you want to pull cases via
    * an iterator, this is handy as currently the iterator access is via the
    * Java incarnation.
    *
    * @return
    *   The Java incarnation [[com.sageserpent.americium.java.Trials]] of this
    *   instance
    */
  def javaTrials: JavaTrials[Case @uncheckedVariance]

  def map[TransformedCase](
      transform: Case => TransformedCase
  ): Trials[TransformedCase]

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]
  ): Trials[TransformedCase]

  def filter(predicate: Case => Boolean): Trials[Case]

  def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): Trials[TransformedCase]

  def withFilter(predicate: Case => Boolean): Trials[Case] = filter(predicate)

  /** Fluent syntax to allow trials to be combined prior to calling
    * [[TrialsScaffolding.withLimit]] etc. This grants the user the choice of
    * either supplying the combined trials in the usual way, in which case the
    * [[Function]] will take a [[Tuple2]] parameterised by types {@code Case}
    * and {@code Case2}, or a [[Function2]] can be used taking separate
    * arguments of types {@code Case} and {@code Case2}.
    *
    * This can be repeated up to a limit by calling {@code and} on the results
    * to add more trials - this enables supply to consumers of higher argument
    * arity.
    *
    * @param secondTrials
    * @tparam Case2
    * @return
    *   Syntax object that permits the test code to consume either a pair or two
    *   separate arguments.
    */
  def and[Case2](secondTrials: Trials[Case2]): Tuple2Trials[Case, Case2]

  /** Fluent syntax to allow trials of *dissimilar* types to be supplied as
    * alternatives to the same test. In contrast to the [[TrialsApi.alternate]],
    * the alternatives do not have to conform to the same type; instead here we
    * can switch in the test between unrelated types using an [[Either]]
    * instance to hold cases supplied from either this trials instance or from
    * {@code alternativeTrials}.
    *
    * @param alternativeTrials
    * @tparam Case2
    * @return
    *   {@link Either} that is populated with either a {@code Case} or with a
    *   {@code Case2}.
    */
  def or[Case2](alternativeTrials: Trials[Case2]): Trials[Either[Case, Case2]]

  /** @return
    *   A lifted trials that wraps the underlying cases from this in an
    *   [[Option]]; the resulting trials also supplies a special case of
    *   [[Option.empty]].
    */
  def options: Trials[Option[Case]]

  /** Transform this to a trials of collection, where {@code Collection} is some
    * kind of collection that can be built from elements of type {@code Case} by
    * a [[Factory]].
    *
    * @param factory
    *   A [[Factory]] that can build a {@code Collection}.
    * @tparam Collection
    *   Any kind of collection that can take an arbitrary number of elements of
    *   type {@code Case}.
    * @return
    *   A [[Trials]] instance that yields {@code Collection} instances.
    */
  def several[Collection](implicit
      factory: Factory[Case, Collection]
  ): Trials[Collection]

  def lists: Trials[List[Case]]

  def sets: Trials[Set[Case @uncheckedVariance]]

  def sortedSets(implicit
      ordering: Ordering[Case @uncheckedVariance]
  ): Trials[SortedSet[Case @uncheckedVariance]]

  def maps[Value](values: Trials[Value]): Trials[Map[Case @uncheckedVariance, Value]]

  def sortedMaps[Value](values: Trials[Value])(implicit
      ordering: Ordering[Case @uncheckedVariance]
  ): Trials[SortedMap[Case @uncheckedVariance, Value]]

  /** Transform this to a trials of collection, where {@code Collection} is some
    * kind of collection that can be built from elements of type {@code Case} by
    * a [[Factory]]. The collection instances yielded by the result are all
    * built from the specified number of elements.
    *
    * @param size
    *   The number of elements of type [[Case]] to build the collection instance
    *   from. Be aware that sets, maps and bounded size collections don't have
    *   to accept that many elements.
    * @param factory
    *   A [[Factory]] that can build a {@code Collection}.
    * @tparam Collection
    *   Any kind of collection that can take an arbitrary number of elements of
    *   type {@code Case}.
    * @return
    *   A {@link Trials} instance that yields {@code Collection} instances.
    */
  def lotsOfSize[Collection](size: Int)(implicit
      factory: Factory[Case, Collection]
  ): Trials[Collection]

  def listsOfSize(size: Int): Trials[List[Case]]
}
