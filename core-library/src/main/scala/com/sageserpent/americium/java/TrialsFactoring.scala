package com.sageserpent.americium.java

trait TrialsFactoring[+Case] {
  // Scala and Java API ...

  /** Reproduce a specific case in a repeatable fashion, based on a recipe.
    *
    * @param recipe
    *   This encodes a specific case and will only be understood by the same
    *   *value* of trials instance that was used to obtain it.
    * @return
    *   The specific {@code Case} denoted by the recipe.
    * @throws RuntimeException
    *   if the recipe does not correspond to the receiver, either due to it
    *   being created by a different flavour of trials instance or subsequent
    *   code changes.
    */
  def reproduce(recipe: String): Case

  abstract class TrialException(cause: Throwable)
      extends com.sageserpent.americium.TrialException(cause) {

    /** @return
      *   The {@code Case} that provoked the exception.
      */
    override def provokingCase: Case
  }
}
