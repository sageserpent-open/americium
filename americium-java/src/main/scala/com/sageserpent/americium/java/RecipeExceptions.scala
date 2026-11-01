package com.sageserpent.americium.java

class RecipeIsNotPresentException(
    recipeHash: String,
    recipeDirectory: os.Path
) extends com.sageserpent.americium.RecipeIsNotPresentException(
      recipeHash,
      recipeDirectory
    )

class RecipeCouldNotBeReproducedException(
    decisionStages: com.sageserpent.americium.generation.Decision.DecisionStages,
    choicesByCumulativeFrequency: scala.collection.immutable.SortedMap[Int, ?],
    index: Int,
    generation: com.sageserpent.americium.generation.GenerationOperation.Generation[
      ?
    ],
    trialsReproductionStorage: com.sageserpent.americium.storage.TrialsReproductionStorage
) extends com.sageserpent.americium.RecipeCouldNotBeReproducedException(
      decisionStages,
      choicesByCumulativeFrequency,
      index,
      generation,
      trialsReproductionStorage
    )
