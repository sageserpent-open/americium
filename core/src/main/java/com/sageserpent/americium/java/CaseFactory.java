package com.sageserpent.americium.java;

import java.math.BigInteger;

public abstract class CaseFactory<Case> {
    public abstract Case apply(BigInteger input);

    public abstract BigInteger lowerBoundInput();

    public abstract BigInteger upperBoundInput();

    public abstract BigInteger maximallyShrunkInput();
}