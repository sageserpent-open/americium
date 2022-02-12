package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import cyclops.control.Try;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TrialsApiTests {
    private final static TrialsApi api = Trials.api();

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

    static Iterator<ImmutableSet<String>> sets() {
        return JUnit5Provider.of(30, api.strings().immutableSets());
    }

    @ParameterizedTest
    @MethodSource(value = "sets")
    void testDriveSetsProvider(ImmutableSet<String> distinctStrings) {
        System.out.println(distinctStrings);
    }

    static Iterator<Arguments> mixtures() {
        return JUnit5Provider.of(32,
                                 api.integers(),
                                 api.strings().immutableMaps(api.booleans()));
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

            lists = api.lists(builder.build());
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

    private static final Trials<String> first =
            api.integers(1, 10)
               .flatMap(size -> api
                       .characters('a', 'z', 'a')
                       .collectionsOfSize(size, Builder::stringBuilder));

    private static final Trials<String> second =
            api.integers(0, 10)
               .flatMap(size -> api
                       .characters('0', '9', '0')
                       .collectionsOfSize(size, Builder::stringBuilder));

    @Disabled
    // This now detects the 'failing' test case correctly - but it is still a
    // test failure. Need to rethink what this test should look like....
    @Test
    void copiedFromJqwik() {
        first.and(second)
             .withLimit(50)
             .supplyTo((String first, String second) -> {
                 final String concatenation = first + second;
                 assertThat("Strings aren't allowed to be of length 4" +
                            " or 5 characters" + " in this test.",
                            4 > concatenation.length() ||
                            5 < concatenation.length());
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
}
