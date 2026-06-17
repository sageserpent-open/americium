---
layout: default
title: The Competition
parent: Reference
nav_order: 1
reviewed: true
---

# The Competition
{: .no_toc }

Americium in the property-based testing ecosystem
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Oh, That Bunch?

Americium fits into a wider ecosystem of property-based testing tools. Many of these are based on a Haskell implementation called **QuickCheck**, while others, including Americium, take their own unique approaches.

---

## Java Tools

### [Jqwik](https://jqwik.net/)

**Author:** [Johannes Link](https://github.com/jlink)

Jqwik is a powerful property-based testing library designed specifically for the JUnit5 framework.

**Key Features:**
- ✅ **Integrated shrinking** (comes for free, like Americium).
- ✅ **The Shrinking Challenge**: Both Jqwik and Americium have Java submissions to this [benchmark repository](https://github.com/jlink/shrinking-challenge).
- ✅ Uses its own engine instead of the standard Jupiter one.
- ✅ Heavy use of annotations for configuration.

**Philosophy:**
- More **prescriptive** about test structure than Americium.
- Assertion language agnostic.
- Comprehensive and well-structured documentation.

---

### [Quick Theories](https://github.com/quicktheories/QuickTheories)

**Approach:** DSL-based (similar to Americium).

**Key Features:**
- ✅ **Integrated shrinking**.
- ✅ Allows breakout from its own assertion language to use others.
- ❌ **JUnit5 Integration**: Limited; typically involves embedding a 'theory' into a standard `@Test`.

---

### [JUnit-Quickcheck](http://pholser.github.io/junit-quickcheck/site/1.0/)

**Approach:** QuickCheck-style.

**Key Features:**
- ✅ Integrates with JUnit5 using the Jupiter engine.
- ✅ Heavy use of annotations.
- ❌ **Manual shrinkage**: You must write your own shrinkage helpers for custom types.

---

### [Vavr Test](https://docs.vavr.io/#_property_checking)

**Approach:** Port of Scalacheck concepts to Java using the Vavr framework.

**Key Features:**
- ❌ **No shrinking support**.
- ✅ Lean and mean API.
- ❌ Sparse documentation (better to consult the Javadoc).

---

## Scala Tools

### [Scalacheck](https://scalacheck.org/)

**Status:** The incumbent tool in the Scala world.

**Key Features:**
- ✅ Can be used standalone or integrated with Scalatest.
- ❌ **Manual shrinkage**: Based on the QuickCheck approach; default shrinkers can sometimes be problematic and are often disabled by default.
- ✅ Own assertion language, but often used with Scalatest assertions.

---

### [Hedgehog](https://hedgehogqa.github.io/scala-hedgehog/)

**Approach:** Lean and mean DSL.

**Key Features:**
- ✅ **Integrated shrinking**.
- ✅ Own assertion language.
- ✅ Integrations with Minitest and MUnit.

---

### Other Scala Options
- **[Scalaprops](https://github.com/scalaprops/scalaprops)**
- **[Nyaya](https://github.com/japgolly/nyaya)**
- **[ZioTest](https://zio.dev/reference/test/property-testing/)**: Property testing integrated directly into the ZIO ecosystem.

---

## The Shrinking Challenge

If you are interested in how different tools handle complex shrinkage scenarios, check out **[The Shrinking Challenge](https://github.com/jlink/shrinking-challenge)**. It provides a set of standard problems and compares how various libraries across different languages (including Python's [Hypothesis](https://github.com/HypothesisWorks/hypothesis)) perform.

---

## Summary

| Feature | Americium | Jqwik | Scalacheck | Hedgehog |
|:---|:---|:---|:---|:---|
| **Language** | Java & Scala | Java | Scala | Scala |
| **Shrinking** | Integrated | Integrated | Manual | Integrated |
| **JUnit5** | Flexible | Core | Third-party | Third-party |
| **API Style** | DSL | Annotations | Monadic DSL | DSL |

Americium's unique position is offering **integrated shrinkage** with a consistent **DSL-based API** for both **Java and Scala**, without forcing a specific test framework or assertion style.
