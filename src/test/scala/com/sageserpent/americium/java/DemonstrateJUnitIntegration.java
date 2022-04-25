package com.sageserpent.americium.java;

import com.google.common.collect.ImmutableSet;
import com.sageserpent.americium.java.TrialsScaffolding.Tuple2Trials;
import com.sageserpent.americium.java.TrialsScaffolding.Tuple3Trials;
import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class DemonstrateJUnitIntegration {
    private static final TrialsApi api = Trials.api();
    private static final Trials<Long> longs = api.longs();
    private static final Trials<String> strings =
            api.choose("Fred", "Harold", "Ethel");
    private static final Trials<String> first =
            api.integers(1, 10)
               .flatMap(size -> api
                       .characters('a', 'z', 'a')
                       .collectionsOfSize(size, Builder::stringBuilder))
               .filter(string -> string.endsWith("h"));
    private static final Trials<String> second =
            api.integers(0, 10)
               .flatMap(size -> api
                       .characters('0', '9', '0')
                       .collectionsOfSize(size, Builder::stringBuilder))
               .filter(string -> string.length() >= 1);
    private static final Trials<String> potentialNulls =
            api.alternate(api.strings(), api.only(null));

    private static final Tuple2Trials<String, Integer> pairs =
            api.strings().and(api.integers());

    private static final Tuple3Trials<Integer, Boolean, ImmutableSet<Double>>
            triples = api
            .choose(-326734, 8484)
            .and(api.booleans())
            .and(api.doubles().immutableSets());

    private static final Trials<Tuple3<Integer, Boolean, ImmutableSet<Double>>>
            plainTriples = triples.trials();

    @BeforeEach
    void beforeEach() {
        System.out.println("Before each...");
    }

    @AfterEach
    void afterEach() {
        System.out.println("...after each.");
    }

    @TrialsTest(trials = "longs", casesLimit = 100)
    void testWithALong(long longCase) {
        final boolean assumption = 0 != longCase % 2;

        assumeTrue(assumption);

        final boolean guardPrecondition = 5 != abs(longCase % 10);

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(assumption);
            assertTrue(guardPrecondition);
        });
    }

    @TrialsTest(trials = "longs", casesLimit = 100)
    void testWithABoxedLong(Long longCase) {
        final boolean assumption = 0 != longCase % 2;

        assumeTrue(assumption);

        final boolean guardPrecondition = 5 != abs(longCase % 10);

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(assumption);
            assertTrue(guardPrecondition);
        });
    }

    @TrialsTest(trials = {"longs", "strings"}, casesLimit = 100)
    void testWithALongAndAString(long longCase, String stringCase) {
        final boolean guardPrecondition =
                5 != abs(longCase % 10) && stringCase.contains("e");

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(guardPrecondition);
        });
    }

    @Disabled
    // This now detects the 'failing' test case correctly - but it is still a
    // test failure. Need to rethink what this test should look like....
    @TrialsTest(trials = {"first", "second"}, casesLimit = 125)
    void copiedFromJqwik(String first, String second) {
        System.out.format("%s, %s\n", first, second);
        final String concatenation = first + second;
        assertThat("Strings aren't allowed to be of length 4" +
                   " or 5 characters" + " in this test.",
                   4 > concatenation.length() ||
                   5 < concatenation.length());
    }


    @TrialsTest(trials = {"strings", "strings"}, casesLimit = 10)
    void allShouldBeWellWithRepeatedTrialsFields(String oneThing,
                                                 String another) {
    }

    @TrialsTest(trials = "potentialNulls", casesLimit = 10)
    void allShouldBeWellWithANullableParameter(String potentiallyThisIsANull) {
    }

    @TrialsTest(trials = {"potentialNulls", "potentialNulls"}, casesLimit = 10)
    void allShouldBeWellWithMultipleNullableParameters(
            String potentiallyThisIsANull,
            String potentiallyThisIsANullToo) {
    }

    @TrialsTest(trials = {"triples", "longs", "triples", "plainTriples",
                          "strings", "pairs"}, casesLimit = 10)
    void casesFromConjoinedTrialsAndTupleTrialsCanBeSuppliedToATestAsIndividualArguments(
            // From triples...
            int firstInOne, boolean secondInOne,
            ImmutableSet<Double> thirdInOne,
            // From longs...
            long two,
            // From triples...
            int firstInThree, Boolean secondInThree,
            ImmutableSet<Double> thirdInThree,
            // From plain triples...
            Integer firstInFour, boolean secondInFour,
            ImmutableSet<Double> thirdInFour,
            // From strings...
            String five,
            // From pairs...
            String firstInSix, int secondInSix) {
    }

    @TrialsTest(trials = {"triples", "longs", "triples", "plainTriples",
                          "strings", "pairs"}, casesLimit = 10)
    void casesFromConjoinedTrialsAndTupleTrialsCanBeSuppliedToATestAsTupledArguments(
            // From triples...
            Tuple3<Integer, Boolean, ImmutableSet<Double>> one,
            // From longs...
            long two,
            // From triples...
            Tuple3<Integer, Boolean, ImmutableSet<Double>> three,
            // From plain triples...
            Tuple3<Integer, Boolean, ImmutableSet<Double>> four,
            // From strings...
            String five,
            // From pairs...
            Tuple2<String, Integer> six) {
    }

    @TrialsTest(trials = {"triples", "longs", "triples", "plainTriples",
                          "strings", "pairs"}, casesLimit = 10)
    void casesFromConjoinedTrialsAndTupleTrialsCanBeSuppliedToATestAsAMixtureOfIndividualAndTupledArguments(
            // From triples...
            Tuple3<Integer, Boolean, ImmutableSet<Double>> one,
            // From longs...
            long two,
            // From triples...
            int firstInThree, Boolean secondInThree,
            ImmutableSet<Double> thirdInThree,
            // From plain triples...
            Tuple3<Integer, Boolean, ImmutableSet<Double>> four,
            // From strings...
            String five,
            // From pairs...
            String firstInSix, int secondInSix) {
    }
}
