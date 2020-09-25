package com.sageserpent.americium;

import scala.NotImplementedError;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Trials {
    public interface Trial<Case> {
        <TransformedCase> Trial<TransformedCase> map(Function<Case, TransformedCase> transform);

        <TransformedCase> Trial<TransformedCase> flatMap(Function<Case, Trial<TransformedCase>> step);

        Trial<Case> filter(Predicate<Case> predicate);

        class TrialException extends RuntimeException{

        }

        /**
         * Consume trial cases until either there are no more or an exception is thrown by {@code consumer}.
         * If an exception is thrown, attempts will be made to shrink the trial case that caused the
         * exception to a simpler case that throws an exception - the specific kind of exception isn't
         * necessarily the same between the first exceptional case and the final simplified one. The exception
         * from the simplified case (or the original exceptional case if it could not be simplified) is wrapped
         * in an instance of {@link TrialException} which also contains the corresponding exceptional trial.
         *
         * @param consumer An operation that consumes a 'Case', and may throw an exception.
         */
        void yieldOrSimplifyExceptionalCase(Consumer<Case> consumer);
    }

    public static <Case> Trial<Case> constant(Case value) {
        throw new NotImplementedError();
    }

    public static <Case> Trial<Case> choose(Case... choices) {
        throw new NotImplementedError();
    }

    public static <Case> Trial<Case> choose(Iterable<Case> choices) {
        throw new NotImplementedError();
    }

    public static <Case> Trial<Case> alternate(Trial<Case>... alternatives) {
        throw new NotImplementedError();
    }

    public static <Case> Trial<Case> alternate(Iterable<Trial<Case>> alternatives) {
        throw new NotImplementedError();
    }
}