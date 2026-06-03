package com.sageserpent.americium.java;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SplitIntoPiecesJavaTest {
    private final TrialsApi api = Trials.api();

    @Test
    void splitIntoPiecesShouldYieldPiecesThatConcatenateToTheOriginalItems() {
        List<Integer> items = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        api.integers(1, 20).withLimit(10).supplyTo(numberOfPieces -> {
            api.splitIntoPieces(items, numberOfPieces).withLimit(10).supplyTo(pieces -> {
                assertThat(pieces.size(), is(numberOfPieces));
                List<Integer> concatenated = pieces.stream().flatMap(List::stream).collect(Collectors.toList());
                assertThat(concatenated, is(items));
            });
        });
    }

    @Test
    void splitIntoNonEmptyPiecesShouldYieldNonEmptyPiecesThatConcatenateToTheOriginalItems() {
        List<Integer> items = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        api.integers(1, items.size()).withLimit(10).supplyTo(numberOfPieces -> {
            api.splitIntoNonEmptyPieces(items, numberOfPieces).withLimit(10).supplyTo(pieces -> {
                assertThat(pieces.size(), is(numberOfPieces));
                List<Integer> concatenated = pieces.stream().flatMap(List::stream).collect(Collectors.toList());
                assertThat(concatenated, is(items));
                for (List<Integer> piece : pieces) {
                    assertThat(piece, is(not(empty())));
                }
            });
        });
    }
}
