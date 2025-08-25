package com.sageserpent.americium
import com.sageserpent.americium.java.{CaseFailureReporting, InlinedCaseFiltration}

case class TestIntegrationContextImplementation[Case](
    caze: Case,
    caseFailureReporting: CaseFailureReporting,
    inlinedCaseFiltration: InlinedCaseFiltration,
    isPartOfShrinkage: Boolean,
    recipe: String
) extends com.sageserpent.americium.java.TestIntegrationContext[Case]
