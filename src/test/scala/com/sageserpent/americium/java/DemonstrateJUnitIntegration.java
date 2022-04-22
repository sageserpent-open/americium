package com.sageserpent.americium.java;

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
}
