package com.sageserpent.americium.java;

import java.math.BigInteger;

public abstract class CaseFactory<Case> {
    // TODO: find a new home for the invariant...
/*    {
        Verify.verify(0 >= lowerBoundInput().compareTo(maximallyShrunkInput()));
        Verify.verify(0 >= maximallyShrunkInput().compareTo(upperBoundInput()));
    }*/

    public abstract Case apply(BigInteger input);

    public abstract BigInteger lowerBoundInput();

    public abstract BigInteger upperBoundInput();

    public abstract BigInteger maximallyShrunkInput();
}