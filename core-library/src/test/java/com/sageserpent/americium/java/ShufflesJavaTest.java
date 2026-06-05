package com.sageserpent.americium.java;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ShufflesJavaTest {
    private final TrialsApi api = Trials.api();

    @Test
    void shufflesShouldYieldAPermutationOfTheOriginalItems() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        api.shuffles(items).withLimit(10).supplyTo(shuffled -> {
            assertThat(shuffled, containsInAnyOrder(items.toArray()));
            assertThat(shuffled, hasSize(items.size()));
        });
    }

    @Test
    void shufflesShouldEventuallyYieldAllPossibleShuffles() {
        List<Integer> items = Arrays.asList(1, 2, 3);
        int expectedNumberOfShuffles = 6; // 3!
        Set<List<Integer>> shuffles = StreamSupport.stream(((Iterable<List<Integer>>) () -> api.shuffles(items).withLimit(100).asIterator()).spliterator(), false)
                .collect(Collectors.toSet());
        assertThat(shuffles, hasSize(expectedNumberOfShuffles));
    }

    @Test
    void shufflesShouldHandleEmptyCollections() {
        List<Integer> items = Arrays.asList();
        api.shuffles(items).withLimit(1).supplyTo(shuffled -> {
            assertThat(shuffled, is(empty()));
        });
    }
}
