---
layout: default
title: Complexity Budgeting
parent: Advanced Techniques
nav_order: 4
reviewed: true
---

# Complexity Budgeting
{: .no_toc }

Controlling recursive structure generation with complexity awareness
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

When generating **recursive data structures** (trees, expressions, nested lists, etc.), you need to control their depth and size.

If you use a simple recursive definition, you often run into two extremes:
1. **Too shallow**: The generation terminates too early, and you never test deep structures.
2. **Runaway recursion**: The generation goes too deep, breaching the default complexity limit, resulting in very few valid test cases being generated.

We've seen that `api().delay()` prevents infinite recursion at the code level, but it doesn't solve the **distribution** problem. We want a healthy mix of shallow and deep structures without "starving" the test suite.

---

## The Solution: `api().complexities`

Americium allows you to access the current **complexity level** during generation. You can use this value to dynamically adjust the weights of your choices, creating "termination pressure" as the structure grows.

### Complexity-Aware Alternation

The key pattern is to use **`api.complexities`** combined with **`alternateWithWeights`**:

```scala
def statelyTrees: Trials[Tree] = api.complexities.flatMap(complexity =>
  api.alternateWithWeights(
    // As complexity increases, the weight of the Leaf (base case) increases
    complexity -> api.uniqueIds.map(Leaf.apply),

    // The Branching (recursive case) has a fixed weight
    2 -> api.integers(1, 5).flatMap(numberOfSubtrees =>
        statelyTrees.listsOfSize(numberOfSubtrees).map(Branching.apply)
      )
  )
)
```

### How It Works

1. **`api.complexities`** provides a `Trials[Int]` that yields the current complexity count (roughly the number of decisions made so far to build the current case).
2. **Early in generation** (low complexity):
   - The weight of the `Leaf` is low (e.g., 0 or 1).
   - The weight of the `Branching` is 2.
   - **Branching is favored**, allowing the tree to grow.
3. **Deep in generation** (high complexity):
   - The weight of the `Leaf` becomes high (e.g., 20 or 50).
   - The weight of the `Branching` remains 2.
   - **The base case (Leaf) becomes much more likely**, effectively "winding down" the recursion.

---

## Example: Expression Trees

Without complexity budgeting, a generator for mathematical expressions might produce either trivial constants or expressions so deep they hit the complexity limit and fail to generate.

With budgeting:

```java
public static Trials<Expr> expressions() {
    return api().complexities().flatMap(complexity ->
        api().alternateWithWeights(
            // Termination pressure: favor constants as we get deeper
            Map.entry(complexity, constants()),

            // Fixed weights for recursive structures
            Map.entry(1, unaryOperations()),
            Map.entry(2, binaryOperations())
        )
    );
}
```

This ensures that the majority of test cases stay within a "sweet spot" of complexity—complex enough to be interesting, but simple enough to be processed quickly and shrunk effectively.

---

## Why Budgeting Matters for Shrinkage

Americium shrinks cases by trying to reduce the "degrees of freedom" (complexity). If your generator only produces valid cases at the very edge of the complexity limit, the shrinker has very little room to move before the case becomes "invalid" according to your generator.

By using complexity budgeting, you ensure that there is a **continuous distribution** of valid cases from zero complexity up to your limit. This allows the shrinker to find a smooth path from a massive failing case down to a minimal one.

---

## Practical Tips

- **Adjust the fixed weight**: Increasing the fixed weight (e.g., from 2 to 5) will result in deeper/larger structures on average.
- **Complexity Limit**: Remember that you can still set a hard cap using `.withComplexityLimit(n)`. Complexity budgeting works *with* this limit to ensure you get a good distribution below the cap.
- **Nested Collections**: Complexity budgeting is also useful when generating lists of lists, where you want to ensure the total number of elements across all lists remains manageable.

---

## Summary

- Use **`api().complexities()`** to get the current growth state of a test case.
- Use **`alternateWithWeights`** to increase the probability of base cases as complexity grows.
- This creates **natural termination pressure**, preventing runaway recursion while still allowing for deep test cases.
- It improves both **generation efficiency** and **shrinkage quality**.
