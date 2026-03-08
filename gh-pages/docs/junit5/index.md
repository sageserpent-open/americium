---
layout: default
title: JUnit5 Integration
nav_order: 4
has_children: true
permalink: /docs/junit5
---

# JUnit5 Integration
{: .no_toc }

Deep integration with JUnit5 for IDE support and test replay
{: .fs-6 .fw-300 }

{: .artifact }
> **Artifact Required:** The JUnit5 integration features are provided by the separate `americium-junit5` artifact (since version 1.26.0):
> ```scala
> libraryDependencies += "com.sageserpent" %% "americium-junit5" % "2.0.0"
> ```

## What You'll Learn

In this section, you'll discover two approaches to integrating Americium with JUnit5:

- **Annotation-Based Integration** - Use `@TrialsTest` for simple, declarative test configuration
- **Strongly-Typed Integration** - Use `@TestFactory` and `dynamicTests` for compile-time type safety

---

## Why JUnit5?

Americium takes a lean approach - it provides test case generation and shrinkage, but doesn't dictate your testing framework. However, JUnit5 is ubiquitous in the Java/Scala ecosystem, and your IDE probably has excellent support for it.

JUnit5's `@ParameterizedTest` lets you run the same test multiple times with different arguments - sound familiar? Americium matches this and goes further:

- **Automatic test case generation** (no need to manually list test cases)
- **Integrated shrinkage** (minimal failing cases)
- **IDE integration** (see individual trial runs in IntelliJ, Eclipse, etc.)
- **Individual replay** (re-run specific failing cases)
- **Shrinkage visualization** (see shrinkage attempts in your IDE)

---

## Two Approaches

### 1. Annotation-Based (`@TrialsTest`)

**Quick and simple** - Uses string-based field references:
```java
import com.sageserpent.americium.junit5.TrialsTest;

public class MyTest {
    private static final Trials<Integer> integers = 
        Trials.api().integers(-10, 10);
    
    @TrialsTest(trials = "integers", casesLimit = 20)
    void testSomething(Integer value) {
        // Test code here
    }
}
```

**Pros:** Simple, concise, familiar to `@ParameterizedTest` users  
**Cons:** String-based (no compile-time checking), refactoring-unfriendly

---

### 2. Strongly-Typed (`@TestFactory` + `dynamicTests`)

**Type-safe** - Compile-time verification:
```java
// Java...
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import com.sageserpent.americium.junit5.java.JUnit5;

public class MyTest {
    @TestFactory
    Iterator<DynamicTest> testSomething() {
        final Trials<Integer> integers = 
            Trials.api().integers(-10, 10);
        
        return JUnit5.dynamicTests(
            integers.withLimit(20),
            value -> {
                // Test code here
            }
        );
    }
}
```

```scala
// Scala...
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.TestFactory

class MyTest {
  @TestFactory
  def testSomething(): DynamicTests = {
    val integers = Trials.api.integers(-10, 10);

    integers.withLimit(20).dynamicTests { value =>
      // Test code here
    }
  }
}
```

**Pros:** Type-safe, refactoring-friendly, works in Scala too  
**Cons:** Slightly more verbose in Java

---

## Package Structure

Since version 2.0.0, JUnit5 integration lives in separate packages:

**Java:**
```java
import com.sageserpent.americium.junit5.TrialsTest;           // Annotation `@TrialsTest`
import com.sageserpent.americium.junit5.ConfiguredTrialsTest; // Annotation `@ConfiguredTrialsTest`
import com.sageserpent.americium.junit5.java.JUnit5;          // `dynamicTests` in module class
```

**Scala:**
```scala
import com.sageserpent.americium.junit5.*  // `dynamicTests` extension methods
```

---

## Features Common to Both Approaches

Regardless of which approach you choose, you get:

- ✅ **Automatic shrinkage** with visual feedback in IDE
- ✅ **Recipe reproduction** via `-Dtrials.recipeHash` or `-Dtrials.recipe`
- ✅ **Individual trial replay** (right-click in IDE to re-run specific case)
- ✅ **Lifecycle hooks** (`@BeforeEach`, `@AfterEach` work per-trial)
- ✅ **Multiple parameters** via `.and()` or tuple unpacking
- ✅ **Same configuration options** (limits, complexity, shrinkage, etc.)

---

## Which Approach Should I Use?

### Use `@TrialsTest` when:
- You like the reflection approach prevalent in Java.
- Your team is familiar with `@ParameterizedTest`
- You're comfortable with string-based references

### Use `@TestFactory` when:
- Type safety is important
- You're refactoring frequently
- You want IDE autocomplete and type hints

---

## Navigation

Choose your path:

- **[Annotation-Based Integration]({% link docs/junit5/integration.md %})** - `@TrialsTest` approach
- **[Strongly-Typed Integration]({% link docs/junit5/typed-tests.md %})** - `@TestFactory` approach

{: .note }
> Both approaches are equally powerful - it's a matter of personal preference and team coding standards.