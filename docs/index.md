---
layout: home
title: Home
nav_order: 1
description: "Americium - Property-based testing for Scala and Java with integrated shrinkage"
permalink: /
---

# Americium
{: .fs-9 }

Property-based testing for Scala and Java with integrated shrinkage. No need to write special-case shrinkers - Americium does it for you.
{: .fs-6 .fw-300 }

[Get started now](#getting-started){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 .mr-2 }
[View it on GitHub](https://github.com/sageserpent-open/americium){: .btn .fs-5 .mb-4 .mb-md-0 }

---

## What is Americium?

Americium is a property-based testing library that automatically generates test cases and shrinks failing cases down to minimal reproducers. Unlike other property testing libraries, **shrinkage is integrated** - you don't need to write separate shrinkers for your custom types.

### Key Features

- **Integrated Shrinkage** - Automatic test case minimization with no extra code
- **Multi-language** - APIs for both Scala and Java
- **JUnit5 Integration** - Deep integration with replay support
- **Powerful DSL** - Build complex test cases with map, flatMap, filter
- **Recipe Reproduction** - Reproduce any test case via recipe hash or JSON
- **Cross-platform** - Targets JVM and Scala Native

---

## Getting Started

### Installation

Add Americium to your project:

**Core library:**
```scala
libraryDependencies += "com.sageserpent" %% "americium" % "2.0.0"
```

**JUnit5 integration (optional):**
```scala
libraryDependencies += "com.sageserpent" %% "americium-junit5" % "2.0.0"
```

### Quick Example
```scala
import com.sageserpent.americium.Trials

val trials = Trials.api().integers(-5, 5)

trials.withLimit(10).supplyTo(println)
// Prints: 2, -5, 5, 4, 1, -4, 0, -3, -2, -1
```

When a test fails, Americium automatically shrinks the failing case:
```scala
val trials = Trials.api().integers()

trials.withLimit(1000).supplyTo { x =>
  val xSquared = x * x
  assertThat(xSquared / x, equalTo(x))
}

// Initial failure: x = 797772800
// After shrinkage: x = -46367  ← Much easier to debug!
```

---

## Quick Links

<div class="grid">
  <div class="grid-item">
    <h3>📚 Getting Started</h3>
    <p>Learn the basics of Americium</p>
    <a href="{% link docs/getting-started/index.md %}">Read more →</a>
  </div>
  <div class="grid-item">
    <h3>🧪 JUnit5 Integration</h3>
    <p>Use Americium with JUnit5</p>
    <a href="{% link docs/junit5/integration.md %}">Read more →</a>
  </div>
  <div class="grid-item">
    <h3>💡 Techniques</h3>
    <p>Advanced patterns and tricks</p>
    <a href="{% link docs/techniques/index.md %}">Read more →</a>
  </div>
  <div class="grid-item">
    <h3>🔧 API Reference</h3>
    <p>Design and implementation details</p>
    <a href="{% link docs/reference/design.md %}">Read more →</a>
  </div>
</div>

---

## Philosophy

Americium believes that **property-based testing should be accessible**. You shouldn't need a PhD in functional programming to write effective property tests, and you definitely shouldn't need to write custom shrinkers for every type you test.

That's why Americium:
- Provides a straightforward, composable API
- Handles shrinkage automatically
- Works with your existing test framework
- Gives you helpful reproduction recipes when tests fail

---

## Community

- **GitHub**: [sageserpent-open/americium](https://github.com/sageserpent-open/americium)
- **Issues**: [Report bugs or request features](https://github.com/sageserpent-open/americium/issues)
- **Releases**: [View on Sonatype](https://central.sonatype.com/artifact/com.sageserpent/americium)

---

## License

Americium is distributed under the [MIT License](https://github.com/sageserpent-open/americium/blob/master/LICENSE).