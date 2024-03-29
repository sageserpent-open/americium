package com.sageserpent.americium.java.junit5;

import com.sageserpent.americium.java.Trials;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @deprecated This was a stop-gap measure allowing Americium to supply a
 * JUnit5 parameterised test using
 * {@link org.junit.jupiter.params.provider.MethodSource} to supply test
 * cases to a {@link org.junit.jupiter.params.ParameterizedTest}.
 * <p>
 * That approach works, but doesn't support shrinkage or the degree of
 * customisation afforded by {@link TrialsTest} or
 * {@link ConfiguredTrialsTest} - use these instead for a 'classic' JUnit5
 * experience, or consider using the more direct coupling offered by
 * {@link JUnit5#dynamicTests}
 */
@Deprecated
public class JUnit5Provider {
    public static <Case> Iterator<Case> of(int limit, Trials<Case> trials) {
        return trials.withLimit(limit).asIterator();
    }

    public static Iterator<Arguments> of(int limit, Trials<?>... trials) {
        class ContextCapture {
            public Trials<Supplier<Stream.Builder<Object>>> addTrialsAt(
                    int index,
                    Trials<Supplier<Stream.Builder<Object>>> partialResult) {
                return index < trials.length ? addTrialsAt(1 + index,
                                                           partialResult.flatMap(
                                                                   builder -> trials[index].map(
                                                                           caze -> () -> builder
                                                                                   .get()
                                                                                   .add(caze))))
                                             : partialResult;
            }
        }

        return of(limit,
                  new ContextCapture()
                          .addTrialsAt(0, Trials.api().only(Stream::builder))
                          .map(Supplier::get)
                          .map(Stream.Builder::build)
                          .map(Stream::toArray)
                          .map(Arguments::of));
    }
}
