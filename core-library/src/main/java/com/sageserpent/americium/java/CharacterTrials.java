package com.sageserpent.americium.java;

public interface CharacterTrials extends Trials<Character> {
    default Trials<String> strings() {
        return collections(Builder::stringBuilder);
    }

    default Trials<String> stringsOfSize(int size) {
        return collectionsOfSize(size, Builder::stringBuilder);
    }
}
