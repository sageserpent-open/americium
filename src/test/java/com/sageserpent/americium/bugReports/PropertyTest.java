package com.sageserpent.americium.bugReports;

import com.sageserpent.americium.java.Trials;
import com.sageserpent.americium.java.TrialsApi;
import com.sageserpent.americium.java.TrialsScaffolding;
import com.sageserpent.americium.java.junit5.ConfiguredTrialsTest;

/**
 * Bug report from: https://github.com/sageserpent-open/americium/issues/255
 * <p>
 * Both tests pass when run individually but, when the test class is run, the
 * second test fails with NoValidTrials
 *
 * <h3>To Reproduce:</h3>
 * <ul>
 *   <li>Run single test: {@code mvn test -Dtest=PropertyTest
 *   #positiveIntegersShouldBeGreaterThanZero} - PASSES ✓</li>
 *   <li>Run single test: {@code mvn test -Dtest=PropertyTest
 *   #positiveIntegersShouldBeLessThan100} - PASSES ✓</li>
 *   <li>Run both tests: {@code mvn test -Dtest=PropertyTest} - the first
 *   test passes but the second FAILS ✗</li>
 * </ul>
 *
 * <p><strong>Environment:</strong></p>
 * <ul>
 *   <li>Americium: 1.22.1 (americium_3)</li>
 *   <li>Java: 21</li>
 *   <li>JUnit: 5.14.0</li>
 *   <li>Maven Surefire: 3.2.5</li>
 * </ul>
 */
final class PropertyTest {

    private static final TrialsApi api = Trials.api();

    /**
     * Simple generator that produces positive integers.
     */
    private static final TrialsScaffolding.SupplyToSyntax<Integer>
            positiveIntegers =
            api.integers(1, 100).withLimit(50);

    @ConfiguredTrialsTest("positiveIntegers")
    void positiveIntegersShouldBeGreaterThanZero(Integer value) {
        if (value == null || value <= 0) {
            throw new AssertionError(
                    "Expected positive integer but got: " + value);
        }
    }

    @ConfiguredTrialsTest("positiveIntegers")
    void positiveIntegersShouldBeLessThan100(Integer value) {
        if (value == null || value > 100) {
            throw new AssertionError("Expected value <= 100 but got: " + value);
        }
    }
}
