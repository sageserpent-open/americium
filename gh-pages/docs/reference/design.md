---
layout: default
title: Design and Implementation
parent: Reference
nav_order: 3
reviewed: true
---

# Design and Implementation
{: .no_toc }

Behind the scenes of the Americium engine
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Build and Project Structure

Americium is an SBT project that uses cross-building to target both **Scala 2.13** and **Scala 3**. It is organized into several modules:

- **`americium`**: The core library containing the `Trials` API and shrinkage engine.
- **`americium-junit5`**: Integration with the JUnit5 framework.
- **`americium-utilities`**: General-purpose utilities used internally.

### Directory Layout
Sources follow the standard Maven/SBT convention with additional version-specific roots:
- `src/main/scala`: Shared Scala code.
- `src/main/scala-2.13` / `src/main/scala-3`: Version-specific code.
- `src/main/java`: Java client APIs and bridge code.

---

## The Core Engine: `Generation`

At its heart, Americium uses a **Free Monad** (provided by the Cats library) to represent the process of generating a test case.

When you compose `Trials` using `map`, `flatMap`, and `filter`, you are building up a recursive data structure called **`Generation`**. This structure doesn't generate data immediately; instead, it serves as a "recipe" for how a test case should be constructed.

### Interpreters
Americium has two primary interpreters for this free monad:

1. **The Random Interpreter**: Used during normal test execution. It walks through the `Generation` structure, making pseudorandom choices at each branch to produce a variety of test cases.
2. **The Recipe Interpreter**: Used for reproduction. It follows a specific `DecisionStages` (a recorded sequence of choices) to reconstruct the exact same test case from a previously failing trial.

---

## Integrated Shrinkage

Unlike many other property-based testing tools (like Scalacheck or QuickCheck) that require manual shrinkers, Americium's shrinkage is **integrated into the generation process**.

### How it Works
When a test fails, Americium records the `DecisionStages` (the list of all random choices made). It then attempts to "simplify" this sequence by:
- **Reducing complexity**: Trying smaller numbers of elements or shallower recursion.
- **Deflating scalars**: Moving numeric values toward a "maximally shrunk" target (usually zero).

The engine uses a combination of **scale deflation** (shrinking individual values) and **guided shrinkage** (using previous failing recipes as templates for new, simpler ones).

### The Cost Function
To decide if one test case is "simpler" than another, Americium uses a cost metric:
- Fewer decisions (lower complexity) are always preferred.
- For scalar values, the "cost" is proportional to the distance from the shrinkage target.

---

## Multi-Language Support

Americium provides first-class APIs for both Java and Scala.

- The **Scala API** is the primary implementation.
- The **Java API** acts as a forwarding wrapper, handling the conversion between Scala's functional types (like `Function1`) and Java's functional interfaces (like `Function`), as well as managing the gap between Scala's primitive-friendly generics and Java's reference-only generics.

---

## Key Components

### `TrialsApi`
The entry point for building trials. It provides factory methods like `integers()`, `choose()`, and `alternate()`.

### `CaseFactory`
Underpins the "streaming" methods (like `doubles()`). It defines a mapping from a `BigInt` input domain to the target test case type, allowing Americium to explore a continuous range of values.

### `SupplyToSyntax`
The DSL used to run tests (e.g., `.withLimit(100).supplyTo(...)`). It encapsulates the logic for driving the interpreters and managing the shrinkage loop.

---

## Summary

- Americium is built on **pure functional principles** using Free Monads.
- **Integrated shrinkage** is the core differentiator, deriving automatically from how you build your `Trials`.
- The library is designed for **high performance** and **repeatability** across both Java and Scala.
- The project follows a strict **cross-building** strategy to support the modern Scala ecosystem.
