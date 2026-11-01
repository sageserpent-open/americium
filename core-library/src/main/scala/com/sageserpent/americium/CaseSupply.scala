package com.sageserpent.americium

trait CaseSupplyCycle {
  def isInitial: Boolean = 0 == numberOfPreviousCycles()
  def numberOfPreviousCycles(): Int
  def numberOfPreviousFailures(): Int
}

trait CasesLimitStrategy {
  def moreToDo(): Boolean
  def noteEmissionOfCase(): Unit
  def noteRejectionOfCase(): Unit
  def noteStarvation(): Unit
}

class NoValidTrialsException
    extends RuntimeException("No valid trials were executed.")

trait Builder[-Element, +Collection] {
  def add(element: Element): Unit
  def build(): Collection
}
