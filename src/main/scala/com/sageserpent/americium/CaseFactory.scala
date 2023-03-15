package com.sageserpent.americium

trait CaseFactory[+Case] {
  def apply(input: BigInt): Case

  def lowerBoundInput: BigInt

  def upperBoundInput: BigInt

  def maximallyShrunkInput: BigInt
}
