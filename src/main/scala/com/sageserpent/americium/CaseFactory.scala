package com.sageserpent.americium

trait CaseFactory[+Case] extends Serializable {
  def apply(input: BigInt): Case

  def lowerBoundInput: BigInt

  def upperBoundInput: BigInt

  def maximallyShrunkInput: BigInt
}
