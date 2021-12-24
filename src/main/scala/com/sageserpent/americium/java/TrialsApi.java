package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TrialsApi {
    /**
     * Helper to break direct recursion when implementing a recursively
     * defined trials. You need this when your definition either doesn't have
     * a flatmap, or the first argument (the 'left hand side') of a flatmap is
     * where the recursion takes place. You won't need this very often, if at
     * all.
     *
     * @param delayed Some definition of a trials instance that is typically
     *                a recursive step - so you don't want it to execute
     *                there and then to avoid infinite recursion.
     * @param <Case>
     * @return A safe form of the {@code delayed} trials instance that won't
     * immediately execute, but will yield the same {@code Case} instances.
     */
    <Case> Trials<Case> delay(Supplier<Trials<Case>> delayed);

    /**
     * Make a {@link Trials} instance that only ever yields one instance of
     * {@code Case}. Typically used with alternation to mix in some important
     * special case with say, a bunch of streamed cases, and also used as a
     * base case for recursively-defined trials.
     *
     * @param onlyCase
     * @param <Case>
     * @return A {@link Trials} instance that only ever yields {@code onlyCase}.
     */
    <Case> Trials<Case> only(Case onlyCase);

    /**
     * Produce a trials instance that chooses between several cases.
     *
     * @param firstChoice  Mandatory first choice, so there is at least one
     *                     {@link Case}.
     * @param secondChoice Mandatory second choice, so there is always some
     *                     element of choice.
     * @param otherChoices Optional further choices.
     * @return The {@link Trials} instance.
     * @apiNote The peculiar signature is to avoid ambiguity with the
     * overloads for an iterable / array of cases.
     */
    <Case> Trials<Case> choose(Case firstChoice,
                               Case secondChoice,
                               Case... otherChoices);

    <Case> Trials<Case> choose(Iterable<Case> choices);

    <Case> Trials<Case> choose(Case[] choices);

    <Case> Trials<Case> chooseWithWeights(Map.Entry<Integer, Case> firstChoice,
                                          Map.Entry<Integer, Case> secondChoice,
                                          Map.Entry<Integer, Case>... otherChoices);

    <Case> Trials<Case> chooseWithWeights(
            Iterable<Map.Entry<Integer, Case>> choices);

    <Case> Trials<Case> chooseWithWeights(Map.Entry<Integer, Case>[] choices);

    /**
     * Produce a trials instance that alternates between the cases of the
     * given alternatives.
     * <p>
     *
     * @param firstAlternative  Mandatory first alternative, so there is at
     *                          least one {@link Trials}.
     * @param secondAlternative Mandatory second alternative, so there is
     *                          always some element of alternation.
     * @param otherAlternatives Optional further alternatives.
     * @return The {@link Trials} instance.
     * @apiNote The peculiar signature is to avoid ambiguity with the
     * overloads for an iterable / array of cases.
     */
    <Case> Trials<Case> alternate(Trials<? extends Case> firstAlternative,
                                  Trials<? extends Case> secondAlternative,
                                  Trials<? extends Case>... otherAlternatives);

    <Case> Trials<Case> alternate(Iterable<Trials<Case>> alternatives);

    <Case> Trials<Case> alternate(Trials<Case>[] alternatives);

    <Case> Trials<Case> alternateWithWeights(
            Map.Entry<Integer, Trials<? extends Case>> firstAlternative,
            Map.Entry<Integer, Trials<? extends Case>> secondAlternative,
            Map.Entry<Integer, Trials<? extends Case>>... otherAlternatives);

    <Case> Trials<Case> alternateWithWeights(
            Iterable<Map.Entry<Integer, Trials<Case>>> alternatives);

    <Case> Trials<Case> alternateWithWeights(
            Map.Entry<Integer, Trials<Case>>[] alternatives);

    /**
     * Combine a list of trials instances into a single trials instance that
     * yields lists, where those lists all have the size given by the number
     * of trials, and the element in each position in the list is provided by
     * the trials instance in the corresponding position in {@code
     * listOfTrials}.
     *
     * @param listOfTrials Several trials that act as sources for the
     *                     elements of lists yielded by the resulting
     *                     {@Trials} instance.
     * @param <Case>       The type of the list elements yielded by the
     *                     resulting {@Trials} instance.
     * @return A {@link Trials} instance that yields lists of the same size.
     */
    <Case> Trials<ImmutableList<Case>> lists(List<Trials<Case>> listOfTrials);

    /**
     * This is for advanced usage, where there is a need to control how
     * trials instances are formulated to avoid hitting the complexity limit,
     * or alternatively to control the amount of potentially unbounded
     * recursion when trials are recursively flat-mapped. If you don't know
     * what this means, you probably don't need this.
     * <p>
     * The notion of a complexity limit is described in
     * {@link Trials#withLimit(int, int)}
     *
     * @return The complexity associated with the trials context, taking into
     * account any flatmapping this call is embedded in.
     */
    Trials<Integer> complexities();

    /**
     * Produce a trials instance that stream cases from a factory.
     * <p>
     * This is used where we want to generate a supposedly potentially
     * unbounded number of cases, although there is an implied upper limit
     * based on the number of distinct long values in the factory's input
     * domain.
     *
     * @param caseFactory Pure (in other words, stateless) function that
     *                    produces a {@link Case} from a long value. Each
     *                    call taking the same long value is expected to
     *                    yield the same case. <p>Rather than {@link Function
     *                    }, the type {@link CaseFactory} is used here - this
     *                    allows the factory to declare its domain of valid
     *                    inputs, as well as the input value in that domain
     *                    that denotes a `maximally shrunk` case.<p>The
     *                    factory is expected to be an injection, so it can
     *                    be fed with any potential long value from that
     *                    domain. It is not expected to be a surjection, so
     *                    distinct long values may result in equivalent cases.
     *                    <p>
     *                    It is expected that long values closer to the
     *                    case factory's maximally shrunk input yield
     *                    smaller' cases, in whatever sense is appropriate to
     *                    either the actual type of the cases or their
     *                    specific use as implemented by the factory.
     * @return The trials instance
     */
    <Case> Trials<Case> stream(CaseFactory<Case> caseFactory);

    /**
     * Produce a trials instance that stream cases from a factory.
     * <p>
     * This is used where we want to generate a supposedly potentially
     * unbounded number of cases, although there is an implied upper limit
     * based on the number of distinct long values in practice.
     *
     * @param factory Pure (in other words, stateless) function that produces
     *                a {@code Case} from a long value. Each call taking the
     *                same long value is expected to yield the same case. The
     *                factory is expected to be an injection, so it can be
     *                fed with any potential long value, negative, zero or
     *                positive. It is not expected to be a surjection, even
     *                if there are at most as many possible values of {@code
     *                Case} as there are long values, so distinct long values
     *                may result in equivalent cases.
     *                <p>
     *                It is expected that long values closer to zero yield
     *                'smaller' cases, in whatever sense is appropriate to
     *                either the actual type of the cases or their specific
     *                use as encoded by the factory.
     * @return The trials instance
     */
    <Case> Trials<Case> streamLegacy(Function<Long, Case> factory);

    Trials<Byte> bytes();

    Trials<Integer> integers();

    Trials<Integer> integers(int lowerBound, int upperBound);

    Trials<Integer> integers(int lowerBound, int upperBound,
                             int shrinkageTarget);

    Trials<Integer> nonNegativeIntegers();

    Trials<Long> longs();

    Trials<Long> longs(long lowerBound, long upperBound);

    Trials<Long> longs(long lowerBound, long upperBound, long shrinkageTarget);

    Trials<Long> nonNegativeLongs();

    Trials<Double> doubles();

    Trials<Boolean> booleans();

    Trials<Character> characters();

    Trials<Instant> instants();

    Trials<String> strings();
}
