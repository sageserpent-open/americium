package com.sageserpent.americium.java
import com.sageserpent.americium.generation.Decision.DecisionStages
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.storage.TrialsReproductionStorage

import scala.collection.immutable.SortedMap

class RecipeCouldNotBeReproducedException(
    decisionStages: DecisionStages,
    choicesByCumulativeFrequency: SortedMap[Int, ?],
    index: Int,
    generation: Generation[?],
    connection: TrialsReproductionStorage
) extends RuntimeException({
      val recipeHash: String = decisionStages.recipeHash

      s"""Failed to reproduce recipe.
     |
     |This failed when trying to key into choices by cumulative frequency: $choicesByCumulativeFrequency
     |with the key: $index.
     |
     |The recipe you're trying to reproduce may have been created with a different
     |generation structure than the current code. This usually happens when:
     |  - You've modified how the trials instance is built.
     |  - You've added, removed or modified `.map`, `.flatMap` or `.filter` calls.
     |  - You've changed the parameters of the trials (e.g. different bounds).
     |
     |Your test cases have probably changed - you may need to regenerate the recipe by
     |re-running the test without the reproduction property.
     |
     |Recipe hash: $recipeHash
     |
     |Recipe:
     |
     |${decisionStages.longhandRecipe}
     |
     |Expected generation structure:
     |${connection.recipeDataFromRecipeHash(recipeHash).structureOutline}
     |
     |Current test's generation structure:
     |${generation.structureOutline}
     |""".stripMargin
    })
