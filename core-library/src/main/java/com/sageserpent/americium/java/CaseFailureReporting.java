package com.sageserpent.americium.java;

@FunctionalInterface
public interface CaseFailureReporting {
    void report(Throwable throwable);
}
