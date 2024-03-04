package com.sageserpent.americium.java.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.util.Optional;

public class TestExecutionListenerCapturingUniqueIds implements
        TestExecutionListener {
    private static final ThreadLocal<UniqueId> uniqueId = new ThreadLocal<>();

    public static Optional<UniqueId> uniqueId() {
        return Optional.ofNullable(uniqueId.get());
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        TestExecutionListenerCapturingUniqueIds.uniqueId.set(testIdentifier.getUniqueIdObject());

        TestExecutionListener.super.dynamicTestRegistered(testIdentifier);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        TestExecutionListenerCapturingUniqueIds.uniqueId.remove();

        TestExecutionListener.super.executionSkipped(testIdentifier, reason);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult testExecutionResult) {
        TestExecutionListenerCapturingUniqueIds.uniqueId.remove();
        
        TestExecutionListener.super.executionFinished(testIdentifier,
                                                      testExecutionResult);
    }
}
