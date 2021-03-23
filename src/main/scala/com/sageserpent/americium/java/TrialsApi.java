package com.sageserpent.americium.java;

import java.util.function.Function;
import java.util.function.Supplier;

public interface TrialsApi {
    <Case> Trials<Case> delay(Supplier<Trials<Case>> delayed);

    <Case> Trials<Case> only(Case onlyCase);

    /**
     * Produce a trials instance that chooses between several cases.
     *
     * @param firstChoice  Mandatory first choice, so there is at least one {@code Case}.
     * @param secondChoice Mandatory second choice, so there is always some element of choice.
     * @param otherChoices Optional further choices.
     * @return The {@link Trials} instance.
     * @apiNote The peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     */
    <Case> Trials<Case> choose(Case firstChoice,
                               Case secondChoice,
                               Case... otherChoices);

    <Case> Trials<Case> choose(Iterable<Case> choices);

    <Case> Trials<Case> choose(Case[] choices);

    /**
     * Produce a trials instance that alternates between the cases of the given alternatives.
     * <p>
     *
     * @param firstAlternative  Mandatory first alternative, so there is at least one {@link Trials}.
     * @param secondAlternative Mandatory second alternative, so there is always some element of choice.
     * @param otherAlternatives Optional further alternatives.
     * @return The {@link Trials} instance.
     * @apiNote The peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     */
    <Case> Trials<Case> alternate(Trials<? extends Case> firstAlternative,
                                  Trials<? extends Case> secondAlternative,
                                  Trials<? extends Case>... otherAlternatives);

    <Case> Trials<Case> alternate(Iterable<Trials<Case>> alternatives);

    <Case> Trials<Case> alternate(Trials<Case>[] alternatives);

    /**
     * Produce a trials instance that stream cases from a factory.
     * <p>
     * This is used where we want to generate a supposedly potentially
     * unbounded number of cases, although there is an implied upper limit
     * based on the number of distinct long values in practice.
     *
     * @param factory Pure (in other words, stateless) function that produces
     *                a {@code Case} from a long value. Each call taking the same long
     *                value is expected to yield the same case. The factory is
     *                expected to be an injection, so it can be fed with any
     *                potential long value, negative, zero or positive. It is
     *                not expected to be a surjection, even if there are at most
     *                as many possible values of {@code Case} as there are long
     *                values, so distinct long values may result in equivalent cases.
     * @return The trials instance
     */
    <Case> Trials<Case> stream(Function<Long, Case> factory);

    default Trials<Integer> integers() {
        return stream(Object::hashCode);
    }

    default Trials<Long> longs() {
        return stream(Function.identity());
    }

    default Trials<Double> doubles() {
        return stream(Double::longBitsToDouble);
    }

    default Trials<Boolean> trueOrFalse() {
        return choose(true, false);
    }

    /**
     * Yields a *streaming* trials of true or false values.
     *
     * @return Either true or false.
     * @seealso {@link TrialsApi#trueOrFalse()}
     */
    default Trials<Boolean> coinFlip() {
        return stream(value -> 0 == value % 2);
    }
}
