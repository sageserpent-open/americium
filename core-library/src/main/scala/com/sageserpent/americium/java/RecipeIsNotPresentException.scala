package com.sageserpent.americium.java

import java.nio.file.Path

class RecipeIsNotPresentException(recipeHash: String, recipeDirectory: Path)
    extends RuntimeException(
      s"""No recipe found for recipe hash: $recipeHash.
          |Has the directory $recipeDirectory containing the recipes been deleted, changed to a different location or been regenerated?
          |Either regenerate the recipe by running the test without Java property `trials.recipeHash`, or use `trials.recipe` with the full recipe JSON.""".stripMargin
    ) {}
