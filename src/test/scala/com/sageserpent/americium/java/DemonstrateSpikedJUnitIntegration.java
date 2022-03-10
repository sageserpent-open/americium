package com.sageserpent.americium.java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class DemonstrateSpikedJUnitIntegration {
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
        assumeTrue(0 != caze % 2);

        Trials.whenever(5 != abs(caze % 10), () -> {
        });
    }
}
