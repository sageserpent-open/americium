package com.sageserpent.americium.java;

import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import cyclops.data.tuple.Tuple4;
import cyclops.function.Consumer3;
import cyclops.function.Consumer4;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sageserpent.americium.java.TrialsDefaults.defaultComplexityLimit;
import static com.sageserpent.americium.java.TrialsDefaults.defaultShrinkageAttemptsLimit;

public interface TrialsScaffolding<Case,
        SupplySyntaxType extends TrialsScaffolding.SupplyToSyntax<Case>>
        extends TrialsFactoring<Case> {
    /**
     * Fluent syntax for configuring a limit to the number of cases
     * supplied to a consumer.
     *
     * @param limit The maximum number of cases that can be supplied - note
     *              that this is no guarantee that so many cases will be
     *              supplied, it is simply a limit.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     * @deprecated Use {@link Trials#withLimits(int, OptionalLimits)} instead.
     */
    @Deprecated
    SupplySyntaxType withLimit(final int limit);

    /**
     * Fluent syntax for configuring a limit to the number of cases
     * supplied to a consumer.
     *
     * @param limit           The maximum number of cases that can be supplied
     *                        - note that this is no guarantee that so many
     *                        cases will be supplied, it is simply a limit.
     * @param complexityLimit The highest complexity that a case may achieve
     *                        as it is synthesized, it is an inclusive limit.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     * @apiNote Complexity is something associated with the production of a
     * {@link Case} when a {@link Trials} is supplied to some test consumer.
     * It ranges from one up to (and including) the {@code complexityLimit}
     * and captures some sense of the case being more elaborately constructed
     * as it increases - as an example, the use of flatmapping to combine
     * inputs from multiple trials instances drives the complexity up for
     * each flatmap stage. In practice, this results in larger collection
     * instances having greater complexity. Deeply recursive trials also
     * result in high complexity.
     * @deprecated Use {@link Trials#withLimits(int, OptionalLimits)} instead.
     */
    @Deprecated
    SupplySyntaxType withLimit(final int limit,
                               final int complexityLimit);

    /**
     * Allows the shrinkage process to be terminated externally by a stateful
     * predicate supplied by the user. That predicate is free to use a timer,
     * counts, consult the heap usage, or examine the best shrunken case seen
     * so far.
     *
     * @param <Case> The type or supertype of the cases yielded by a trials
     *               instance.
     */
    @FunctionalInterface
    interface ShrinkageStop<Case> {
        /**
         * @return A predicate that examines both state captured by the
         * instance of {@link ShrinkageStop} and the case passed to it. When
         * the predicate holds, the shrinkage is terminated.
         * @apiNote Building the predicate is expected to set up or capture
         * any state required by it, such as a freshly started timer or count
         * set to zero.
         */
        Predicate<Case> build();
    }

    ShrinkageStop<Object> noStopping = () -> (unused -> false);

    ShrinkageStop<Object> noShrinking = () -> (unused -> true);

    @lombok.Builder
    @lombok.EqualsAndHashCode
    class OptionalLimits {
        public static OptionalLimits defaults =
                OptionalLimits.builder().build();

        /**
         * The maximum permitted complexity when generating a case.
         *
         * @apiNote Complexity is something associated with the production of
         * a {@link Case} when a {@link Trials} is supplied to some test
         * consumer. It ranges from one up to (and including) the {@code
         * complexityLimit} and captures some sense of the case being more
         * elaborately constructed as it increases - as an example, the use
         * of flatmapping to combine inputs from multiple trials instances
         * drives the complexity up for each flatmap stage. In practice, this
         * results in larger collection instances having greater complexity.
         * Deeply recursive trials also result in high complexity.
         */
        @lombok.Builder.Default
        public final int complexity = defaultComplexityLimit;

        /**
         * The maximum number of shrinkage attempts when shrinking a case.
         * Setting this to zero disables shrinkage and will thus yield the
         * original failing case.
         */
        @lombok.Builder.Default
        public final int shrinkageAttempts =
                defaultShrinkageAttemptsLimit;
    }

    /**
     * Fluent syntax for configuring a limit to the number of cases supplied
     * to a consumer.
     *
     * @param casesLimit     The maximum number of cases that can be supplied
     *                       - note that this is no guarantee that so many
     *                       cases will be supplied, it is simply a limit.
     * @param optionalLimits Optional limits used to configure other aspects
     *                       of supplying and shrinkage.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     */
    SupplySyntaxType withLimits(final int casesLimit,
                                final OptionalLimits optionalLimits);

    /**
     * Fluent syntax for configuring a limit to the number of cases supplied
     * to a consumer.
     *
     * @param casesLimit     The maximum number of cases that can be supplied
     *                       - note that this is no guarantee that so many
     *                       cases will be supplied, it is simply a limit.
     * @param optionalLimits Optional limits used to configure other aspects
     *                       of supplying and shrinkage.
     * @param shrinkageStop  Allows external control of the shrinkage process
     *                       in addition to what is configured by the {@code
     *                       optionalLimits}. See also {@link ShrinkageStop}.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     */
    SupplySyntaxType withLimits(final int casesLimit,
                                final OptionalLimits optionalLimits,
                                final ShrinkageStop<? super Case> shrinkageStop);

    /**
     * Reproduce a trial case using a recipe. This is intended to repeatedly
     * run a test against a known failing case when debugging.
     *
     * @param recipe This encodes a specific {@link Case} and will only be
     *               understood by the same *value* of {@link Trials} that
     *               was used to obtain it.
     * @return An instance of {@link SupplyToSyntax} that supplies the
     * reproduced trial case.
     */
    SupplySyntaxType withRecipe(final String recipe);

    interface SupplyToSyntax<Case> {
        /**
         * Consume trial cases until either there are no more or an exception
         * is thrown by {@code consumer}. If an exception is thrown, attempts
         * will be made to shrink the trial case that caused the exception to
         * a simpler case that throws an exception - the specific kind of
         * exception isn't necessarily the same between the first exceptional
         * case and the final simplified one. The exception from the
         * simplified case (or the original exceptional case if it could not
         * be simplified) is wrapped in an instance of {@link TrialException}
         * which also contains the {@link Case} that provoked the exception.
         *
         * @param consumer An operation that consumes a {@link Case}, and may
         *                 throw an exception.
         * @note The limit applies to the count of the number of supplied
         * cases, regardless of whether some of these cases are
         * duplicated or not. There is no guarantee that all of
         * the non-duplicated cases have to be supplied, even if
         * they could potentially all fit within the limit.
         */
        void supplyTo(final Consumer<Case> consumer);

        Iterator<Case> asIterator();

        Tuple2<Iterator<Case>, InlinedCaseFiltration> testIntegration();
    }

    interface Tuple2Trials<Case1, Case2> extends
            TrialsScaffolding<Tuple2<Case1, Case2>,
                    Tuple2Trials.SupplyToSyntaxTuple2<Case1, Case2>> {
        <Case3> Tuple3Trials<Case1, Case2, Case3> and(
                Trials<Case3> thirdTrials);

        interface SupplyToSyntaxTuple2<Case1, Case2>
                extends SupplyToSyntax<Tuple2<Case1, Case2>> {
            void supplyTo(BiConsumer<Case1, Case2> biConsumer);
        }
    }

    interface Tuple3Trials<Case1, Case2, Case3> extends
            TrialsScaffolding<Tuple3<Case1, Case2, Case3>,
                    Tuple3Trials.SupplyToSyntaxTuple3<Case1, Case2, Case3>> {
        <Case4> Tuple4Trials<Case1, Case2, Case3, Case4> and(
                Trials<Case4> fourthTrials);

        interface SupplyToSyntaxTuple3<Case1, Case2, Case3>
                extends SupplyToSyntax<Tuple3<Case1, Case2, Case3>> {
            void supplyTo(Consumer3<Case1, Case2, Case3> triConsumer);
        }
    }

    interface Tuple4Trials<Case1, Case2, Case3, Case4> extends
            TrialsScaffolding<Tuple4<Case1, Case2, Case3, Case4>,
                    Tuple4Trials.SupplyToSyntaxTuple4<Case1, Case2, Case3,
                            Case4>> {
        interface SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4>
                extends SupplyToSyntax<Tuple4<Case1, Case2, Case3, Case4>> {
            void supplyTo(Consumer4<Case1, Case2, Case3, Case4> quadConsumer);
        }
    }
}
