package com.sageserpent.americium.java;

public interface Builder<Case, Collection> {
    void add(Case caze);

    Collection build();
}
