---
layout: default
title: Alternate Picking
parent: Advanced Techniques
nav_order: 5
reviewed: true
---

# Alternate Picking
{: .no_toc }

Merging multiple sequences into one while preserving internal order
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

Sometimes you have multiple independent sequences of events or data, and you want to test how your system handles them when they are **interleaved**.

For example, you might have:
- Sequence A: `[A1, A2, A3]`
- Sequence B: `[B1, B2]`

You want to generate test cases like `[A1, B1, A2, B2, A3]` or `[B1, A1, A2, B2, A3]`. Crucially, you must **preserve the relative order** of elements within each sequence (e.g., `A1` must always come before `A2`).

Doing this manually with `flatMap` and random choices is complex and often leads to shuffles that Americium cannot shrink effectively.

---

## The Solution: `pickAlternatelyFrom`

Americium provides **`api().pickAlternatelyFrom()`**, which takes a set of sequences and merges them into a single list. The merging is done by picking the next element from one of the available sequences at each step.

```java
final List<Integer> odds = List.of(1, 3, 5, 7, 9);
final List<Integer> evens = List.of(0, 2, 4, 6, 8, 10);

final Trials<List<Integer>> merged =
        api().pickAlternatelyFrom(true, odds, evens);
```

### Parameters

- **`shrinkToRoundRobin` (boolean)**: This defines the "minimal" form that Americium will shrink toward if a failure occurs.
    - `true`: Shrink toward a strict interleaved pattern (`[A1, B1, A2, B2, ...]`).
    - `false`: Shrink toward a simple concatenation (`[A1, A2, A3, B1, B2, ...]`).
- **`sequences`**: Two or more iterables to be merged.

---

## Example: Testing a Merged Stream

Suppose you are testing a component that merges several sorted streams into a single sorted output.

```java
List<Integer> stream1 = List.of(1, 10, 100);
List<Integer> stream2 = List.of(2, 20, 200);
List<Integer> stream3 = List.of(3, 30, 300);

api().pickAlternatelyFrom(true, stream1, stream2, stream3)
    .withLimit(100)
    .supplyTo(mergedSequence -> {
        // The merged sequence preserves the relative order of each stream,
        // but the streams are interleaved in various ways.
        List<Integer> result = mySystem.process(mergedSequence);

        assertThat(result, isSorted());
    });
```

---

## Shrinkage and Alternate Picking

When a failure occurs with a wild, randomized interleave, Americium will use the `shrinkToRoundRobin` flag to find a simpler failing case.

- If **`shrinkToRoundRobin` is `true`**, Americium will try to find a failing case where the elements are perfectly interleaved:
  `[odds0, evens0, negatives0, odds1, evens1, negatives1, ...]`
  
- If **`shrinkToRoundRobin` is `false`**, Americium will try to find a failing case where one sequence is drained before the next starts:
  `[odds0, odds1, ..., evens0, evens1, ..., negatives0, negatives1, ...]`

This allows you to choose the "simplest" failure mode that makes sense for your specific domain.

---

## Summary

- Use **`api().pickAlternatelyFrom()`** to merge multiple ordered sequences into one.
- It **guarantees** that the relative order of elements within each source sequence is maintained.
- It provides a **highly efficient search space** for interleaved events.
- The **shrinkage strategy** (round-robin vs. concatenation) is configurable, allowing you to isolate bugs more easily.
- Use it for testing stream processing, multi-threaded event merging, or any system that consumes multiple ordered inputs.
