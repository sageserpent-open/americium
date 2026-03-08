---
layout: default
title: Alternate Picking
parent: Advanced Techniques
nav_order: 5
---

# Alternate Picking
{: .no_toc }

Merging sequences while preserving element order
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Problem

Imagine you're testing a **merge algorithm** or a **multi-stream processor**. You have multiple input sequences and need to generate **interleavings** of them:

**Sequences:**
```
A: [1, 2, 3]
B: [a, b, c]
C: [X, Y]
```

**Valid interleavings** (preserve order within each sequence):
```
[1, a, 2, X, b, 3, Y, c]  ✓ Order preserved: 1→2→3, a→b→c, X→Y
[a, 1, X, 2, b, Y, 3, c]  ✓ Order preserved
[1, 2, 3, a, b, c, X, Y]  ✓ Order preserved (simple concatenation)
```

**Invalid interleavings** (violate order):
```
[2, 1, a, 3, b, c, X, Y]  ✗ 2 before 1!
[a, c, b, 1, 2, 3, X, Y]  ✗ c before b!
```

How do you generate **valid interleavings** for testing?

---

## The Solution: `api.pickAlternatelyFrom()`

Americium provides a built-in way to generate interleavings:
```scala
val seq1 = IndexedSeq(1, 2, 3)
val seq2 = IndexedSeq('a', 'b', 'c')
val seq3 = IndexedSeq('X', 'Y')

api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  seq1, seq2, seq3
).withLimit(10).supplyTo(println)
```

Output:
```
[1, a, X, 2, b, Y, 3, c]
[1, 2, a, b, 3, c, X, Y]
[a, 1, 2, X, 3, b, Y, c]
[1, a, 2, b, 3, X, c, Y]
...
```

All elements from all sequences are present, and **order is preserved** within each sequence!

---

## How It Works

`api.pickAlternatelyFrom()` generates a **merge** of the input sequences:

1. Iterate through all sequences
2. Pick elements **alternately** from each
3. **Preserve order** within each sequence
4. Ensure **all elements** appear exactly once

The exact interleaving pattern varies based on Americium's pseudorandom generation.

---

## Parameters
```scala
api.pickAlternatelyFrom(
  shrinkToRoundRobin: Boolean,  // Shrinkage strategy
  sequences: IndexedSeq[T]*      // Variable number of sequences
)
```

### `shrinkToRoundRobin`

Controls how interleavings shrink when a test fails:

**`shrinkToRoundRobin = true`** - Shrinks toward **round-robin** merge:
```
Initial: [a, 1, 2, b, X, 3, c, Y]
Shrunk:  [1, a, X, 2, b, Y, 3, c]  ← Strict alternation
```

**`shrinkToRoundRobin = false`** - Shrinks toward **concatenation**:
```
Initial: [a, 1, 2, b, X, 3, c, Y]
Shrunk:  [1, 2, 3, a, b, c, X, Y]  ← Just appended
```

---

## Basic Usage Example

Testing a merge function:
```scala
val list1 = api.integers(1, 100).immutableListsOfSize(5)
val list2 = api.integers(1, 100).immutableListsOfSize(5)

list1.flatMap(l1 =>
  list2.flatMap(l2 =>
    api.pickAlternatelyFrom(
      shrinkToRoundRobin = true,
      l1.toIndexedSeq,
      l2.toIndexedSeq
    ).map(_.toList)
  )
).withLimit(100).supplyTo { merged =>
  // merged contains all elements from both lists
  // with order preserved
  
  val elements = merged.toSet
  assert(elements == (l1.toSet ++ l2.toSet))
  
  // Verify order preservation
  // (elements from l1 appear in same relative order)
  // (elements from l2 appear in same relative order)
}
```

---

## Real-World Example: Event Stream Merging

Testing a system that merges events from multiple sources:
```scala
case class Event(source: String, id: Int, timestamp: Long)

val source1Events = (1 to 5).map(i => Event("A", i, i * 100))
val source2Events = (1 to 3).map(i => Event("B", i, i * 100))
val source3Events = (1 to 4).map(i => Event("C", i, i * 100))

api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  source1Events.toIndexedSeq,
  source2Events.toIndexedSeq,
  source3Events.toIndexedSeq
).withLimit(50).supplyTo { mergedEvents =>
  
  // Process merged event stream
  val processor = new EventProcessor()
  mergedEvents.foreach(processor.process)
  
  // Verify all events processed
  assert(processor.processedCount == 12)  // 5 + 3 + 4
  
  // Verify order within each source
  val source1Processed = 
    processor.getProcessedEvents
      .filter(_.source == "A")
      .map(_.id)
  
  assert(source1Processed == List(1, 2, 3, 4, 5))
  
  // Similarly for source2 and source3...
}
```

---

## Choosing Shrinkage Strategy

### Round-Robin (`shrinkToRoundRobin = true`)

**Best for:** Testing systems that expect **balanced** input from sources.

**Shrinks to:**
```
[A1, B1, C1, A2, B2, C2, A3, B3, C3, ...]
```

**Example:** Load balancers, fair schedulers, round-robin distributors.

---

### Concatenation (`shrinkToRoundRobin = false`)

**Best for:** Testing systems where **bulk processing** might reveal bugs.

**Shrinks to:**
```
[A1, A2, A3, ..., B1, B2, B3, ..., C1, C2, C3, ...]
```

**Example:** Batch processors, systems with buffering, stream coalescers.

---

## Combining with Other Techniques

