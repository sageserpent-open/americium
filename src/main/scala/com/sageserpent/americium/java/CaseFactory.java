package com.sageserpent.americium.java;

import com.google.common.base.Verify;

public abstract class CaseFactory<Case> {
    {
        Verify.verify(lowerBoundInput() <= maximallyShrunkInput());
        Verify.verify(maximallyShrunkInput() <= upperBoundInput());
    }

    public abstract Case apply(int input);

    public abstract int lowerBoundInput();

    public abstract int upperBoundInput();

    public abstract int maximallyShrunkInput();
}