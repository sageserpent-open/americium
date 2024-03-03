package com.sageserpent.americium.java

trait TestIntegrationContext[+Case] {
  def caze: Case
  def caseFailureReporting: CaseFailureReporting
  def inlinedCaseFiltration: InlinedCaseFiltration
  def isPartOfShrinkage: Boolean
  def testCaseRecording: TestCaseRecording
}
