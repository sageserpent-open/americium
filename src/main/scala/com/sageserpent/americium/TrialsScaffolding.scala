package com.sageserpent.americium

import com.sageserpent.americium.TrialsScaffolding.{ShrinkageStop, noStopping}
import com.sageserpent.americium.java.TrialsDefaults.{
  defaultComplexityLimit,
  defaultShrinkageAttemptsLimit
}
import com.sageserpent.americium.java.{
  CaseSupplyCycle,
  CasesLimitStrategy,
  TestIntegrationContext,
  TrialsFactoring
}

object TrialsScaffolding {

  /** @return
    *   A predicate that examines both state captured by the instance of
    *   [[ShrinkageStop]] and the case passed to it. When the predicate holds,
    *   the shrinkage is terminated.
    * @note
    *   Building the predicate is expected to set up or capture any state
    *   required by it, such as a freshly started timer or count set to zero.
    */
  type ShrinkageStop[-Case] = () => Case => Boolean

  val noStopping: ShrinkageStop[Any] = () => _ => false

  val noShrinking: ShrinkageStop[Any] = () => _ => true

  trait SupplyToSyntax[+Case] {
    def withSeed(seed: Long): SupplyToSyntax[Case]

    /** The maximum permitted complexity when generating a case.
      *
      * @note
      *   Complexity is something associated with the production of an instance
      *   of {@code Case} when a [[Trials]] is supplied to some test consumer.
      *   It ranges from one up to (and including) the {@code complexityLimit}
      *   and captures some sense of the case being more elaborately constructed
      *   as it increases - as an example, the use of flat-mapping to combine
      *   inputs from multiple trials instances drives the complexity up for
      *   each flatmap stage. In practice, this results in larger collection
      *   instances having greater complexity. Deeply recursive trials also
      *   result in high complexity.
      */
    def withComplexityLimit(complexityLimit: Int): SupplyToSyntax[Case]

    /** The maximum number of shrinkage attempts when shrinking a case. Setting
      * this to zero disables shrinkage and will thus yield the original failing
      * case.
      */
    def withShrinkageAttemptsLimit(
        shrinkageAttemptsLimit: Int
    ): SupplyToSyntax[Case]

    /** @see
      *   [[ShrinkageStop]]
      */
    def withShrinkageStop(
        shrinkageStop: ShrinkageStop[Case]
    ): SupplyToSyntax[Case]

    /** Configures whether a check is made after supplying test cases that any
      * valid trials were performed during supply.
      *
      * If either no test cases were produced, or the trials rejected all of
      * them, then the check will throw a [[NoValidTrialsException]].
      * @param enabled
      *   If true, perform the check once supply is finished.
      * @note
      *   Checking is enabled by default.
      */
    def withValidTrialsCheck(enabled: Boolean): SupplyToSyntax[Case]

    /** Consume trial cases until either there are no more or an exception is
      * thrown by {@code consumer}. If an exception is thrown, attempts will be
      * made to shrink the trial case that caused the exception to a simpler
      * case that throws an exception - the specific kind of exception isn't
      * necessarily the same between the first exceptional case and the final
      * simplified one. The exception from the simplified case (or the original
      * exceptional case if it could not be simplified) is wrapped in an
      * instance of [[TrialException]] which also contains the {@code Case} that
      * provoked the exception.
      *
      * @param consumer
      *   An operation that consumes a {@code Case}, and may throw an exception.
      */
    def supplyTo(consumer: Case => Unit): Unit

    def asIterator(): Iterator[Case]

    def testIntegrationContexts(): Iterator[TestIntegrationContext[Case]]

    def reproduce(recipe: String): Case
  }

  trait SupplyToSyntaxTuple2[+Case1, +Case2]
      extends SupplyToSyntax[(Case1, Case2)] {
    def supplyTo(consumer: (Case1, Case2) => Unit): Unit
  }

  trait SupplyToSyntaxTuple3[+Case1, +Case2, +Case3]
      extends SupplyToSyntax[(Case1, Case2, Case3)] {
    def supplyTo(consumer: (Case1, Case2, Case3) => Unit): Unit
  }

  trait SupplyToSyntaxTuple4[+Case1, +Case2, +Case3, +Case4]
      extends SupplyToSyntax[(Case1, Case2, Case3, Case4)] {
    def supplyTo(consumer: (Case1, Case2, Case3, Case4) => Unit): Unit
  }

