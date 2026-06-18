---
layout: default
title: Permutations
parent: Advanced Techniques
nav_order: 3
reviewed: true
---

# Permutations
{: .no_toc }

Testing order-dependent logic with `indexPermutations`
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

A common pattern in property-based testing is to take an expected output that is correct by construction, then demolish it and use the resulting pieces as input to the test.

For example, if you are testing a sorting algorithm, you might:
1. Generate a sorted list (easy to do by construction).
2. **Permute** it (shuffle the elements).
3. Feed the shuffled list to your sorting algorithm.
4. Verify the output matches your original sorted list.

Generating random shuffles using a standard `Collections.shuffle()` inside a test is problematic because it introduces randomness that Americium cannot track or shrink.

---

## The Solution: `indexPermutations`

Americium provides **`api().indexPermutations(size)`**, which generates permutations of indices from `0` to `size - 1`.

```scala
api.indexPermutations(3).withLimit(10).supplyTo(println)
// Vector(1, 0, 2)
// Vector(2, 1, 0)
// Vector(1, 2, 0)
// Vector(0, 2, 1)
// Vector(0, 1, 2)
// Vector(2, 0, 1)
```

### Why "Index" Permutations?

By generating a permutation of **indices** rather than the elements themselves, Americium can provide a stable specification that works for any collection of that size. You then use these indices to rearrange your actual data.

---

## Example: Testing a Sort Algorithm

Let's test a sorting algorithm by generating a sorted collection, permuting it, and then sorting it back.

### 1. Generate Sorted Data
First, we generate a list of non-strictly increasing values:

```scala
val sortedData: Trials[Vector[Int]] = api
  .choose(0 until 5)
  .several[Vector[Int]]
  .flatMap(increments =>
    api.choose(-10 to 10).map(base =>
        increments.scanLeft(base)(_ + _)
    )
  )
```

### 2. Permute the Data
Now we use `indexPermutations` to shuffle that data:

```scala
val testCases = sortedData.flatMap { original =>
  api.indexPermutations(original.size).map { indices =>
    // Rearrange original using the permuted indices
    val shuffled = indices.map(original)
    (original, shuffled)
  }
}
```

### 3. Run the Test
Finally, we verify that sorting the shuffled data reproduces the original:

```scala
testCases.withLimit(100).supplyTo { case (original, shuffled) =>
  val result = shuffled.sorted
  assert(result == original)
}
```

---

## Shrinkage and Permutations

This is where `indexPermutations` shines. When a test fails on a complex permutation, Americium doesn't just reduce the size of the collection; it also **shrinks the permutation toward the identity permutation** (`[0, 1, 2, ...]`).

Suppose our "sort" algorithm has a bug where it fails if elements are moved more than 2 positions from their original spot.

1. Americium finds a failure with a wild permutation: `[8, 1, 13, 12, 11, 9, 6, 4, 10, 14, 0, 3, 5, 7, 2]`
2. It shrinks the list size and shuffles.
3. It eventually finds the **minimal failing permutation**:
   ```
   Case: [0, 1, 2, 3, 4, 5, 6, 7, 9, 8, 10, 11, 12, 13, 14]
   ```
   Notice how only **9 and 8** are out of order. This makes the bug immediately obvious: the failure is triggered specifically by this one swap.

---

## On-the-fly Permutations

If you don't need the whole collection at once, you can compose the indices with your source:

```scala
val inPermutationAt: Int => Int = indices.andThen(original)
```

This works well if you just need random access to permuted items without allocating a new collection.

---

## Java Example

```java
Trials<ImmutableList<Integer>> sortedData = ...;

sortedData.flatMap(original ->
    api().indexPermutations(original.size()).map(indices -> {
        List<Integer> shuffled = indices.stream()
            .map(original::get)
            .collect(Collectors.toList());
        return Tuple.tuple(original, shuffled);
    })
).withLimit(100).supplyTo(tuple -> {
    List<Integer> original = tuple._1();
    List<Integer> shuffled = tuple._2();
    
    assertThat(sort(shuffled), equalTo(original));
});
```

---

## Summary

- Use **`api().indexPermutations(n)`** to generate shuffles.
- It provides **stable, repeatable randomness** that Americium can track.
- It **shrinks toward the identity permutation**, making order-dependent bugs easy to isolate.
- Use it for testing sorts, caches, priority queues, or any logic where the order of operations/data matters.
