package com.sageserpent.americium
import com.sageserpent.americium.java.{CaseFailureReporting, InlinedCaseFiltration, TestCaseRecording}

case class TestIntegrationContextImplementation[Case](
    caze: Case,
    caseFailureReporting: CaseFailureReporting,
    inlinedCaseFiltration: InlinedCaseFiltration,
    isPartOfShrinkage: Boolean,
    testCaseRecording: TestCaseRecording
) extends com.sageserpent.americium.java.TestIntegrationContext[Case]
