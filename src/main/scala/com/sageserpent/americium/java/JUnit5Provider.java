package com.sageserpent.americium.java;

import java.util.Iterator;

public class JUnit5Provider {
    public static <Case> Iterator<? extends Case> of(Trials<Case> trials, int limit) {
        return trials.withLimit(limit).asIterator();
    }
}
