package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.math.BigInteger;
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
     * @return A safe form of the {@code delayed} {@link Trials} instance
     * that won't
     * immediately execute, but will yield the same {@code Case} instances.
     */
    <Case> Trials<Case> delay(Supplier<Trials<Case>> delayed);

    /**
     * Make a {@link Trials} instance that only ever yields a single instance
     * of {@code Case}. Typically used with alternation to mix in some
     * important special case with say, a bunch of streamed cases, and also
     * used as a base case for recursively-defined trials.
     *
     * @param onlyCase
     * @param <Case>
     * @return A {@link Trials} instance that only ever yields {@code onlyCase}.
     */
    <Case> Trials<Case> only(Case onlyCase);

    /**
     * Denote a situation where no cases are possible. This is obviously an
     * obscure requirement - it is intended for sophisticated composition of
     * several {@link Trials} instances via nested flat-mapping where a
     * combination of the parameters from the prior levels of flat-mapping
     * cannot yield a test-case, possibly because of a precondition violation
     * or simply because the combination is undesirable for testing. <p>In
     * this situation one can detect such bad combinations and substitute an
     * impossible {@link Trials} instance.
     *
     * @param <Case>
     * @return A {@link Trials} instance that never yields any cases.
     */
    <Case> Trials<Case> impossible();

    /**
     * Produce a {@link Trials} instance that chooses between several cases.
     *
     * @param firstChoice  Mandatory first choice, so there is at least one
     *                     {@link Case}.
     * @param secondChoice Mandatory second choice, so there is always some
     *                     element of choice.
     * @param otherChoices Optional further choices.
     * @return A {@link Trials} instance.
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
     * Produce a {@link Trials} instance that alternates between the cases of
     * the
     * given alternatives.
     * <p>
     *
     * @param firstAlternative  Mandatory first alternative, so there is at
     *                          least one {@link Trials}.
     * @param secondAlternative Mandatory second alternative, so there is
     *                          always some element of alternation.
     * @param otherAlternatives Optional further alternatives.
     * @return A {@link Trials} instance.
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
     * the trials instance at the corresponding position within {@code
     * listOfTrials}.
     *
     * @param listOfTrials Several trials that act as sources for the
     *                     elements of lists yielded by the resulting
     *                     {@link Trials} instance.
     * @param <Case>       The type of the list elements yielded by the
     *                     resulting {@link Trials} instance.
     * @return A {@link Trials} instance that yields lists of the same size.
     */
    <Case> Trials<ImmutableList<Case>> immutableLists(
            List<Trials<Case>> listOfTrials);

    /**
     * Combine an iterable of trials instances into a single trials instance
     * that yields collections, where each collection is built from elements
     * taken in sequence from the corresponding trials instances in {@code
     * iterableOfTrials}. {@link Collection} is some kind of collection that
     * can be built from elements of type {@link Case} by a {@link Builder}.
     *
     * @param iterableOfTrials Several trials that act as sources for the
     *                         elements of collections yielded by the
     *                         resulting {link @Trials} instance. The assumption
     *                         is made that this can be traversed multiple
     *                         times and yield the same elements.
     * @param builderFactory   A {@link Supplier} that should construct a
     *                         *fresh* instance of a {@link Builder}.
     * @param <Case>           The type of the collection elements yielded by
     *                         the resulting {@link Trials} instance.
     * @param <Collection>     Any kind of collection that can take an
     *                         arbitrary number of elements of type
     *                         {@code Case}.
     * @return A {@link Trials} instance that yields collections.
     */
    <Case, Collection> Trials<Collection> collections(
            Iterable<Trials<Case>> iterableOfTrials,
            Supplier<Builder<Case, Collection>> builderFactory);

    /**
     * This is for advanced usage, where there is a need to control how
     * trials instances are formulated to avoid hitting the complexity limit,
     * or alternatively to control the amount of potentially unbounded
     * recursion when trials are recursively flat-mapped. If you don't know
     * what this means, you probably don't need this.
     * <p>
     * The notion of a complexity limit is described in
     * {@link TrialsScaffolding.SupplyToSyntax#withComplexityLimit(int)}
     *
     * @return A {@link Trials} instance yielding the complexity associated
     * with the definition's context, taking into account any flat-mapping
     * this call is embedded in.
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
     *                    that denotes a 'maximally shrunk' case.<p>The
     *                    factory is expected to be an injection, so it can
     *                    be fed with any potential long value from that
     *                    domain. It is not expected to be a surjection, so
     *                    distinct long values may result in equivalent cases.
     *                    <p>
     *                    It is expected that long values closer to the case
     *                    factory's maximally shrunk input yield 'smaller'
     *                    cases, in whatever sense is appropriate to either
     *                    the actual type of the cases or their specific use
     *                    as implemented by the factory.
     * @return A {@link Trials} instance.
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
     * @return A {@link Trials} instance.
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

    Trials<BigInteger> bigIntegers(BigInteger lowerBound,
                                   BigInteger upperBound);

    Trials<BigInteger> bigIntegers(BigInteger lowerBound, BigInteger upperBound,
                                   BigInteger shrinkageTarget);

    Trials<Double> doubles();

    Trials<Double> doubles(double lowerBound, double upperBound);

    Trials<Double> doubles(double lowerBound, double upperBound,
                           double shrinkageTarget);

    Trials<BigDecimal> bigDecimals(BigDecimal lowerBound,
                                   BigDecimal upperBound);

    Trials<BigDecimal> bigDecimals(BigDecimal lowerBound, BigDecimal upperBound,
                                   BigDecimal shrinkageTarget);

    Trials<Boolean> booleans();

    Trials<Character> characters();

    Trials<Character> characters(char lowerBound, char upperBound);

    Trials<Character> characters(char lowerBound, char upperBound,
                                 char shrinkageTarget);

    Trials<Instant> instants();

    Trials<Instant> instants(Instant lowerBound, Instant upperBound);

    Trials<Instant> instants(Instant lowerBound, Instant upperBound,
                             Instant shrinkageTarget);

    Trials<String> strings();

    /**
     * Produce a trials instance whose cases can be used to permute elements
     * of indexed collections, or as permutations of integers in their own
     * right.
     *
     * @param numberOfIndices The size of the set of indices <code>[0;
     *                        numberOfIndices)</code> that permutations are
     *                        generated from: also the size of the
     *                        permutations themselves.
     * @return A {@link Trials} instance whose cases are permutations of the
     * integer range
     * <code>[0; numberOfIndices)</code>.
     */
    Trials<List<Integer>> indexPermutations(int numberOfIndices);

    /**
     * Produce a trials instance whose cases can be used to permute elements
     * of indexed collections, or as permutations of integers in their own
     * right.
     *
     * @param numberOfIndices The size of the set of indices <code>[0;
     *                        numberOfIndices)</code> that permutations are
     *                        generated from.
     * @param permutationSize The size of the generated permutations; must be
     *                        in the range <code>[0; numberOfIndices)</code>
     * @return A {@link Trials} instance whose cases are permutations of the
     * integer range
     * <code>[0; numberOfIndices)</code>.
     */
    Trials<List<Integer>> indexPermutations(int numberOfIndices,
                                            int permutationSize);

    /**
     * Produce a trials instance whose cases can be used to select
     * combinations of elements of indexed collections, or as permutations of
     * integers in their own right.
     *
     * @param numberOfIndices The size of the set of indices <code>[0;
     *                        numberOfIndices)</code> that combinations are
     *                        generated from.
     * @param combinationSize The size of the generated combinations; must be
     *                        in the range <code>[0; numberOfIndices)</code>
     * @return A {@link Trials} instance whose cases are combinations of the
     * integer range <code>[0; numberOfIndices)</code>.
     */
    Trials<List<Integer>> indexCombinations(int numberOfIndices,
                                            int combinationSize);
}
