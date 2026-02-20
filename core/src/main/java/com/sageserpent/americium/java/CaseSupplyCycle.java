package com.sageserpent.americium.java;

/**
 * Describes a cycle of supplying cases; intended for use in dynamically
 * configuring a {@link CasesLimitStrategy} to track shrinkage.
 */
public interface CaseSupplyCycle {
    /**
     * @return Whether the cycle is the initial one used to discover a test
     * failure prior to any subsequent shrinkage.
     */
    default boolean isInitial() {
        return 0 == numberOfPreviousCycles();
    }

    /**
     * @return The number of cycles run previously - will be zero if and only
     * if {@link #isInitial()} is true.
     */
    int numberOfPreviousCycles();

    /**
     * @return The number of failed trials seen in previous cycles.
     * @apiNote This can be lag behind
     * {@link CaseSupplyCycle#numberOfPreviousCycles()} if a cycle has not
     * encountered a failure, but the shrinkage wants to press on anyway.
     */
    int numberOfPreviousFailures();
}
