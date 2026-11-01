package com.sageserpent.americium.java

trait TrialsFactoring[+Case]
    extends com.sageserpent.americium.TrialsFactoring[Case] {
  def reproduce(recipe: String): Case
}

object TrialsFactoring {
  type TrialException = com.sageserpent.americium.TrialException
}
