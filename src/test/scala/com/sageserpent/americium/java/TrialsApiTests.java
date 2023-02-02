package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import cyclops.control.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrialsApiTests {
    private final static TrialsApi api = Trials.api();

    static Iterator<ImmutableSet<String>> sets() {
        return JUnit5Provider.of(30, api.strings().immutableSets());
    }

    static Iterator<Arguments> mixtures() {
        return JUnit5Provider.of(32,
                                 api.integers(),
                                 api.strings().immutableMaps(api.booleans()));
    }

    public static CasesLimitStrategy oneSecond(CaseSupplyCycle unused) {
        return CasesLimitStrategy.timed(Duration.ofSeconds(1));
    }

    Trials<String> chainedIntegers() {
        return api.alternate(
                api.integers()
                   .flatMap(value -> chainedIntegers()
                           .map(chain -> String.join(",",
                                                     value.toString(),
                                                     chain))),
                api.only("*** END ***"));
    }

    Trials<String> chainedBooleansAndIntegersInATree() {
        return api.alternate(
                api.delay(this::chainedBooleansAndIntegersInATree)
                   .flatMap(left -> api.booleans()
                                       .flatMap(side -> chainedBooleansAndIntegersInATree()
                                               .map(right -> "(" + String.join(
                                                       ",",
                                                       left,
                                                       side.toString(),
                                                       right) + ")"))),
                api.integers().map(Object::toString));
    }

    Trials<String> chainedIntegersUsingAnExplicitTerminationCase() {
        return api.booleans().flatMap(terminate ->
                                              terminate
                                              ? api.only("*** END ***") : api
                                                      .integers()
                                                      .flatMap(value -> chainedIntegersUsingAnExplicitTerminationCase().map(
                                                              simplerCase -> String.join(
                                                                      ",",
                                                                      value.toString(),
                                                                      simplerCase))));
    }

    @Test
    void testDrivePureJavaApi() {
        final Trials<Integer> integerTrials = api.only(1);
        final Trials<Double> doubleTrials =
                api.choose(new Double[]{1.2, 5.6, 0.1 + 0.1 + 0.1})
                   .map(value -> 10 * value);
        final BigDecimal oneTenthAsABigDecimal =
                BigDecimal.ONE.divide(BigDecimal.TEN);
        final Trials<BigDecimal> bigDecimalTrials =
                api
                        .only(oneTenthAsABigDecimal
                                      .add(oneTenthAsABigDecimal)
                                      .add(oneTenthAsABigDecimal))
                        .map(value -> value.multiply(BigDecimal.TEN));

        final Trials<Number> alternateTrials =
                api.alternate(integerTrials, doubleTrials, bigDecimalTrials);

        final int limit = 20;

        alternateTrials.withLimit(limit).supplyTo(number -> {
            System.out.println(number.doubleValue());
        });

        final Trials<Number> alternateTrialsFromArray =
                api.alternate(new Trials[]{integerTrials, doubleTrials,
                                           bigDecimalTrials});

        alternateTrialsFromArray.withLimit(limit).supplyTo(number -> {
            System.out.println(number.doubleValue());
        });

        System.out.println("Chained integers...");

        chainedIntegers().withLimit(limit).supplyTo(System.out::println);

        System.out.println(
                "Chained integers using an explicit termination case...");

        chainedIntegersUsingAnExplicitTerminationCase()
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("Chained integers and Booleans in a tree...");

        chainedBooleansAndIntegersInATree()
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A list of doubles...");

        doubleTrials
                .immutableLists()
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A set of doubles...");

        doubleTrials
                .immutableSets()
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A sorted set of doubles...");

        doubleTrials
                .immutableSortedSets(Double::compareTo)
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A sorted set of doubles via the generic method...");

        doubleTrials
                .collections(() -> new Builder<Double, SortedSet<Double>>() {
                    final SortedSet<Double> sortedSet = new TreeSet<>();

                    @Override
                    public void add(Double caze) {
                        sortedSet.add(caze);
                    }

                    @Override
                    public SortedSet<Double> build() {
                        return sortedSet;
                    }
                })
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A map of strings keyed by integers...");

        Trials<Integer> integersTrialsWithVariety = api.choose(1, 2, 3);

        integersTrialsWithVariety
                .immutableMaps(api.strings())
                .withLimit(limit)
                .supplyTo(System.out::println);

        System.out.println("A sorted map of strings keyed by integers...");

        integersTrialsWithVariety
                .immutableSortedMaps(Integer::compare, api.strings())
                .withLimit(limit)
                .supplyTo(System.out::println);
    }

    @ParameterizedTest
    @MethodSource(value = "sets")
    void testDriveSetsProvider(ImmutableSet<String> distinctStrings) {
        System.out.println(distinctStrings);
    }

    @ParameterizedTest
    @MethodSource(value = "mixtures")
    void testDriveMixturesProvider(int integer,
                                   Map<String, Boolean> dictionary) {
        System.out.printf("%d, %s%n", integer, dictionary);
    }

    @Test
    void testDriveInlinedFiltration() {
        api
                .integers()
                .immutableSets()
                .withLimit(100)
                .supplyTo(setOfIntegers -> {
                    final boolean satisfiedPrecondition = setOfIntegers
                            .stream()
                            .allMatch(value -> 0 == value % 2);
                    Trials.whenever(satisfiedPrecondition, () -> {
                        System.out.println(setOfIntegers);
                        assertThat("All members of the set are even",
                                   satisfiedPrecondition);
                    });
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10})
    void testDriveListTrialsFromListOfTrials(int numberOfElements) {
        final Trials<ImmutableList<Integer>> lists;

        {
            final ImmutableList.Builder<Trials<Integer>> builder =
                    ImmutableList.builder();

            for (int size = 1; numberOfElements >= size; ++size) {
                builder.add(api.choose(Stream
                                               .iterate(0,
                                                        previous -> 1 +
                                                                    previous)
                                               .limit(size)
                                               .toArray(Integer[]::new)));
            }

            lists = api.immutableLists(builder.build());
        }

        lists.withLimit(100).supplyTo(list -> {
            assertThat("The size of the list should be number of element " +
                       "trials",
                       list.size(),
                       equalTo(numberOfElements));

            System.out.println(list);

            for (int index = 0; numberOfElements > index; ++index) {
                assertThat("The range should not exceed the index",
                           list.get(index),
                           lessThanOrEqualTo(index));
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10})
    void testDriveCollectionTrialsFromListOfTrials(int numberOfElements) {
        final Trials<ImmutableList<Integer>> lists;

        {
            final ImmutableList.Builder<Trials<Integer>> builder =
                    ImmutableList.builder();

            for (int size = 1; numberOfElements >= size; ++size) {
                builder.add(api.choose(Stream
                                               .iterate(0,
                                                        previous -> 1 +
                                                                    previous)
                                               .limit(size)
                                               .toArray(Integer[]::new)));
            }

            lists = api.collections(builder.build(), () -> new Builder<Integer,
                    ImmutableList<Integer>>() {
                final ImmutableList.Builder<Integer> underlyingBuilder =
                        ImmutableList.builder();

                @Override
                public void add(Integer caze) {
                    underlyingBuilder.add(caze);
                }

                @Override
                public ImmutableList<Integer> build() {
                    return underlyingBuilder.build();
                }
            });
        }

        lists.withLimit(100).supplyTo(list -> {
            assertThat("The size of the list should be number of element " +
                       "trials",
                       list.size(),
                       equalTo(numberOfElements));

            System.out.println(list);

            for (int index = 0; numberOfElements > index; ++index) {
                assertThat("The range should not exceed the index",
                           list.get(index),
                           lessThanOrEqualTo(index));
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 100000})
    void testDriveSizedListTrials(int numberOfElements) {
        final Trials<ImmutableList<Integer>> lists =
                api.integers().immutableListsOfSize(numberOfElements);

        lists.withLimit(100).supplyTo(list -> {
            assertThat("The size of the list should be number of element " +
                       "trials",
                       list.size(),
                       equalTo(numberOfElements));

            System.out.println(list);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 100000})
    void testDriveSizedCollectionTrials(int numberOfElements) {
        final Trials<List<Integer>> lists =
                api
                        .integers()
                        .collectionsOfSize(numberOfElements,
                                           () -> new Builder<Integer,
                                                   List<Integer>>() {
                                               final List<Integer> list =
                                                       new LinkedList<>();

                                               @Override
                                               public void add(Integer caze) {
                                                   list.add(caze);
                                               }

                                               @Override
                                               public List<Integer> build() {
                                                   return list;
                                               }
                                           });

        lists.withLimit(100).supplyTo(list -> {
            assertThat("The size of the list should be number of element " +
                       "trials",
                       list.size(),
                       equalTo(numberOfElements));

            System.out.println(list);
        });
    }

    @Test
    void testDriveCombinationsOfTrials() {
        api.integers()
           .and(api.strings())
           .and(api.booleans().immutableSets())
           .and(api.characters().immutableListsOfSize(4))
           .withLimit(100)
           .supplyTo((first, second, third, fourth) ->
                             System.out.printf("%s, %s, %s, %s%n",
                                               first,
                                               second,
                                               third,
                                               fourth));
    }

    @Test
    void reproduceFailureInvolvingCombinationsOfTrials() {
        final TrialsScaffolding.Tuple4Trials<Integer, String,
                ImmutableSet<Boolean>, String>
                combinationOfTrials = api.integers()
                                         .and(api.strings())
                                         .and(api.booleans().immutableSets())
                                         .and(api.characters()
                                                 .collectionsOfSize(4,
                                                                    () -> Builder.stringBuilder()));

        final Try<Void, TrialsFactoring.TrialException> shouldHarbourAnError =
                Try.runWithCatch(() ->
                                 {
                                     combinationOfTrials
                                             .withLimit(100)
                                             .supplyTo(this::thisShouldFail);
                                 }, TrialsFactoring.TrialException.class);

        // If this fails, it is because the test's own logic is incorrect,
        // therefore we don't have any expectations here.
        final TrialsFactoring.TrialException trialException =
                shouldHarbourAnError.failureGet().toOptional().get();

        System.out.println("Now to reproduce the failure...");

        final Try<Void, TrialsFactoring.TrialException> mustHarbourAnError =
                Try.runWithCatch(() -> {
                    combinationOfTrials
                            .withRecipe(trialException.recipe())
                            .supplyTo(this::thisShouldFail);
                }, TrialsFactoring.TrialException.class);

        assertThat("This should have reproduced a failure.",
                   mustHarbourAnError.onFail(exception -> {
                       assertThat(exception.provokingCase(),
                                  equalTo(trialException.provokingCase()));
                       assertThat(exception.recipe(),
                                  equalTo(trialException.recipe()));
                       assertThat(exception.getMessage(),
                                  equalTo(trialException.getMessage()));
                   }).isFailure());
    }

    private void thisShouldFail(Integer first, String second,
                                ImmutableSet<Boolean> third, String fourth) {
        final int
                numberOfBitsForHashCode =
                Hashing.murmur3_32()
                       .newHasher()
                       .putInt(first)
                       .putUnencodedChars(
                               second)
                       .putObject(
                               third,
                               (from, into) -> {
                                   from
                                           .iterator()
                                           .forEachRemaining(
                                                   into::putBoolean);
                               })
                       .putUnencodedChars(
                               fourth)
                       .hash()
                       .bits();

        try {
            // A questionable assertion that we expect will fail...
            assertThat(
                    numberOfBitsForHashCode,
                    lessThanOrEqualTo(
                            31));
        } catch (Throwable throwable) {
            System.out.format(
                    "%d, %s, %s, %s\n",
                    first,
                    second,
                    third,
                    fourth);
            throw throwable;
        }
    }

    @Test
    void instantsCanBeShrunkToo() {
        {
            final Instant lowerBound = Instant.EPOCH.minusSeconds(25);
            final Instant upperBound = Instant.EPOCH;

            final Trials<Instant> instants =
                    api.instants(lowerBound,
                                 upperBound);

            instants.withLimit(10).supplyTo(instant -> {
                assertThat(instant, greaterThanOrEqualTo(lowerBound));
                assertThat(instant, lessThanOrEqualTo(upperBound));
            });

            final TrialsFactoring.TrialException trialException = assertThrows(
                    TrialsFactoring.TrialException.class,
                    () -> instants.withLimit(10).supplyTo(instant -> {
                        assertThat(instant, greaterThanOrEqualTo(lowerBound));
                        assertThat(instant, lessThanOrEqualTo(upperBound));
                        throw new RuntimeException();
                    }));

            assertThat(trialException.provokingCase(),
                       equalTo(Instant.EPOCH));
        }

        {
            final Instant lowerBound = Instant.parse("2007-12-03T10:15:30.00Z");
            final Instant upperBound = Instant.parse("2007-12-03T10:15:40.00Z");

            final Trials<Instant> instants =
                    api.instants(lowerBound,
                                 upperBound);

            instants.withLimit(10).supplyTo(instant -> {
                assertThat(instant, greaterThanOrEqualTo(lowerBound));
                assertThat(instant, lessThanOrEqualTo(upperBound));
            });

            final TrialsFactoring.TrialException trialException = assertThrows(
                    TrialsFactoring.TrialException.class,
                    () -> instants.withLimit(10).supplyTo(instant -> {
                        assertThat(instant, greaterThanOrEqualTo(lowerBound));
                        assertThat(instant, lessThanOrEqualTo(upperBound));
                        throw new RuntimeException();
                    }));

            assertThat(trialException.provokingCase(),
                       equalTo(lowerBound));
        }

        {
            final Instant lowerBound = Instant.parse("2007-12-03T10:15:30.00Z");
            final Instant upperBound = Instant.parse("2007-12-03T10:15:40.00Z");
            final Instant shrinkageTarget =
                    Instant.parse("2007-12-03T10:15:30.12Z");

            final Trials<Instant> instants =
                    api.instants(lowerBound,
                                 upperBound,
                                 shrinkageTarget);

            instants.withLimit(10).supplyTo(instant -> {
                assertThat(instant, greaterThanOrEqualTo(lowerBound));
                assertThat(instant, lessThanOrEqualTo(upperBound));
            });

            final TrialsFactoring.TrialException trialException = assertThrows(
                    TrialsFactoring.TrialException.class,
                    () -> instants.withLimit(10).supplyTo(instant -> {
                        assertThat(instant, greaterThanOrEqualTo(lowerBound));
                        assertThat(instant, lessThanOrEqualTo(upperBound));
                        throw new RuntimeException();
                    }));

            assertThat(trialException.provokingCase(),
                       equalTo(shrinkageTarget));
        }
    }

    @ParameterizedTest
    @CsvSource(
            nullValues = {"None"},
            value = {"0.0, 0.0, None",
                     "0.0, 0.0, 0.0",

                     "123.456789, 123.456789, None",
                     "123.456789, 123.456789, 123.456789",

                     "-123.456789, -123.456789, None",
                     "-123.456789, -123.456789, -123.456789",

                     "0.0, 4.9e-324, None",
                     "0.0, 4.9e-324, 0.0",
                     "0.0, 4.9e-324, 4.9e-324",

                     "-4.9e-324, 0.0, None",
                     "-4.9e-324, 0.0, -4.9e-324",
                     "-4.9e-324, 0.0, 0.0",

                     "-4.9e-324, 4.9e-324, None",
                     "-4.9e-324, 4.9e-324, -4.9e-324",
                     "-4.9e-324, 4.9e-324, 0.0",
                     "-4.9e-324, 4.9e-324, 4.9e-324",

                     "0.0, 123.456789, None",
                     "0.0, 123.456789, 0.0",
                     "0.0, 123.456789, 1.234567",
                     "0.0, 123.456789, 123.456789",
                     "0.0, 123.456789, 123",

                     "-123.456789, 0.0, None",
                     "-123.456789, 0.0, 0.0",
                     "-123.456789, 0.0, -1.234567",
                     "-123.456789, 0.0, -123.456789",
                     "-123.456789, 0.0, -123",

                     "-123.456789, 123.456789, None",
                     "-123.456789, 123.456789, 0.0",
                     "-123.456789, 123.456789, 1.234567",
                     "-123.456789, 123.456789, 123.456789",
                     "-123.456789, 123.456789, 123",
                     "-123.456789, 123.456789, -1.234567",
                     "-123.456789, 123.456789, -123.456789",
                     "-123.456789, 123.456789, -123",

                     "-12345678.9, 12345678.9, None",
                     "-12345678.9, 12345678.9, 0.0",
                     "-12345678.9, 12345678.9, 1.234567",
                     "-12345678.9, 12345678.9, 12345678.9",
                     "-12345678.9, 12345678.9, 968676",
                     "-12345678.9, 12345678.9, -1.234567",
                     "-12345678.9, 12345678.9, -12345678.9",
                     "-12345678.9, 12345678.9, -968676",

                     "-0.123456789, 0.123456789, None",
                     "-0.123456789, 0.123456789, 0.0",
                     "-0.123456789, 0.123456789, 0.1234567",
                     "-0.123456789, 0.123456789, 0.123456789",
                     "-0.123456789, 0.123456789, 0.123",
                     "-0.123456789, 0.123456789, -0.1234567",
                     "-0.123456789, 0.123456789, -0.123456789",
                     "-0.123456789, 0.123456789, -0.123"})
    void testDoubleCases(double lowerBound, double upperBound,
                         Double shrinkageTarget) {
        final Trials<Double> doubles = Optional
                .ofNullable(shrinkageTarget)
                .map(target -> api.doubles(lowerBound, upperBound, target))
                .orElse(api.doubles(lowerBound, upperBound));

        doubles.withLimit(100).supplyTo(aDouble -> {
            assertThat(aDouble, greaterThanOrEqualTo(lowerBound));
            assertThat(aDouble, lessThanOrEqualTo(upperBound));

            System.out.format("Lower bound: %f, case: %f, upper bound: %f\n",
                              lowerBound,
                              aDouble,
                              upperBound);
        });

        final TrialsFactoring.TrialException trialException = assertThrows(
                TrialsFactoring.TrialException.class,
                () -> doubles.withLimit(100).supplyTo(aDouble -> {
                    assertThat(aDouble, greaterThanOrEqualTo(lowerBound));
                    assertThat(aDouble, lessThanOrEqualTo(upperBound));
                    throw new RuntimeException();
                }));

        Optional.ofNullable(shrinkageTarget).ifPresentOrElse(target -> {
            assertThat((double) trialException.provokingCase(),
                       closeTo(target, 1e-5));
        }, () -> {
            if (0.0 < lowerBound) {
                assertThat(trialException.provokingCase(), equalTo(lowerBound));
            } else if (0.0 > upperBound) {
                assertThat(trialException.provokingCase(), equalTo(upperBound));
            } else {
                assertThat(trialException.provokingCase(), equalTo(0.0));
            }
        });


    }

    @Test
    void testAdditionalLimits() {
        final int bestPossibleShrinkage = 1;

        {
            final TrialsFactoring.TrialException trialException =
                    assertThrows(TrialsFactoring.TrialException.class, () -> api
                            .integers()
                            .withLimits(100, Trials.OptionalLimits.defaults)
                            .supplyTo(caze -> {
                                if (1 == caze % 2) {
                                    throw new RuntimeException();
                                }
                            }));

            assertThat(trialException.provokingCase(),
                       equalTo(bestPossibleShrinkage));
        }

        {
            final int upperBoundOfFinalShrunkCase = 50;

            final TrialsFactoring.TrialException trialException =
                    assertThrows(TrialsFactoring.TrialException.class, () -> api
                            .integers()
                            .withLimits(100,
                                        Trials.OptionalLimits.defaults,
                                        () -> caze ->
                                                upperBoundOfFinalShrunkCase >=
                                                caze)
                            .supplyTo(caze -> {
                                if (1 == caze % 2) {
                                    throw new RuntimeException();
                                }
                            }));

            assertThat((int) trialException.provokingCase(),
                       allOf(lessThanOrEqualTo(upperBoundOfFinalShrunkCase),
                             greaterThan(bestPossibleShrinkage)));
        }

        {
            final int upperBoundOfFinalShrunkCase = 50;

            final TrialsFactoring.TrialException trialException =
                    assertThrows(TrialsFactoring.TrialException.class, () -> api
                            .integers()
                            .withLimits(100,
                                        Trials.OptionalLimits
                                                .builder()
                                                .shrinkageAttempts(0)
                                                .build())
                            .supplyTo(caze -> {
                                if (1 == caze % 2) {
                                    throw new RuntimeException();
                                }
                            }));

            assertThat((int) trialException.provokingCase(),
                       greaterThan(upperBoundOfFinalShrunkCase));
        }
    }

    @Test
    void customCasesLimitStrategy(@Mock BiConsumer<Integer, Integer> consumer) {
        final Function<CaseSupplyCycle, CasesLimitStrategy>
                casesLimitStrategyFactory =
                TrialsApiTests::oneSecond;

        final int highestMagnitude = 100;

        api
                .integers(-highestMagnitude, 2 * highestMagnitude)
                .filter(value -> highestMagnitude >= Math.abs(value))
                .and(api.integers())
                .withStrategy(casesLimitStrategyFactory,
                              TrialsScaffolding.OptionalLimits.defaults)
                .supplyTo(consumer);

        int countDown = highestMagnitude;

        do {
            verify(consumer, atLeast(2)).accept(eq(countDown),
                                                anyInt());
        } while (0 < countDown--);

        verify(consumer, atLeast(1)).accept(eq(0), anyInt());
    }
}
