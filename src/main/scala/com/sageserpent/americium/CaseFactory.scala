package com.sageserpent.americium

trait CaseFactory[+Case] {
  // TODO: find a new home for the invariant...
  /* require(lowerBoundInput <= maximallyShrunkInput)
   * require(maximallyShrunkInput <= upperBoundInput) */

  def apply(input: BigInt): Case

  def lowerBoundInput: BigInt

  def upperBoundInput: BigInt

  def maximallyShrunkInput: BigInt
}
