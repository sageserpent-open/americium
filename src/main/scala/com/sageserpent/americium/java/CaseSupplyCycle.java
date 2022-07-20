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
        return 0 == numberOfPreviousShrinkages();
    }

    /**
     * @return The number of shrinkage cycles run previously - will be zero
     * if and only if {@link #isInitial()} is true.
     */
    int numberOfPreviousShrinkages();
}
