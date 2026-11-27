package com.sageserpent.americium.java;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NonEmptyCollectionsJavaTest {
    private final TrialsApi api = Trials.api();
    private final Trials<Integer> elementTrials = api.integers();

    @Test
    void nonEmptyCollectionsShouldYieldOnlyNonEmptyCollections() {
        elementTrials.nonEmptyCollections(() -> new Builder<Integer, List<Integer>>() {
            private final List<Integer> underlyingBuilder = new ArrayList<>();
            @Override
            public void add(Integer caze) {
                underlyingBuilder.add(caze);
            }
            @Override
            public List<Integer> build() {
                return underlyingBuilder;
            }
        }).withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(empty())));
        });
    }

    @Test
    void nonEmptyImmutableListsShouldYieldOnlyNonEmptyLists() {
        elementTrials.nonEmptyImmutableLists().withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(empty())));
        });
    }

    @Test
    void nonEmptyImmutableSetsShouldYieldOnlyNonEmptySets() {
        elementTrials.nonEmptyImmutableSets().withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(empty())));
        });
    }

    @Test
    void nonEmptyImmutableSortedSetsShouldYieldOnlyNonEmptySortedSets() {
        elementTrials.nonEmptyImmutableSortedSets(Comparator.naturalOrder()).withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(empty())));
        });
    }

    @Test
    void nonEmptyImmutableMapsShouldYieldOnlyNonEmptyMaps() {
        elementTrials.nonEmptyImmutableMaps(api.booleans()).withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(anEmptyMap())));
        });
    }

    @Test
    void nonEmptyImmutableSortedMapsShouldYieldOnlyNonEmptySortedMaps() {
        elementTrials.nonEmptyImmutableSortedMaps(Comparator.naturalOrder(), api.booleans()).withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(anEmptyMap())));
        });
    }

    @Test
    void nonEmptyStringsShouldYieldOnlyNonEmptyStrings() {
        api.nonEmptyStrings().withLimit(50).supplyTo(collection -> {
            assertThat(collection, is(not(emptyString())));
        });
    }
}
