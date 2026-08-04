package com.sageserpent.americium.junit5

import com.sageserpent.americium.generation.Decision
import org.apache.commons.text.StringEscapeUtils

class TrialException(
    cause: Throwable,
    override val provokingCase: Any,
    override val recipe: String,
    override val recipeHash: String
) extends com.sageserpent.americium.TrialException(cause) {
  override val escapedRecipe: String =
    StringEscapeUtils.escapeJava(Decision.parseRecipe(recipe).shorthandRecipe)
}
