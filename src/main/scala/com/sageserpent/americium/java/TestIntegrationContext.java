package com.sageserpent.americium.java;

public interface TestIntegrationContext<Case> {
    Case caze();

    CaseFailureReporting caseFailureReporting();

    InlinedCaseFiltration inlinedCaseFiltration();

    boolean isPartOfShrinkage();
}
