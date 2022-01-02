package com.sageserpent.americium.java;

import com.google.common.collect.*;
import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import cyclops.data.tuple.Tuple4;
import cyclops.function.Consumer3;
import cyclops.function.Consumer4;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.*;


public interface Trials<Case> extends TrialsFactoring<Case> {
    /**
     * Start here: this yields a {@link TrialsApi} instance that is the
     * gateway to creating various kinds of {@link Trials} instances via its
     * factory methods.
     *
     * @return A stateless {@link TrialsApi} instance.
     * @apiNote All the methods defined in {@link Trials} itself are either
     * ways of transforming and building up more complex trials, or for
     * putting them to work by running test code.
     */
    static TrialsApi api() {
        return (TrialsApi) TrialsApiImplementation.javaApi();
    }

    static <Result> Result whenever(Boolean satisfiedPrecondition,
                                    Supplier<Result> block) {
        return com.sageserpent.americium.Trials.whenever(satisfiedPrecondition,
                                                         block::get);
    }

    static void whenever(Boolean satisfiedPrecondition, Runnable block) {
        com.sageserpent.americium.Trials.whenever(satisfiedPrecondition, () -> {
            block.run();
            return null;
        });
    }

    /**
     * This is just for implementation purposes, as the Java incarnation
     * {@link Trials} is effectively a wrapper around the Scala incarnation
     * {@link com.sageserpent.americium.Trials}. Still, it's there if you
     * want it...
     *
     * @return The Scala incarnation {@link com.sageserpent.americium.Trials}
     * of this instance
     */
    com.sageserpent.americium.Trials<Case> scalaTrials();

    <TransformedCase> Trials<TransformedCase> map(
            final Function<Case, TransformedCase> transform);

    <TransformedCase> Trials<TransformedCase> flatMap(
            final Function<Case, Trials<TransformedCase>> step);

    Trials<Case> filter(final Predicate<Case> predicate);

    <TransformedCase> Trials<TransformedCase> mapFilter(
            final Function<Case, Optional<TransformedCase>> filteringTransform);

