package com.sageserpent.americium
import com.sageserpent.americium.java.{
  CaseFailureReporting,
  InlinedCaseFiltration,
  TestIntegrationContext
}

case class TestIntegrationContextImplementation[Case](
    caze: Case,
    caseFailureReporting: CaseFailureReporting,
    inlinedCaseFiltration: InlinedCaseFiltration,
    isPartOfShrinkage: Boolean,
    recipe: String
) extends TestIntegrationContext[Case]
