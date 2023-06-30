package com.sageserpent.americium.java.examples.junit5;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

class PoorQualitySetMembershipPredicate<Element extends Comparable<Element>>
        implements
        Predicate<Element> {
    // Need this workaround if running on Java < 11.
    private static final Comparable[] exemplar = {};

    private final Comparable[] elements;

    public PoorQualitySetMembershipPredicate(Collection<Element> elements) {
        this.elements = elements.toArray(exemplar);
    }

    @Override
    public boolean test(Element element) {
        return 0 <= Arrays.binarySearch(elements, element);
    }
}