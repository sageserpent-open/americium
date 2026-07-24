package com.sageserpent.americium

abstract class TrialException(cause: Throwable)
    extends RuntimeException(cause) {
  override def toString: String =
    s"Trial exception with underlying cause:\n$getCause\nProvoked by test case:\n$provokingCase\n\nReproduce via Java property:\ntrials.recipeHash=$recipeHash\n\n... or via Java property:\ntrials.recipe=\"$escapedRecipe\"\n\n... or via `withRecipe` using recipe:\n$recipe"

  /** @return
    *   The {@code Case} that provoked the exception.
    */
  def provokingCase: Any

  /** @return
    *   A recipe that can be used to reproduce the provoking {@code Case} when
    *   supplied to the corresponding trials instance.
    */
  def recipe: String

  def escapedRecipe: String

  def recipeHash: String
}
