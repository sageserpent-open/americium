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
        itemsTrials.flatMap(items ->
            api.integers(1, Math.max(1, items.size())).flatMap(numberOfPieces ->
                api.splitIntoPieces(items, numberOfPieces).map(pieces ->
                    new Object() {
                        final List<Integer> itemsValue = items;
                        final int numberOfPiecesValue = numberOfPieces;
                        final List<List<Integer>> piecesValue = pieces;
                    }
                )
            )
        ).withLimit(100).supplyTo(testCase -> {
            assertThat(testCase.piecesValue.size(), is(testCase.numberOfPiecesValue));
            List<Integer> concatenated = testCase.piecesValue.stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList());
            assertThat(concatenated, is(testCase.itemsValue));
        });
    }

    @Test
    void splitIntoNonEmptyPiecesShouldYieldNonEmptyPiecesThatConcatenateToTheOriginalItems() {
        itemsTrials.filter(items -> !items.isEmpty()).flatMap(items ->
            api.integers(1, items.size()).flatMap(numberOfPieces ->
                api.splitIntoNonEmptyPieces(items, numberOfPieces).map(pieces ->
                    new Object() {
                        final List<Integer> itemsValue = items;
                        final int numberOfPiecesValue = numberOfPieces;
                        final List<List<Integer>> piecesValue = pieces;
                    }
                )
            )
        ).withLimit(100).supplyTo(testCase -> {
            assertThat(testCase.piecesValue.size(), is(testCase.numberOfPiecesValue));
            List<Integer> concatenated = testCase.piecesValue.stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList());
            assertThat(concatenated, is(testCase.itemsValue));
            for (List<Integer> piece : testCase.piecesValue) {
                assertThat(piece, is(not(empty())));
            }
        });
    }
}
