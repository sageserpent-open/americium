---
layout: default
title: The Competition
parent: Reference
nav_order: 1
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

Americium fits into a wider ecosystem of property-based testing tools. Many of these are based on a Haskell implementation called **QuickCheck**, and there are some others that take their own approaches - including Americium itself.

---

## Java Tools

### Jqwik

**Author:** Johannes Link  
**Challenge Repository:** [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge)

Jqwik is a powerful property-based testing library designed specifically for JUnit5.

**Key Features:**
- ✅ **Integrated shrinking** (free, like Americium!)
- ✅ **Comprehensive documentation**
- ✅ Uses JUnit5's own engine (not Jupiter)
- ✅ Heavy use of annotations
- ✅ In "The Shrinking Challenge"

**Philosophy:**
- More **prescriptive** about test structure than Americium
- Annotation-driven approach
- Assertion language agnostic (like Americium)

**When to choose Jqwik:**
- You want a JUnit5-first experience
- You prefer annotation-based configuration
- You like comprehensive, structured documentation

**Website:** [jqwik.net](https://jqwik.net/)

---

### Quick Theories

**Approach:** DSL-based (like Americium)

**Key Features:**
- ✅ **Integrated shrinking**
- ✅ DSL-based API
- ✅ Assertion language breakout support
- ❌ Not in The Shrinking Challenge

**Integration:**
- Basic JUnit5 support - embed a 'theory' in a JUnit test
- Similar to using Americium's `.supplyTo` within a `@Test` method

**When to choose Quick Theories:**
- You want DSL-style API
- You're comfortable with minimal JUnit integration
- You prefer a lightweight approach

---

### JUnit-Quickcheck

**Approach:** QuickCheck-style

**Key Features:**
- ✅ JUnit5 Jupiter engine integration
- ✅ Uses annotations
- ❌ **Manual shrinkage** - you write shrinkers for custom types
- ❌ Not in The Shrinking Challenge

**Philosophy:**
- Follows QuickCheck's approach
- Default shrinkers for built-in types
- Custom types require custom shrinkage code

**When to choose JUnit-Quickcheck:**
- You're familiar with QuickCheck's approach
- You don't mind writing shrinkers
- You want annotation-based JUnit5 integration

---

### Vavr Test

**Approach:** Port of Scalacheck concepts to Java using Vavr

**Key Features:**
- ❌ **No shrinking support**
- ✅ Lean and mean (even more than Americium!)
- ❌ Very sparse website documentation
- ✅ Good Javadoc in code

**When to choose Vavr Test:**
- You're already using the Vavr library
- You don't need shrinking
- You prefer reading code over documentation
- You want the leanest possible API

**Note:** Check the code - Javadoc is the primary documentation source.

---

## Scala Tools

### Scalacheck

**Status:** The incumbent tool in the Scala world

**Key Features:**
- ✅ Can be used standalone or with Scalatest
- ✅ Fairly good documentation covering basics
- ❌ **Manual shrinkage** for custom types (QuickCheck approach)
- ❌ Default shrinkers can break (disabled by default now)
- ❌ Not in The Shrinking Challenge
- ✅ Own assertion language (or Scalatest with integration)

**Philosophy:**
- Assumes familiarity with monadic DSL
- Scala users expected to understand for-comprehensions naturally
- Manual shrinking for custom types

**When to choose Scalacheck:**
- You're already using it (established ecosystem)
- You're comfortable writing shrinkers
- You want Scalatest integration

**Migration Guide:** See [Migrating from Scalacheck]({% link docs/reference/from-scalacheck.md %}) for translation to Americium.

---

### Hedgehog

**Approach:** Lean DSL with integrated shrinking

**Key Features:**
- ✅ **Integrated shrinking**
- ✅ Lean and mean DSL
- ✅ Own assertion language
- ✅ Good documentation
- ✅ Integrations with Minitest and MUnit
- ❌ Not in The Shrinking Challenge

**When to choose Hedgehog:**
- You want integrated shrinking in Scala
- You prefer a minimal DSL
- You're using Minitest or MUnit

---

### Scalaprops

Mentioned in the ecosystem but details sparse.

---

### Nyaya

Mentioned in the ecosystem but details sparse.

---

### ZioTest

Part of the ZIO ecosystem - property testing integrated into ZIO Test.

**When to choose ZioTest:**
- You're already using ZIO
- You want property testing integrated with your effect system

---

## Other Languages

### Hypothesis (Python)

**Status:** In The Shrinking Challenge

The go-to property-based testing library for Python, with integrated shrinkage and a powerful API.

**When to choose Hypothesis:**
- You're writing Python
- You want the most mature Python property testing library

**Link:** [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge) has examples

---

### More Languages

The Shrinking Challenge repository includes submissions from many languages and tools:
- F# (FsCheck)
- Rust (proptest, quickcheck)
- Swift (SwiftCheck)
- And more!

**Explore:** [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge)

---

## How Americium Compares

### Americium's Unique Position

| Feature | Americium | Jqwik | Scalacheck | Hedgehog |
|---------|-----------|-------|------------|----------|
| **Integrated Shrinking** | ✅ | ✅ | ❌ | ✅ |
| **Java API** | ✅ | ✅ | ❌ | ❌ |
| **Scala API** | ✅ | ❌ | ✅ | ✅ |
| **JUnit5 Integration** | ✅ Optional | ✅ Core | ❌ | ❌ |
| **DSL Approach** | ✅ | ❌ (Annotations) | ✅ | ✅ |
| **Standalone Use** | ✅ | ❌ | ✅ | ✅ |
| **In Shrinking Challenge** | ✅ | ✅ | ❌ | ❌ |

---

### Americium's Philosophy

**Lean and Mean:**
- No prescribed test structure
- Use any assertion library
- Optional JUnit5 integration (not required)

**Integrated Shrinkage:**
- No manual shrinker writing
- Shrinkage derives from how you build trials
- "Delete them and dance a jig!" 🎉

**Multi-Language:**
- Equal-class Java and Scala APIs
- Both are first-class citizens

**Flexible:**
- Works standalone with `.supplyTo`
- Works with JUnit5 via annotations or `@TestFactory`
- Mix and match as needed

---

## The Shrinking Challenge

Johannes Link (Jqwik's author) maintains an excellent repository comparing shrinking across different tools:

**Repository:** [The Shrinking Challenge](https://github.com/jlink/shrinking-challenge)

Both **Jqwik** and **Americium** have Java submissions demonstrating their shrinking capabilities on the same challenges.

**Why it matters:**
- See real comparisons of shrinkage quality
- Understand trade-offs between tools
- Benchmarks across languages and libraries

{: .tip }
> If you're evaluating property testing tools, spend time in The Shrinking Challenge repository. It's an invaluable resource!

---

## Choosing a Tool

### Choose Americium if:
- ✅ You want **integrated shrinkage** (no manual shrinkers)
- ✅ You need **both Java and Scala** APIs
- ✅ You want **flexibility** (JUnit5 optional, not required)
- ✅ You prefer **DSL-based** test case building
- ✅ You like a **lean approach** (bring your own assertions)

### Choose Jqwik if:
- ✅ You want a **JUnit5-first** experience
- ✅ You prefer **annotation-driven** configuration
- ✅ You want **comprehensive documentation**
- ✅ You only need **Java** support

### Choose Scalacheck if:
- ✅ You're already using it (established ecosystem)
- ✅ You want **Scalatest integration**
- ✅ You're okay writing **manual shrinkers**
- ✅ You only need **Scala** support

### Choose Hedgehog if:
- ✅ You want **integrated shrinkage** in Scala
- ✅ You prefer a **minimal DSL**
- ✅ You're using **Minitest** or **MUnit**

---

## Resources

- **The Shrinking Challenge:** [github.com/jlink/shrinking-challenge](https://github.com/jlink/shrinking-challenge)
- **Jqwik:** [jqwik.net](https://jqwik.net/)
- **Scalacheck:** [scalacheck.org](https://scalacheck.org/)
- **Hedgehog:** [github.com/hedgehogqa/scala-hedgehog](https://github.com/hedgehogqa/scala-hedgehog)
- **Hypothesis:** [hypothesis.works](https://hypothesis.works/)

---

{: .note-title }
> Key Takeaways
>
> - **Many property testing tools available** across JVM and beyond
> - **Two main approaches:** Integrated shrinking (Americium, Jqwik, Hedgehog) vs. manual shrinking (Scalacheck, JUnit-Quickcheck)
> - **Americium's niche:** Integrated shrinkage + Java & Scala + JUnit5 optional + DSL approach
> - **The Shrinking Challenge** - Excellent resource for comparing tools
> - Each tool has different trade-offs - choose based on your needs