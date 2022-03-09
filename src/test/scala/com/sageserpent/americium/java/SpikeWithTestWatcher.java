package com.sageserpent.americium.java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class SpikeWithTestWatcher {
    @BeforeEach
    void beforeEach() {
        System.out.println("Before each...");
    }

    @AfterEach
    void afterEach() {
        System.out.println("...after each.");
    }

    @TestTemplate
    @ExtendWith(SpikeTestExtension.class)
    void throwAnException(Long caze) {
        assumeTrue(0 != caze % 2);

        if (5 == abs(caze % 10))
            throw new SpikeTestExtension.SpecialException();
    }
}
