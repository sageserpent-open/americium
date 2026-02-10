package com.sageserpent.americium.generation
import cats.free.Free
import com.google.common.hash.Hashing as GuavaHashing
import com.sageserpent.americium.CaseFactory

import scala.collection.immutable.SortedMap

sealed trait GenerationOperation[Case]

object GenerationOperation {
  type Generation[Case] = Free[GenerationOperation, Case]

  /** Utilities for generating structural representations and hashes of
    * [[Generation]] instances.
    *
    * These methods produce representations that capture the *structure* of a
    * Generation (free monad), allowing detection of when a test's trial
    * composition has changed between runs.
    *
    * Note: The representations are "shape-only" - they don't capture lambda
    * behaviour, only the types and parameters of operations.
    */
  implicit class Syntax[Case](val generation: Generation[Case]) extends AnyVal {

    /** Compute a hash of the Generation structure
      *
      * Uses Guava's Murmur3 hash (same as used for recipe hashing) for
      * consistency.
      *
      * This hash will be the same for structurally identical trials, even
      * across JVM restarts, as long as the trials composition hasn't changed.
      */
    def structureOutlineHash: String = GuavaHashing
      .murmur3_128()
      .hashUnencodedChars(generation.structureOutline)
      .toString

    /** Generate a string representation of a Generation for structural
      * comparison
      *
      * This uses the Free monad's built-in structure to create a string
      * representation. Changes to the composition (added/removed operations,
      * changed parameters) will result in different strings.
      *
      * Limitations: - Lambda implementations are shown as <function> - Doesn't
      * capture changes to lambda logic - Does capture structural changes (map,
      * flatMap, filter added/removed) - Does capture parameter changes (bounds,
      * number of choices, etc.)
      */
    def structureOutline: String = {
      // Use pprint for a readable, deterministic representation
      pprint.apply(generation, height = Int.MaxValue).plainText
    }
  }

}

// Use a sorted map keyed by cumulative frequency to implement weighted
// choices. That idea is inspired by Scalacheck's `Gen.frequency`.
case class Choice[Case](choicesByCumulativeFrequency: SortedMap[Int, Case])
    extends GenerationOperation[Case]

case class Factory[Case](factory: CaseFactory[Case])
    extends GenerationOperation[Case]

// NASTY HACK: as `Free` does not support `filter/withFilter`, reify
// the optional results of a flat-mapped filtration; the interpreter
// will deal with these.
case class FiltrationResult[Case](result: Option[Case])
    extends GenerationOperation[Case]

case object NoteComplexity extends GenerationOperation[Int]

case class ResetComplexity[Case](complexity: Int)
    extends GenerationOperation[Unit]

case object UniqueId extends GenerationOperation[Int]
