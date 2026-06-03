package com.sageserpent.americium.java;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SplitIntoPiecesJavaTest {
    private final TrialsApi api = Trials.api();

    private final Trials<? extends List<Integer>> itemsTrials = api.integers().immutableLists();

    @Test
    void splitIntoPiecesShouldYieldPiecesThatConcatenateToTheOriginalItems() {
        itemsTrials.withLimit(50).supplyTo(items -> {
            api.integers(1, Math.max(1, items.size())).withLimit(10).supplyTo(numberOfPieces -> {
                api.splitIntoPieces(items, numberOfPieces).withLimit(10).supplyTo(pieces -> {
                    assertThat(pieces.size(), is(numberOfPieces));
                    List<Integer> concatenated = pieces.stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList());
                    assertThat(concatenated, is(items));
                });
            });
        });
    }

    @Test
    void splitIntoNonEmptyPiecesShouldYieldNonEmptyPiecesThatConcatenateToTheOriginalItems() {
        itemsTrials.filter(items -> !items.isEmpty()).withLimit(50).supplyTo(items -> {
            api.integers(1, items.size()).withLimit(10).supplyTo(numberOfPieces -> {
                api.splitIntoNonEmptyPieces(items, numberOfPieces).withLimit(10).supplyTo(pieces -> {
                    assertThat(pieces.size(), is(numberOfPieces));
                    List<Integer> concatenated = pieces.stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList());
                    assertThat(concatenated, is(items));
                    for (List<Integer> piece : pieces) {
                        assertThat(piece, is(not(empty())));
                    }
                });
            });
        });
    }
}