  trait Tuple2Trials[+Case1, +Case2] extends TrialsScaffolding[(Case1, Case2)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple2[Case1, Case2]

    def and[Case3](
        thirdTrials: Trials[Case3]
    ): Tuple3Trials[Case1, Case2, Case3]
  }

  trait Tuple3Trials[+Case1, +Case2, +Case3]
      extends TrialsScaffolding[(Case1, Case2, Case3)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple3[Case1, Case2, Case3]

    def and[Case4](
        fourthTrials: Trials[Case4]
    ): Tuple4Trials[Case1, Case2, Case3, Case4]
  }

  trait Tuple4Trials[+Case1, +Case2, +Case3, +Case4]
      extends TrialsScaffolding[(Case1, Case2, Case3, Case4)] {
    override type SupplySyntaxType <: SupplyToSyntaxTuple4[
      Case1,
      Case2,
      Case3,
      Case4
    ]
  }
}

trait TrialsScaffolding[+Case] extends TrialsFactoring[Case] {
  type SupplySyntaxType <: TrialsScaffolding.SupplyToSyntax[Case]

  /** Use this to lose any specialised supply syntax and go back to the regular
    * [[Trials]] API. The motivation for this is when the `and` combinator is
    * used to glue together several trials instances, but we want to treat the
    * result as a plain trials of tuples, rather than calling
    * [[Trials.withLimits]] etc there and then.
    *
    * @return
    *   The equivalent [[Trials]] instance.
    */
  def trials: Trials[Case]

  /** Fluent syntax for configuring a limit to the number of cases supplied to a
    * consumer.
    *
    * @param limit
    *   The maximum number of cases that can be supplied - note that this is no
    *   guarantee that so many cases will be supplied, it is simply a limit.
    * @return
    *   An instance of [[SupplyToSyntax]] with the limit configured.
    */
  def withLimit(limit: Int): SupplySyntaxType

  /** Fluent syntax for configuring a limit strategy for the number of cases
    * supplied to a consumer.
    *
    * @param casesLimitStrategyFactory
    *   A factory method that should produce a *fresh* instance of a
    *   [[CasesLimitStrategy]] on each call.
    * @return
    *   An instance of [[SupplyToSyntax]] with the strategy configured.
    * @note
    *   The factory {@code casesLimitStrategyFactory} takes an argument of
    *   [[CaseSupplyCycle]]; this can be used to dynamically configure the
    *   strategy depending on which cycle the strategy is intended for, or
    *   simply disregarded if a one-size-fits-all approach is desired.
    */
  def withStrategy(
      casesLimitStrategyFactory: CaseSupplyCycle => CasesLimitStrategy,
      @deprecated("Use a following call to `withComplexityLimit` instead.")
      complexityLimit: Int = defaultComplexityLimit,
      @deprecated(
        "Use a following call to `withShrinkageAttemptsLimit` instead."
      )
      shrinkageAttemptsLimit: Int = defaultShrinkageAttemptsLimit,
      @deprecated("Use a following call to `withShrinkageStop` instead.")
      shrinkageStop: ShrinkageStop[Case] = noStopping
  ): SupplySyntaxType

  @deprecated(
    "Use `withLimit` followed by calls to `withComplexityLimit`, `withShrinkageAttemptsLimit` and `withShrinkageStop`."
  )
  def withLimits(
      casesLimit: Int,
      @deprecated("Use a following call to `withComplexityLimit` instead.")
      complexityLimit: Int = defaultComplexityLimit,
      @deprecated(
        "Use a following call to `withShrinkageAttemptsLimit` instead."
      )
      shrinkageAttemptsLimit: Int = defaultShrinkageAttemptsLimit,
      @deprecated("Use a following call to `withShrinkageStop` instead.")
      shrinkageStop: ShrinkageStop[Case] = noStopping
  ): SupplySyntaxType

  @deprecated(
    "Use the JVM system property JavaPropertyNames.recipeHashJavaProperty() - `trials.recipeHash` instead to force existing tests written using `withLimit` or `withStrategy` to pick up the recipe. This has the advantage of being a temporary measure for debugging that doesn't require test code changes."
  )
  def withRecipe(recipe: String): SupplySyntaxType
}
