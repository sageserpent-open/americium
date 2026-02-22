package com.sageserpent.americium.java;

public interface Builder<Case, Collection> {
    void add(Case caze);

    Collection build();

    static Builder<Character, String> stringBuilder() {
        return new Builder<>() {
            final StringBuffer
                    buffer =
                    new StringBuffer();

            @Override
            public void add(
                    Character caze) {
                buffer.append(
                        caze);
            }

            @Override
            public String build() {
                return buffer.toString();
            }
        };
    }
}
