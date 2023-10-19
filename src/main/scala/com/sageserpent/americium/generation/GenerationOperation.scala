package com.sageserpent.americium.generation
import cats.free.Free
import com.sageserpent.americium.CaseFactory

import scala.collection.immutable.SortedMap

sealed trait GenerationOperation[Case]

object GenerationOperation {
  type Generation[Case] = Free[GenerationOperation, Case]
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