    /**
     * Fluent syntax for configuring a limit to the number of cases
     * supplied to a consumer.
     *
     * @param limit The maximum number of cases that can be supplied - note
     *              that this is no guarantee that so many cases will be
     *              supplied, it is simply a limit.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     * @deprecated The overload
     * {@link Trials#withLimits(int, AdditionalLimits)} with all the
     * arguments following the first defaulted will replace this.
     */
    @Deprecated
    Trials.SupplyToSyntax<Case> withLimit(final int limit);

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
     * It ranges from one up to (but not including) the {@code
     * complexityLimit} and captures some sense of the case being more
     * elaborately constructed as it increases - as an example, the use of
     * flatmapping to combine inputs from multiple trials instances drives
     * the complexity up for each flatmap stage. In practice, this results in
     * larger collection instances having greater complexity. Deeply
     * recursive trials also result in high complexity.
     * @deprecated The overload
     * {@link Trials#withLimits(int, AdditionalLimits)} with all the
     * arguments following the first defaulted will replace this.
     */
    @Deprecated
    Trials.SupplyToSyntax<Case> withLimit(final int limit,
                                          final int complexityLimit);

    @FunctionalInterface
    interface ShrinkageStop<Case> {
        Predicate<Case> build();
    }

    ShrinkageStop<Object> noStopping = () -> (unused -> false);

    ShrinkageStop<Object> noShrinking = () -> (unused -> true);

    @lombok.Builder
    @lombok.EqualsAndHashCode
    class AdditionalLimits<Case> {
        public static AdditionalLimits<Object> defaults =
                AdditionalLimits.builder().build();

        @lombok.Builder.Default
        final int complexityLimit = TrialsFactoring.defaultComplexityLimit();

        @lombok.Builder.Default
        final int shrinkageAttemptsLimit =
                TrialsFactoring.defaultShrinkageAttemptsLimit();

        @lombok.Builder.Default
        final ShrinkageStop<? super Case> shrinkageStop = noStopping;
    }

    Trials.SupplyToSyntax<Case> withLimits(final int casesLimit,
                                           final AdditionalLimits<?
                                                   super Case> additionalLimits);

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
    Trials.SupplyToSyntax<Case> withRecipe(final String recipe);

    <Case2> Trials.Tuple2Trials<Case, Case2> and(
            Trials<Case2> secondTrials);

    /**
     * Transform this to a trials of collection, where {@link Collection} is
     * some kind of collection that can be built from elements of type
     * {@link Case} by a {@link Builder}.
     *
     * @param builderFactory A {link @Supplier} that should construct a
     *                       *fresh* instance of a {link @Builder}.
     * @param <Collection>   Any kind of collection that can take an
     *                       arbitrary number of elements of type {@Case}.
     * @return A {@link Trials} instance that yields collections.
     */
    <Collection> Trials<Collection> collections(
            Supplier<Builder<Case, Collection>> builderFactory);

    Trials<ImmutableList<Case>> immutableLists();

    Trials<ImmutableSet<Case>> immutableSets();

    Trials<ImmutableSortedSet<Case>> immutableSortedSets(
            final Comparator<Case> elementComparator);

    <Value> Trials<ImmutableMap<Case, Value>> immutableMaps(
            final Trials<Value> values);

    <Value> Trials<ImmutableSortedMap<Case, Value>> immutableSortedMaps(
            final Comparator<Case> elementComparator,
            final Trials<Value> values);

    /**
     * Transform this to a trials of collection, where {@link Collection} is
     * some kind of collection that can be built from elements of type
     * {@link Case} by a {@link Builder}. The collection instances yielded
     * by the result are all built from the specified number of elements.
     *
     * @param size           The number of elements of type {@Case} to build
     *                       the collection instance from. Be aware that
     *                       sets, maps and bounded size collection don't
     *                       have to accept that many elements.
     * @param builderFactory A {link @Supplier} that should construct a
     *                       *fresh* instance of a {link @Builder}.
     * @param <Collection>   Any kind of collection that can take an
     *                       arbitrary number of elements of type {@Case}.
     * @return A {@link Trials} instance that yields collections.
     */
    <Collection> Trials<Collection> collectionsOfSize(
            final int size, Supplier<Builder<Case, Collection>> builderFactory);

    Trials<ImmutableList<Case>> immutableListsOfSize(
            final int size);

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
    }

    interface SupplyToSyntaxTuple2<Case1, Case2>
            extends SupplyToSyntax<Tuple2<Case1, Case2>> {
        void supplyTo(BiConsumer<Case1, Case2> biConsumer);
    }

    interface SupplyToSyntaxTuple3<Case1, Case2, Case3>
            extends SupplyToSyntax<Tuple3<Case1, Case2, Case3>> {
        void supplyTo(Consumer3<Case1, Case2, Case3> triConsumer);
    }

    interface SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4>
            extends SupplyToSyntax<Tuple4<Case1, Case2, Case3, Case4>> {
        void supplyTo(Consumer4<Case1, Case2, Case3, Case4> quadConsumer);
    }

    interface Tuple2Trials<Case1, Case2> {
        <Case3> Trials.Tuple3Trials<Case1, Case2, Case3> and(
                Trials<Case3> thirdTrials);

        Trials.SupplyToSyntaxTuple2<Case1, Case2> withLimit(final int limit);

        Trials.SupplyToSyntaxTuple2<Case1, Case2> withRecipe(
                final String recipe);
    }

    interface Tuple3Trials<Case1, Case2, Case3> {
        <Case4> Trials.Tuple4Trials<Case1, Case2, Case3, Case4> and(
                Trials<Case4> fourthTrials);

        Trials.SupplyToSyntaxTuple3<Case1, Case2, Case3> withLimit(
                final int limit);

        Trials.SupplyToSyntaxTuple3<Case1, Case2, Case3> withRecipe(
                final String recipe);
    }

    interface Tuple4Trials<Case1, Case2, Case3, Case4> {
        Trials.SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4> withLimit(
                final int limit);

        Trials.SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4> withRecipe(
                final String recipe);
    }
}


        