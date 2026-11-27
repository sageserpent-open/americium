package com.sageserpent.americium.java

trait TrialsEnrichment {
  implicit class ScalaTrialsToJavaTrialsEnrichment[Case](
      underlyingTrials: com.sageserpent.americium.Trials[Case]
  ) {
    def javaTrials: com.sageserpent.americium.java.Trials[Case] =
      com.sageserpent.americium.java.TrialsImplementation(underlyingTrials)
  }
}

object TrialsEnrichment extends TrialsEnrichment
