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


public abstract class Trials<Case> implements TrialsFactoring<Case> {
    public static TrialsApi api() {
        return (TrialsApi) TrialsImplementation.javaApi();
    }

    public static <Result> Result whenever(Boolean satisfiedPrecondition,
                                           Supplier<Result> block) {
        return com.sageserpent.americium.Trials.whenever(satisfiedPrecondition,
                                                         block::get);
    }

    public static void whenever(Boolean satisfiedPrecondition, Runnable block) {
        com.sageserpent.americium.Trials.whenever(satisfiedPrecondition, () -> {
            block.run();
            return null;
        });
    }

    abstract com.sageserpent.americium.Trials<Case> scalaTrials();

    public abstract <TransformedCase> Trials<TransformedCase> map(
            final Function<Case, TransformedCase> transform);

    public abstract <TransformedCase> Trials<TransformedCase> flatMap(
            final Function<Case, Trials<TransformedCase>> step);

    public abstract Trials<Case> filter(final Predicate<Case> predicate);

    public abstract <TransformedCase> Trials<TransformedCase> mapFilter(
            final Function<Case, Optional<TransformedCase>> filteringTransform);

    /**
     * Fluent syntax for configuring a limit to the number of cases
     * supplied to a consumer.
     *
     * @param limit The maximum number of cases that can be supplied - note
     *              that this is no guarantee that so many cases will be
     *              supplied, it is simply a limit.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     */
    public abstract Trials.SupplyToSyntax<Case> withLimit(final int limit);

    /**
     * Fluent syntax for configuring a limit to the number of cases
     * supplied to a consumer.
     *
     * @param limit          The maximum number of cases that can be supplied
     *                       - note that this is no guarantee that so many
     *                       cases will be supplied, it is simply a limit.
     * @param complexityWall <b>One past</b> the highest complexity that a
     *                       case may achieve as it is synthesized.
     * @return An instance of {@link SupplyToSyntax} with the limit configured.
     */
    public abstract Trials.SupplyToSyntax<Case> withLimit(
            final int limit, final int complexityWall);

    /**
     * Reproduce a trial case using a recipe. This is intended to repeatedly
     * run a test against a known failing case when debugging.
     *
     * @param recipe This encodes a specific {@code Case} and will only be
     *               understood by the
     *               same *value* of {@link Trials} that was used to obtain it.
     * @return An instance of {@link SupplyToSyntax} that supplies the
     * reproduced trial case.
     */
    public abstract Trials.SupplyToSyntax<Case> withRecipe(final String recipe);

    public abstract <Case2> Trials.Tuple2Trials<Case, Case2> and(
            Trials<Case2> secondTrials);

    /**
     * Transform this to a trials of collection, where {@link Collection} is
     * some kind of collection that can be built from elements of type
     * {@link Case} by a {@link Builder}.
     *
     * @param builderFactory A supplier that should construct a *fresh*
     *                       instance of a {@Builder}.
     * @param <Collection>   Any kind of collection that can take an
     *                       arbitrary number of elements of type {@Case}.
     * @return A trials instance that yields collections.
     */
    public abstract <Collection> Trials<Collection> collections(
            Supplier<Builder<Case, Collection>> builderFactory);

    public abstract Trials<ImmutableList<Case>> immutableLists();

    public abstract Trials<ImmutableSet<Case>> immutableSets();

    public abstract Trials<ImmutableSortedSet<Case>> immutableSortedSets(
            final Comparator<Case> elementComparator);

    public abstract <Value> Trials<ImmutableMap<Case, Value>> immutableMaps(
            final Trials<Value> values);

    public abstract <Value> Trials<ImmutableSortedMap<Case, Value>> immutableSortedMaps(
            final Comparator<Case> elementComparator,
            final Trials<Value> values);

    public abstract <Collection> Trials<Collection> collectionsOfSize(
            final int size, Supplier<Builder<Case, Collection>> builderFactory);

    public abstract Trials<ImmutableList<Case>> immutableListsOfSize(
            final int size);

    public interface SupplyToSyntax<Case> {
        /**
         * Consume trial cases until either there are no more or an exception
         * is thrown by {@code consumer}.
         * If an exception is thrown, attempts will be made to shrink the
         * trial case that caused the
         * exception to a simpler case that throws an exception - the
         * specific kind of exception isn't
         * necessarily the same between the first exceptional case and the
         * final simplified one. The exception
         * from the simplified case (or the original exceptional case if it
         * could not be simplified) is wrapped
         * in an instance of {@link TrialException} which also contains the
         * case that provoked the exception.
         *
         * @param consumer An operation that consumes a {@code Case}, and may
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

    public interface SupplyToSyntaxTuple2<Case1, Case2>
            extends SupplyToSyntax<Tuple2<Case1, Case2>> {
        void supplyTo(BiConsumer<Case1, Case2> biConsumer);
    }

    public interface SupplyToSyntaxTuple3<Case1, Case2, Case3>
            extends SupplyToSyntax<Tuple3<Case1, Case2, Case3>> {
        void supplyTo(Consumer3<Case1, Case2, Case3> triConsumer);
    }

    public interface SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4>
            extends SupplyToSyntax<Tuple4<Case1, Case2, Case3, Case4>> {
        void supplyTo(
                Consumer4<Case1, Case2, Case3, Case4> quadConsumer);
    }

    public interface Tuple2Trials<Case1, Case2> {
        <Case3> Trials.Tuple3Trials<Case1, Case2, Case3> and(
                Trials<Case3> thirdTrials);

        Trials.SupplyToSyntaxTuple2<Case1, Case2> withLimit(
                final int limit);

        Trials.SupplyToSyntaxTuple2<Case1, Case2> withRecipe(
                final String recipe);
    }

    public interface Tuple3Trials<Case1, Case2, Case3> {
        <Case4> Trials.Tuple4Trials<Case1, Case2, Case3, Case4> and(
                Trials<Case4> fourthTrials);

        Trials.SupplyToSyntaxTuple3<Case1, Case2, Case3> withLimit(
                final int limit);

        Trials.SupplyToSyntaxTuple3<Case1, Case2, Case3> withRecipe(
                final String recipe);
    }

    public interface Tuple4Trials<Case1, Case2, Case3, Case4> {
        Trials.SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4> withLimit(
                final int limit);

        Trials.SupplyToSyntaxTuple4<Case1, Case2, Case3, Case4> withRecipe(
                final String recipe);
    }
}


        