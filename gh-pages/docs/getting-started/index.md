---
layout: default
title: Getting Started
nav_order: 2
has_children: true
permalink: /docs/getting-started
---

# Getting Started with Americium

{: .no_toc }

Welcome to Americium! This section will guide you through the fundamentals of property-based testing with integrated
shrinkage.
{: .fs-6 .fw-300 }

## What You'll Learn

In this section, you'll discover:

- **Introducing Americium** - The `Trials` API, supplying test cases, and shrinkage in action
- **Variations** - Choices, alternation, weights, and special cases
- **Building Test Cases** - Collections, mapping, filtering, flat-mapping, and recursion

---

## Overview

Americium's API is built around the `Trials<Case>` generic interface. Think of it as a **fountain of test data** that:

1. Supplies varying test cases to your parameterized tests
2. Automatically shrinks failing cases to minimal reproducers
3. Records recipes so you can reproduce any test case exactly

Unlike traditional example-based testing where you write specific test cases by hand, with Americium you describe **what
kind of data** you want, and Americium generates hundreds or thousands of varied test cases for you.

---

## The Three-Step Pattern

Every Americium test follows this pattern:
```scala
// 1. Create a trials instance (describes what test data to generate)
val trials = Trials.api().integers(-5, 5)

// 2. Set a limit (how many test cases to try)
val configured = trials.withLimit(100)

// 3. Supply to your test
configured.supplyTo { testCase =>
  // Your test code here
  assert(testCase >= -5 && testCase <= 5)
}
```

That's it! Americium handles:
- ✅ Generating varied test cases
- ✅ Running your test repeatedly
- ✅ Shrinking failures to minimal cases
- ✅ Providing reproduction recipes

---

## Quick Start Example

Let's test that squaring a number and dividing by that number gives back the original:
```java
import com.sageserpent.americium.java.Trials;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

final Trials<Integer> trials = Trials.api().integers();

trials.withLimit(1000).supplyTo(x -> {
    final int xSquared = x * x;
    assertThat(xSquared / x, equalTo(x));
});
```

This test **will fail** due to integer overflow. But watch what Americium does:
```
Initial failure:
    x = 797772800  (huge number!)

After shrinkage:
    x = -46367  (much easier to debug!)
```

Americium automatically found the smallest failing case. No extra work needed!

---

## Navigation

Proceed through the topics in order, or jump to what interests you:

{: .note }
> The examples use both Java and Scala interchangeably. The APIs are equivalent - pick whichever language you prefer!