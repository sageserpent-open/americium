package com.sageserpent.americium.java;

import com.google.common.base.Verify;

public abstract class CaseFactory<Case> {
    {
        Verify.verify(lowerBoundInput() <= maximallyShrunkInput());
        Verify.verify(maximallyShrunkInput() <= upperBoundInput());
    }

    public abstract Case apply(long input);

    public abstract long lowerBoundInput();

    public abstract long upperBoundInput();

    public abstract long maximallyShrunkInput();
}