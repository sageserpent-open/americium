---
layout: default
title: Forcing Duplicates
parent: Advanced Techniques
nav_order: 1
reviewed: true
---

# Forcing Duplicates
{: .no_toc }

Generating test cases guaranteed to contain duplicate values
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

Many data structures need to handle **duplicate values** correctly—sets, maps, ranking algorithms, or caches. However, if you generate data from a large range, duplicates are mathematically rare:

```java
// Generate lists of integers from a large range
api().integers(-1000000, 1000000).immutableLists()

// The chance of getting a duplicate in a list of size 10 is tiny.
// You might need thousands of trials to hit a bug that only occurs with duplicates.
```

You could reduce the range (e.g., `integers(0, 5)`), but then you lose the ability to test with a wide variety of values.

---

## The Solution: Pool-based Generation

The trick to encouraging duplicates while maintaining a wide range of values is to **generate a small "pool" of choices first**, and then build your collection by picking from that pool.

```java
final Trials<ImmutableList<Integer>> queryValueLists = api()
    .integers(1, 10)                        // 1. Pick a pool size 'n'
    .flatMap(numberOfChoices -> api()
        .integers(-1000, 1000)              // 2. Generate 'n' random values
        .immutableListsOfSize(numberOfChoices)
        .flatMap(choices -> api()           // 3. Build a list of size 'n'...
            .choose(choices)                //    ...by picking FROM those choices
            .immutableListsOfSize(numberOfChoices)));
```

### Why This Works

1. We pick a small number `n` (e.g., 5).
2. We generate 5 random integers to serve as our "palette" for this trial (e.g., `[42, -107, 0, 999, 14]`).
3. We then generate a list of 5 elements by choosing **only** from those 5 values.

Because we are picking 5 times from a set of 5 values, the probability of hitting at least one duplicate is very high (based on the Pigeonhole Principle and the Birthday Paradox).

---

## Performance Impact

This technique can have a massive impact on the efficiency of your tests.

In a real-world example testing a "Tiers" data structure (which keeps track of the largest elements seen so far), a bug related to duplicate handling was only found after **89,489 trials** using standard generation.

By applying the **forcing duplicates** trick, the same bug was found and fully shrunk in just **69 trials**.

**That is a 99.9% reduction in the time and resources needed to find the bug.**

---

## When to Use This Technique

- **Sets and Maps**: To ensure you test "already exists" or "overwrite" logic.
- **Sorting/Ranking**: To ensure the algorithm is stable or handles "equal" elements correctly.
- **Deduplication logic**: To verify that duplicates are correctly identified and removed.
- **Stateful systems**: When an operation's validity depends on whether an ID has been seen before.

---

## Summary

- Randomly generated duplicates are rare in large search spaces.
- **Forcing duplicates** involves a two-step process: generate a limited pool of values, then pick from that pool to build your test case.
- This technique makes tests for duplicate-sensitive logic much more efficient, finding bugs in orders of magnitude fewer trials.
- It preserves the ability to test over a wide range of values while ensuring the *relationship* (duality) between values is explored.
