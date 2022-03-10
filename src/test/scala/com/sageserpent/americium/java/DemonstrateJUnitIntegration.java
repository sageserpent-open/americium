package com.sageserpent.americium.java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static java.lang.Math.abs;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class DemonstrateJUnitIntegration {
    @BeforeEach
    void beforeEach() {
        System.out.println("Before each...");
    }

    @AfterEach
    void afterEach() {
        System.out.println("...after each.");
    }

    private static final Trials<Long> longs = Trials.api().longs();

    private static final Trials<String> strings =
            Trials.api().choose("Fred", "Harold", "Ethel");

    @TrialsTest(trials = "longs", casesLimit = 100)
    void testWithALong(Long longCase) {
        final boolean assumption = 0 != longCase % 2;

        assumeTrue(assumption);

        final boolean guardPrecondition = 5 != abs(longCase % 10);

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(assumption);
            assertTrue(guardPrecondition);
        });
    }

    @TrialsTest(trials = {"longs", "strings"}, casesLimit = 100)
    void testWithALongAndAString(Long longCase, String stringCase) {
        final boolean guardPrecondition =
                5 != abs(longCase % 10) && stringCase.contains("e");

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(guardPrecondition);
        });
    }
}
