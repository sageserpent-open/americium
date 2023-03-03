package com.sageserpent.americium

trait CaseFactory[+Case] {
  require(lowerBoundInput <= maximallyShrunkInput)
  require(maximallyShrunkInput <= upperBoundInput)

  def apply(input: Long): Case

  def lowerBoundInput: Long

  def upperBoundInput: Long

  def maximallyShrunkInput: Long
}
