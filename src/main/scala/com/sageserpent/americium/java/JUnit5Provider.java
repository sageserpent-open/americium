package com.sageserpent.americium.java;

import org.junit.jupiter.params.provider.Arguments;

import java.util.Iterator;

public class JUnit5Provider {
    public static <Case> Iterator<? extends Case> of(int limit, Trials<Case> trials) {
        return trials.withLimit(limit).asIterator();
    }

    public static Iterator<Arguments> of(int limit, Trials<?>... trials) {
        return null;
    }
}
