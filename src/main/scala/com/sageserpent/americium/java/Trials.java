package com.sageserpent.americium.java;

import scala.NotImplementedError;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Trials<Case> {
    public abstract <TransformedCase> Trials<TransformedCase> map(Function<Case, TransformedCase> transform);

    public abstract <TransformedCase> Trials<TransformedCase> flatMap(Function<Case, Trials<TransformedCase>> step);

    public abstract Trials<Case> filter(Predicate<Case> predicate);

    public abstract static class TrialException extends RuntimeException {
        /**
         * @return The case that provoked the exception.
         */
        public abstract Object provokingCase();


        /**
         * @return A recipe that can be used to reproduce the provoking case
         * when supplied to the corresponding trials instance.
         */
        public abstract String recipe();
    }

    /**
     * Consume trial cases until either there are no more or an exception is thrown by {@code consumer}.
     * If an exception is thrown, attempts will be made to shrink the trial case that caused the
     * exception to a simpler case that throws an exception - the specific kind of exception isn't
     * necessarily the same between the first exceptional case and the final simplified one. The exception
     * from the simplified case (or the original exceptional case if it could not be simplified) is wrapped
     * in an instance of {@link TrialException} which also contains the case that provoked the exception.
     *
     * @param consumer An operation that consumes a 'Case', and may throw an exception.
     */
    public abstract void supplyTo(Consumer<? super Case> consumer);

    /**
     * Reproduce a specific case in a repeatable fashion, based on a recipe.
     *
     * @param recipe This encodes a specific case and will only be understood by the
     *               same *value* of trials instance that was used to obtain it.
     * @return The specific case denoted by the recipe.
     * @throws RuntimeException if the recipe does not correspond to the receiver,
     *                          either due to it being created by a different
     *                          flavour of trials instance or subsequent code changes.
     */
    public abstract Case reproduce(String recipe);

    /**
     * Consume the single trial case reproduced by a recipe. This is intended
     * to repeatedly run a test against a known failing case when debugging, so
     * the expectation is for this to *eventually* not throw an exception after
     * code changes are made in the system under test.
     *
     * @param recipe   This encodes a specific case and will only be understood by the
     *                 same *value* of trials instance that was used to obtain it.
     * @param consumer An operation that consumes a 'Case', and may throw an exception.
     * @throws RuntimeException if the recipe is not one corresponding to the receiver,
     *                          either due to it being created by a different flavour of trials instance.
     */
    public abstract void supplyTo(String recipe, Consumer<? super Case> consumer);

    public static <SomeCase> Trials<SomeCase> only(SomeCase onlyCase) {
        throw new NotImplementedError();
    }

    /**
     * Produce a trials instance that chooses between several cases.
     * <p>
     * NOTE: the peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     *
     * @param firstChoice  Mandatory first choice, so there is at least one case.
     * @param secondChoice Mandatory second choice, so there is always some element of choice.
     * @param otherChoices Optional further choices.
     * @return The trials instance.
     */
    public static <SomeCase> Trials<SomeCase> choose(SomeCase firstChoice,
                                                     SomeCase secondChoice,
                                                     SomeCase... otherChoices) {
        throw new NotImplementedError();
    }

    public static <SomeCase> Trials<SomeCase> choose(Iterable<SomeCase> choices) {
        throw new NotImplementedError();
    }

    public static <SomeCase> Trials<SomeCase> choose(SomeCase[] choices) {
        throw new NotImplementedError();
    }

    /**
     * Produce a trials instance that alternates between the cases of the given alternatives.
     * <p>
     * NOTE: the peculiar signature is to avoid ambiguity with the overloads for an iterable / array of cases.
     *
     * @param firstAlternative  Mandatory first alternative, so there is at least one trials.
     * @param secondAlternative Mandatory second alternative, so there is always some element of choice.
     * @param otherAlternatives Optional further alternatives.
     * @return The trials instance.
     */
    public static <SomeCase> Trials<SomeCase> alternate(Trials<? extends SomeCase> firstAlternative,
                                                        Trials<? extends SomeCase> secondAlternative,
                                                        Trials<? extends SomeCase>... otherAlternatives) {
        throw new NotImplementedError();
    }

    public static <SomeCase> Trials<SomeCase> alternate(Iterable<Trials<SomeCase>> alternatives) {
        throw new NotImplementedError();
    }

    public static <SomeCase> Trials<SomeCase> alternate(Trials<SomeCase>[] alternatives) {
        throw new NotImplementedError();
    }
}