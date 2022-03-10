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

    @TrialsTest
    void throwAnException(Long caze) {
        final boolean assumption = 0 != caze % 2;

        assumeTrue(assumption);

        final boolean guardPrecondition = 5 != abs(caze % 10);

        Trials.whenever(guardPrecondition, () -> {
            assertTrue(assumption);
            assertTrue(guardPrecondition);
        });
    }
}
