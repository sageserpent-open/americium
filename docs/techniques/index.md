---
layout: default
title: Advanced Techniques
nav_order: 5
has_children: true
permalink: /docs/techniques
---

# Advanced Techniques
{: .no_toc }

Powerful patterns for sophisticated property testing
{: .fs-6 .fw-300 }

## What You'll Learn

In this section, you'll discover advanced techniques that solve specific testing challenges:

- **Forcing Duplicates** - Generate test cases guaranteed to contain duplicate values
- **Unique IDs** - Create readable, unique identifiers that shrink well
- **Permutations** - Test order-dependent behavior with index permutations
- **Complexity Budgeting** - Control recursive structure generation with complexity awareness
- **Alternate Picking** - Merge sequences while preserving element order

---

## Why Advanced Techniques?

The basic Americium API (`.map`, `.flatMap`, `.filter`, `.choose`, etc.) is powerful, but some testing scenarios need specialized approaches:

- **Finding subtle bugs** that only appear with specific patterns (like duplicates)
- **Testing algorithms** that depend on ordering or permutations
- **Controlling recursion** to avoid overly deep or overly shallow structures
- **Generating realistic data** with specific properties

These techniques show you **patterns and tricks** learned from real-world property testing.

---

## A Taste: Forcing Duplicates

As a preview, consider this problem: You want to test that a data structure handles **duplicate values** correctly, but random generation rarely produces duplicates.

**Naive approach** (inefficient):
```java
// Hope we get duplicates by chance - might need millions of trials!
api().integers(1, 1000000).immutableLists()
```

**Smart approach** (guaranteed duplicates):
```java
api().integers(1, 10).flatMap(n ->           // Pick list size
    api().integers(-1000, 1000)              // Generate pool of values
        .immutableListsOfSize(n)
        .flatMap(choices ->                  // Pick FROM those values
            api().choose(choices)            // Guaranteed duplicates!
                .immutableListsOfSize(n)))
```

This reduced one bug detection from **11,000 trials to 30 trials**!

---

## Navigation

Each technique page includes:
- **The Problem** - What testing challenge does it solve?
- **The Solution** - How to implement it
- **Concrete Examples** - Real code you can adapt
- **When to Use** - Guidance on applicability

Work through them in order, or jump to what you need:

{: .note }
> These are **patterns**, not APIs. You'll compose them from basic Americium building blocks.