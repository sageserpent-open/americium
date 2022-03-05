package com.sageserpent.americium.java;

import com.google.common.collect.*;
import cyclops.control.Either;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Trials<Case> extends
        TrialsScaffolding<Case, TrialsScaffolding.SupplyToSyntax<Case>> {
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
        return com.sageserpent.americium.TrialsApis.javaApi();
    }

    /**
     * This is an alternative to calling {@link Trials#filter(Predicate)} -
     * the idea here is to embed calls to this method in the test itself as
     * an enclosing guard condition prior to executing the core testing code.
     * Usually the guard precondition would involve some check on the
     * supplied case and possibly some other inputs that come from elsewhere.
     * Like the use of filtration, this approach will interact correctly with
     * the shrinkage mechanism, which is why it is provided.
     *
     * @param guardPrecondition A precondition that must be satisfied to run
     *                          the test code.
     * @param block             The core testing code as a lambda.
     * @param <Result>
     * @return The result of successful execution of the test code.
     */
    static <Result> Result whenever(Boolean guardPrecondition,
                                    Supplier<Result> block) {
        return com.sageserpent.americium.Trials.whenever(guardPrecondition,
                                                         block::get);
    }

    /**
     * This is an alternative to calling {@link Trials#filter(Predicate)} -
     * the idea here is to embed calls to this method in the test itself as
     * an enclosing guard condition prior to executing the core testing code.
     * Usually the guard precondition would involve some check on the
     * supplied case and possibly some other inputs that come from elsewhere.
     * Like the use of filtration, this approach will interact correctly with
     * the shrinkage mechanism, which is why it is provided.
     *
     * @param guardPrecondition A precondition that must be satisfied to run
     *                          the test code.
     * @param block             The core testing code as a lambda.
     */
    static void whenever(Boolean guardPrecondition, Runnable block) {
        com.sageserpent.americium.Trials.whenever(guardPrecondition, () -> {
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

    // Monad syntax methods...

    <TransformedCase> Trials<TransformedCase> map(
            final Function<Case, TransformedCase> transform);

    <TransformedCase> Trials<TransformedCase> flatMap(
            final Function<Case, Trials<TransformedCase>> step);

    Trials<Case> filter(final Predicate<Case> predicate);

    <TransformedCase> Trials<TransformedCase> mapFilter(
            final Function<Case, Optional<TransformedCase>> filteringTransform);

    /**
     * Fluent syntax to allow trials to be combined prior to calling
     * {@link TrialsScaffolding#withLimits(int, OptionalLimits)} etc. This
     * grants the user the choice of either supplying the combined trials in
     * the usual way, in which case the {@link java.util.function.Consumer}
     * will take a {@link cyclops.data.tuple.Tuple2} parameterised by types
     * {@link Case} and {@link Case2}, or a
     * {@link java.util.function.BiConsumer} can be used taking separate
     * arguments of types {@link Case} and {@link Case2}.
     * <p>
     * This can be repeated up to a limit by calling {@code and} on the
     * results to add more trials - this enables supply to consumers of
     * higher argument arity.
     *
     * @param secondTrials
     * @param <Case2>
     * @return Syntax object that permits the test code to consume either a
     * pair or two separate arguments.
     */
    <Case2> Tuple2Trials<Case, Case2> and(
            Trials<Case2> secondTrials);

    /**
     * Fluent syntax to allow trials of *dissimilar* types to be supplied as
     * alternatives to the same test. In contrast to the
     * {@link TrialsApi#alternate(Trials, Trials, Trials[])}, the
     * alternatives do not have to conform to the same type; instead here we
     * can switch in the test between unrelated types using an {@link Either}
     * instance to hold cases from either this trials instance or {@code
     * alternativeTrials}.
     *
     * @param alternativeTrials
     * @param <Case2>
     * @return {@link Either} that is populated with either a {@link Case} or
     * a {@link Case2}.
     */
    <Case2> Trials<Either<Case, Case2>> or(Trials<Case2> alternativeTrials);

    /**
     * @return A lifted trials that wraps the underlying cases from this in
     * an {@link Optional}; the resulting trials also supplies a special case
     * of {@link Optional#empty()}.
     */
    Trials<Optional<Case>> optionals();

    /**
     * Transform this to a trials of collection, where {@link Collection} is
     * some kind of collection that can be built from elements of type
     * {@link Case} by a {@link Builder}.
     *
     * @param builderFactory A {@link Supplier} that should construct a
     *                       *fresh* instance of a {@link Builder}.
     * @param <Collection>   Any kind of collection that can take an
     *                       arbitrary number of elements of type {@link Case}.
     * @return A {@link Trials} instance that yields collections.
     */
    <Collection> Trials<Collection> collections(
            Supplier<Builder<Case, Collection>> builderFactory);

    // Convenience methods that enable supply of Guava collection cases...

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
     * @param size           The number of elements of type {@link Case} to
     *                       build the collection instance from. Be aware
     *                       that sets, maps and bounded size collection
     *                       don't have to accept that many elements.
     * @param builderFactory A {@link Supplier} that should construct a
     *                       *fresh* instance of a {@link Builder}.
     * @param <Collection>   Any kind of collection that can take an
     *                       arbitrary number of elements of type {@link Case}.
     * @return A {@link Trials} instance that yields collections.
     */
    <Collection> Trials<Collection> collectionsOfSize(
            final int size, Supplier<Builder<Case, Collection>> builderFactory);

    Trials<ImmutableList<Case>> immutableListsOfSize(
            final int size);
}


        