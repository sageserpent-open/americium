---
layout: default
title: Arrived from Scalacheck?
parent: Reference
nav_order: 2
reviewed: true
---

# Arrived from Scalacheck?
{: .no_toc }

Welcome to Americium—learn the local language
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction

If you are coming from Scalacheck, you will find Americium's approach very familiar but with some key improvements—most notably, **integrated shrinkage**.

In Americium, you don't need to write `Shrink[T]` instances. Shrinkage is automatically derived from the way you build your `Trials`.

---

## Terminology Translation

### `Arbitrary` and `Gen`

- **`Gen[T]`** becomes **`Trials[T]`**.
- There is no direct analogue for `Arbitrary[T]`. Instead, you either explicitly pass a `Trials[T]` or use **`Factory[T]`** for auto-derivation (similar to `scalacheck-shapeless`).

### Common Generators

| Scalacheck (`Gen`) | Americium (`api` or `Trials`) |
|:---|:---|
| `Gen.choose` / `chooseNum` | `api.integers`, `api.doubles`, `api.bigInts`, etc. (using range overloads) |
| `Gen.long` / `double` | `api.longs`, `api.doubles` (no-arg overloads) |
| `Gen.oneOf(t1, t2, ...)` | `api.choose(t1, t2, ...)` |
| `Gen.oneOf(g1, g2, ...)` | `api.alternate(g1, g2, ...)` |
| `Gen.frequency` | `api.alternateWithWeights` |
| `Gen.const` | `api.only` |
| `Gen.delay` | `api.delay` |
| `Gen.sequence` | `api.sequences` |
| `Gen.listOf` | `trials.lists` |
| `Gen.listOfN` | `trials.listsOfSize` |
| `Gen.containerOf` | `trials.collections` or `trials.several` |
| `Gen.option` | `trials.options` |
| `Gen.either` | `trials1.or(trials2)` |

{: .note }
> **Note on naming**: Americium's `TrialsApi` methods are typically pluralized (e.g., `api.longs` vs `Gen.long`).

---

## `Trials` as a Monad

Like `Gen`, `Trials` is a monad. It provides:
- `map`, `flatMap`, `filter`
- `mapFilter`
- `withFilter` (enabling full for-comprehension support)
- A typeclass instance for Cats' `Monad`.

Americium's implementation is **stack-safe**.

---

## Running Tests

### Instead of `Prop.forAll`

In Americium, you call `.supplyTo` on your `Trials` instance. You must specify a limit first:

```scala
trials.withLimit(100).supplyTo { caseValue =>
  // Your assertion here
}
```

### Multiple Parameters

Instead of taking multiple arguments in `forAll`, you gang trials together using `.and`:

```scala
(trials1 and trials2 and trials3)
  .withLimit(100)
  .supplyTo { (a, b, c) =>
    // ...
  }
```

---

## Test Configuration

| Scalacheck `Test.Parameters` | Americium equivalent |
|:---|:---|
| `withMinSuccessfulTests` | `trials.withLimit(n)` |
| `withMaxDiscardRatio` | `trials.withStrategy(c => CasesLimitStrategy.counted(n, discardRatio))` |
| `withInitialSeed` | `trials.withLimit(n).withSeed(seed)` |
| `withMaxSize` | `trials.withLimit(n).withComplexityLimit(maxSize)` |

---

## Sized Generation

Americium uses **`api.complexities`** instead of `Gen.size`.

```scala
api.complexities.flatMap { complexity =>
  // Build a trials instance based on the current complexity budget
}
```

This is often used with `alternateWithWeights` to create **Complexity Budgeting** (see [Advanced Techniques]({% link docs/techniques/complexity-budgeting.md %})).

---

## Custom Shrink Instances

**Delete them!**

You no longer need to maintain separate shrink logic. If your `Trials` is built correctly using `map`, `flatMap`, and `alternate`, Americium will find the minimal case automatically.

---

## Auto-Derivation

Americium uses the **Magnolia** library for auto-derivation via the `Factory[T]` typeclass.

```scala
import com.sageserpent.americium.Factory

// Deriving a Trials instance for a case class hierarchy
val rootTrials = implicitly[Factory[Root]].trials
```

For recursive structures in Scala 3, you may need an explicit given:
```scala
given evidence: Factory[Root] = Factory.autoDerived
```

---

## Summary

- **`Gen` → `Trials`**.
- **`Arbitrary` → `Factory`**.
- **Integrated Shrinkage** means no more `Shrink[T]` instances.
- **Plural naming convention** for API methods.
- **Explicit limits** required before running (`.withLimit(n)`).
