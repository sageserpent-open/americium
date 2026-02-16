package com.sageserpent.americium.java

import java.nio.file.Path

class RecipeIsNotPresentException(recipeHash: String, databasePath: Path)
    extends RuntimeException(
      s"""No recipe found for recipe hash: $recipeHash.
          |Has the directory containing the RocksDB instance for recipes at $databasePath been deleted?
          |Either regenerate the recipe by running the test without Java property `trials.recipeHash`, or use `trials.recipe` with the full recipe JSON.""".stripMargin
    ) {}