### Alternate Picking + Unique IDs

Test concurrent operations from multiple clients:
```scala
val client1Ops = api.uniqueIds().immutableListsOfSize(10)
val client2Ops = api.uniqueIds().immutableListsOfSize(10)
val client3Ops = api.uniqueIds().immutableListsOfSize(10)

client1Ops.flatMap(c1 =>
  client2Ops.flatMap(c2 =>
    client3Ops.flatMap(c3 =>
      api.pickAlternatelyFrom(
        shrinkToRoundRobin = true,
        c1.toIndexedSeq,
        c2.toIndexedSeq,
        c3.toIndexedSeq
      )
    )
  )
).withLimit(100).supplyTo { interleavedOps =>
  // Simulate concurrent operations
  val system = new ConcurrentSystem()
  
  interleavedOps.foreach(opId => {
    system.performOperation(opId)
  })
  
  // Verify system state
  assert(system.isConsistent())
}
```

---

### Alternate Picking + Permutations

First merge sequences, then permute the result:
```scala
api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  seq1, seq2, seq3
).flatMap(merged =>
  api.indexPermutations(merged.size).map(perm =>
    perm.map(merged)
  )
).withLimit(100).supplyTo { permutedMerge =>
  // Now order is NOT preserved!
  // Good for testing order-agnostic systems
}
```

---

## Variable-Length Sequences

Sequences don't need to be the same length:
```scala
val short = IndexedSeq(1, 2)
val medium = IndexedSeq('a', 'b', 'c', 'd')
val long = IndexedSeq('X', 'Y', 'Z', 'W', 'V', 'U')

api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  short, medium, long
).supplyTo(println)
```

Output example:
```
[1, a, X, 2, b, Y, c, Z, d, W, V, U]
```

All elements appear, order preserved within each sequence.

---

## Empty Sequences

Handles empty sequences gracefully:
```scala
val seq1 = IndexedSeq(1, 2, 3)
val seq2 = IndexedSeq.empty[Int]
val seq3 = IndexedSeq(7, 8)

api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  seq1, seq2, seq3
).supplyTo(println)
```

Output:
```
[1, 7, 2, 8, 3]
```

The empty sequence contributes nothing but doesn't break the merge.

---

## Performance Considerations

The alternate picking algorithm is **efficient** - it doesn't generate all possible interleavings (which would be exponential), but rather samples the space effectively.

**Number of possible interleavings:**
```
For sequences of lengths n1, n2, n3, ...:
Number of interleavings = (n1 + n2 + n3 + ...)! / (n1! × n2! × n3! × ...)
```

For `[3, 3, 2]`:
```
8! / (3! × 3! × 2!) = 40,320 / (6 × 6 × 2) = 40,320 / 72 = 560
```

Americium **samples** from these 560 possibilities rather than generating them all.

---

## When to Use Alternate Picking

### ✅ Use when:
- Testing **merge** algorithms
- Testing **multi-stream** processors
- Testing **concurrent** event handlers
- Simulating **interleaved** execution
- Testing **order-preserving** systems with multiple inputs

### ❌ Don't use when:
- Order **doesn't matter** (just concatenate or shuffle)
- You need **all possible interleavings** (combinatorial explosion)
- Sequences are **too large** (performance impact)

---

## Shrinkage Example

See shrinkage in action:
```scala
val seq1 = IndexedSeq(1, 2, 3, 4, 5)
val seq2 = IndexedSeq('a', 'b', 'c')

api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  seq1, seq2
).withLimit(100).supplyTo { merged =>
  // Simulate bug: fails if 'b' comes before 3
  val bIndex = merged.indexOf('b')
  val threeIndex = merged.indexOf(3)
  
  if (bIndex >= 0 && threeIndex >= 0 && bIndex < threeIndex) {
    throw new AssertionError(s"Bug! Merged: $merged")
  }
}
```

Shrinkage progression:
```
Initial failure: [1, a, 2, b, 3, c, 4, 5]
Shrinking toward round-robin...
[1, a, 2, b, 3, c, 4, 5]
[1, a, 2, b, 3, c, 4, 5]
[1, a, b, 2, 3, c, 4, 5]  ← Shrunk!
```

With `shrinkToRoundRobin = false`:
```
Initial failure: [1, a, 2, b, 3, c, 4, 5]
Shrinking toward concatenation...
[1, a, b, 2, 3, c, 4, 5]
[1, 2, a, b, 3, c, 4, 5]
[1, 2, 3, a, b, c, 4, 5]  ← Shrunk to concatenation
```

---

## Scala vs Java

**Scala:**
```scala
api.pickAlternatelyFrom(
  shrinkToRoundRobin = true,
  seq1, seq2, seq3
)
```

**Java:**
```java
api().pickAlternatelyFrom(
  true,  // shrinkToRoundRobin
  seq1, seq2, seq3
)
```

Same functionality, slightly different syntax.

---

{: .note-title }
> Key Takeaways
>
> - **`api.pickAlternatelyFrom(shrinkToRoundRobin, seqs...)`** - Merge sequences preserving order
> - **Order preserved** within each sequence
> - **All elements** appear exactly once
> - **Two shrinkage modes:** Round-robin (balanced) or concatenation (bulk)
> - **Efficient sampling** - Doesn't generate all possibilities
> - Perfect for merge algorithms, multi-stream processors, concurrent systems
> - Combine with unique IDs, permutations, and other techniques
> - Handles variable-length and empty sequences gracefully