package com.sageserpent.americium

package object java extends com.sageserpent.americium.java.TrialsEnrichment {
  type TestIntegrationContext[+Case] =
    com.sageserpent.americium.TestIntegrationContext[Case]
  type InlinedCaseFiltration = com.sageserpent.americium.InlinedCaseFiltration
  type CaseFailureReporting  = com.sageserpent.americium.CaseFailureReporting
}
