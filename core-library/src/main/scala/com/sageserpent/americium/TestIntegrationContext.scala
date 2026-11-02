package com.sageserpent.americium

trait InlinedCaseFiltration {
  def executeInFiltrationContext(
      runnable: Runnable,
      additionalExceptionsToNoteAsFiltration: Array[Class[_ <: Throwable]]
  ): Boolean
}

trait CaseFailureReporting {
  def report(throwable: Throwable): Unit
}

trait TestIntegrationContext[+Case] {
  def caze: Case
  def caseFailureReporting: CaseFailureReporting
  def inlinedCaseFiltration: InlinedCaseFiltration
  def isPartOfShrinkage: Boolean
  def recipe: String
}

case class TestIntegrationContextImplementation[Case](
    caze: Case,
    caseFailureReporting: CaseFailureReporting,
    inlinedCaseFiltration: InlinedCaseFiltration,
    isPartOfShrinkage: Boolean,
    recipeThunk: () => String
) extends TestIntegrationContext[Case] {
  override lazy val recipe: String = recipeThunk()
}
