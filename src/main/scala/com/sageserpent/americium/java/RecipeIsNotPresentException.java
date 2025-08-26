package com.sageserpent.americium.java;

import java.nio.file.Path;

public class RecipeIsNotPresentException extends RuntimeException {
    public RecipeIsNotPresentException(String recipeHash, Path databasePath) {
        super(String.format(
                "No recipe found for recipe hash: %s.\nHas the directory " +
                "containing the RocksDB instance for recipes at %s been " +
                "deleted?\nEither regenerate the recipe by running the test " +
                "without Java property `trials.recipeHash`, or use `trials" +
                ".recipe` with the full recipe JSON.",
                recipeHash,
                databasePath));
    }
}
