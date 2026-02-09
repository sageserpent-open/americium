package com.sageserpent.americium.generation

import cats.free.Free
import io.circe.{Encoder, Json}
import io.circe.syntax.*

/** Utilities for generating structural representations and hashes of Generation
  * instances.
  *
  * These methods produce representations that capture the *structure* of a
  * Generation (free monad), allowing detection of when a test's trial
  * composition has changed between runs.
  *
  * Note: The representations are "shape-only" - they don't capture lambda
  * behavior, only the types and parameters of operations.
  */
object GenerationOperationCodecs {

  /** Generate a string representation of a Generation for structural comparison
    *
    * This uses the Free monad's built-in structure to create a string
    * representation. Changes to the composition (added/removed operations,
    * changed parameters) will result in different strings.
    *
    * Limitations: - Lambda implementations are shown as <function> -  Doesn't
    * capture changes to lambda logic - Does capture structural changes (map,
    * flatMap, filter added/removed) - Does capture parameter changes (bounds,
    * number of choices, etc.)
    */
  def toStructureString[Case](
      generation: Free[GenerationOperation, Case]
  ): String = {
    // Use pprint for a readable, deterministic representation
    pprint.apply(generation, height = Int.MaxValue).plainText
  }

  /** Compute a hash of the Generation structure
    *
    * Uses Guava's Murmur3 hash (same as used for recipe hashing) for
    * consistency.
    *
    * This hash will be the same for structurally identical trials, even across
    * JVM restarts, as long as the trials composition hasn't changed.
    */
  def computeStructureHash[Case](
      generation: Free[GenerationOperation, Case]
  ): String = {
    val structureString = toStructureString(generation)
    com.google.common.hash.Hashing
      .murmur3_128()
      .hashUnencodedChars(structureString)
      .toString
  }
}

