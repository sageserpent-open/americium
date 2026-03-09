---
layout: default
title: Strongly-Typed Integration
parent: JUnit5 Integration
nav_order: 2
reviewed: true
---

# Strongly-Typed Integration
{: .no_toc }

Using `@TestFactory` and `dynamicTests` for compile-time type safety
{: .fs-6 .fw-300 }

{: .artifact }
> **Artifact Required:**
> ```scala
> libraryDependencies += "com.sageserpent" %% "americium-junit5" % "2.0.0"
> ```

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Problem with String-Based Configuration

The `@TrialsTest` annotation is great - it's simple and concise. But it has a weakness: **string-based field references**.
```java
@TrialsTest(trials = "testCases", casesLimit = 10)  // "testCases" is just a string!
void myTest(TestCase tc) { ... }
```

Who knows what `"testCases"` actually refers to? What if:
- The field was renamed during refactoring?
- The field has the wrong type?
- There's a typo in the string?

You'll only find out at **runtime** when the test fails with a diagnostic error. Not ideal.

---

## The Solution: Strongly-Typed Integration

Whether you're writing in **Java** or **Scala**, you can integrate with JUnit5 using **compile-time type checking**. The supplier of test cases must match the test parameters in both type and number - verified at compile time!

---

## Scala Example with `dynamicTests`

Here's a test to verify sorting works, only it has a fault in the test itself:
```scala
// JUnit5 annotation
import org.junit.jupiter.api.TestFactory

// Americium JUnit5 integration for Scala
import com.sageserpent.americium.junit5.*

// Expecty assertions (optional - use your favorite assertion library)
import com.eed3si9n.expecty.Expecty.assert

@TestFactory
def sortingPutsThingsInOrder(): DynamicTests = {
  val testCases = for {
    nonNegativeIncrements <- api.integers(0, 10).lists
    minimum               <- api.integers(-20, 20)
    ascendingSequence = nonNegativeIncrements.scanLeft(minimum)(_ + _)
    permutation <- api.indexPermutations(ascendingSequence.size)
  } yield ascendingSequence -> permutation.map(ascendingSequence.apply)

  testCases.withLimit(10).dynamicTests {
    (ascendingSequence: Seq[Int], permutedSequence: Seq[Int]) =>
      val sortedSequence =
        permutedSequence /* FORGOT TO SORT IT! ----->> */ // .sorted

      assert(ascendingSequence == sortedSequence)
  }
}
```
Americium picks up the assertion failure and shrinks down to:

```
java.lang.AssertionError: assertion failed

assert(ascendingSequence == sortedSequence)
       |                 |  |
       List(0, 1)        |  Vector(1, 0)
                         false
```

Note the helpful pretty-printed labels on the failing assertion - these come from Expecty.

---

## What Changed?

### 1. `@TestFactory` Instead of `@TrialsTest`
```scala
@TestFactory
def sortingPutsThingsInOrder(): DynamicTests = {
  // ...
}
```

JUnit5's **`@TestFactory`** annotation expects a method that returns some kind of Java abstraction representing a series of `DynamicTest` instances.

---

### 2. `DynamicTests` Type Alias

To avoid polluting your nice Scala code with coarse and ill-mannered Java types, Americium provides a **type alias**:
```scala
import com.sageserpent.americium.junit5.*

// Brings in the DynamicTests type alias (note the plural!)
```

Behind the scenes, this is just `java.util.Iterator[org.junit.jupiter.api.DynamicTest]`, but you don't have to care about that!

---

### 3. `.dynamicTests` Instead of `.supplyTo`
```scala
  testCases.withLimit(10).dynamicTests {
  (ascendingSequence: Seq[Int], permutedSequence: Seq[Int]) =>  // ← Type-checked!
    // Test code
  }
```

The **`.dynamicTests`** method (pulled in via the import) packages up your test and supply into something JUnit5 can use.

**The key:** This is a **type-checked call** - the compiler verifies that the lambda's parameter type matches the trials instance's type parameter!

{: .tip }
> As Scala performs type inference, you could just write the arguments without the type declarations; the test body will be type-checked anyway. See which way you prefer.

---

### 4. Assertion Libraries
```scala
import com.eed3si9n.expecty.Expecty.assert
```

You're free to use **any assertion library** compatible with JUnit5 and Scala:

- **Expecty** - Great error messages
- **Standard `Predef.assert`** - Works but less informative
- **ScalaTest** - If using ScalaTest with JUnit5
- **JUnit5 assertions** - `org.junit.jupiter.api.Assertions.*`

Try different libraries and see what you prefer!

---

## Scala Package Information

**Import:**
```scala
import org.junit.jupiter.api.TestFactory
import com.sageserpent.americium.junit5.*
```

The **`.*`** import brings in:
- **`dynamicTests`** extension method
- **`DynamicTests`** type alias

---

## Java Example with `JUnit5.dynamicTests`

Java developers get type-safe integration too via the **`JUnit5`** utility class:
```java
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import com.sageserpent.americium.junit5.java.JUnit5;
import java.util.Iterator;

class DemonstrateJUnit5Integration {
    @TestFactory
    Iterator<DynamicTest> dynamicTestsExample() {
        final TrialsScaffolding.SupplyToSyntax<Integer> supplier =
            Trials.api().integers().withLimit(10);

        return JUnit5.dynamicTests(
            supplier,
            // The parameterised test - just prints the test case
            testCase -> {
                System.out.format("Test case %d\n", testCase);
            }
        );
    }
}
```

---

## Java Package Information

**Import:**
```java
import com.sageserpent.americium.junit5.java.JUnit5;
```

The **`JUnit5`** class provides static methods for creating type-safe dynamic tests.

---

## No Type Alias in Java

In Java, you have to see the full Java type in its glory:
```java
Iterator<DynamicTest> dynamicTestsExample() {
    // ...
}
```

But that's okay - Scala developers are just namby-pamby purists anyway. 😄

---

## Overloads for Multiple Parameters

Both **`.dynamicTests`** (Scala) and **`JUnit5.dynamicTests`** (Java) are overloaded to support:

- **Ganged trials** - Multiple independent trials with `.and()`
- **Tuple trials** - Trials producing tuples that auto-unpack

### Scala Example with Multiple Trials
```scala
val integers: Trials[Int] = api.integers()
val strings: Trials[String] = api.strings()
val booleans: Trials[Boolean] = api.booleans()

@TestFactory
def multiParamTest(): DynamicTests = {
  integers
    .and(strings)
    .and(booleans)
    .withLimit(20)
    .dynamicTests { (num, str, flag) =>  // Three parameters!
      // Test code
    }
}
```

### Java Example with Multiple Trials
```java
@TestFactory
Iterator<DynamicTest> multiParamTest() {
    final Trials<Integer> integers = Trials.api().integers();
    final Trials<String> strings = Trials.api().strings();
    
    return JUnit5.dynamicTests(
        integers.and(strings).withLimit(20),
        (num, str) -> {  // Two parameters!
            // Test code
        }
    );
}
```

---

## Tuple Auto-Unpacking

Works the same as with `@TrialsTest`:
```java
@TestFactory
Iterator<DynamicTest> tupleTest() {
    final Trials<Tuple2<Integer, String>> pairs =
        Trials.api().integers().flatMap(i ->
            Trials.api().strings().map(s ->
                Tuple.tuple(i, s)));
    
    return JUnit5.dynamicTests(
        pairs.withLimit(10),
        (Integer number, String text) -> {  // Tuple unpacked!
            System.out.format("Number: %d, Text: %s\n", number, text);
        }
    );
}
```

The **`Tuple2`** is automatically unpacked into two separate parameters!

---

## Benefits of Type-Safe Integration

### ✅ Compile-Time Type Checking
```scala
val trials: Trials[Int] = api.integers()

trials.withLimit(10).dynamicTests { (x: String) =>  // ← Compile error!
  // Type mismatch: expected Int, got String
}
```

The compiler **catches type mismatches** before you even run the test!

---

### ✅ Refactoring-Friendly

When you rename a field or change a type:
```java
// Before
private final Trials<Integer> numbers = ...

// After refactoring
private final Trials<Long> values = ...  // Changed type and name
```

With **`@TrialsTest`**: All tests with `trials = "numbers"` silently break (runtime error)

With **`dynamicTests`**: Compiler errors immediately point to what needs updating!

---

### ✅ IDE Support

Your IDE provides:
- **Autocomplete** on parameter types
- **Type hints** showing what the test receives
- **Jump to definition** from test to trials
- **Refactoring tools** that understand the connection

---

## Same Features as `@TrialsTest`

Despite the different syntax, you get **all the same features**:

- ✅ **Shrinkage** with visualization in IDE
- ✅ **Recipe reproduction** via `-Dtrials.recipeHash` or `-Dtrials.recipe`
- ✅ **Individual trial replay** (right-click in IDE)
- ✅ **Configuration options** (limits, complexity, shrinkage)
- ✅ **Lifecycle hooks** (`@BeforeEach`, `@AfterEach`)
- ✅ **Multiple parameters** via `.and()` or tuples

---

## Trade-offs

### Compared to `@TrialsTest`

| `@TrialsTest` | `dynamicTests` |
|---------------|----------------|
| More concise | Slightly more verbose |
| String-based (runtime errors) | Type-checked (compile errors) |
| Familiar to JUnit5 users | More functional style |
| Configuration in annotation | Configuration in code |

---

## When to Use Each

### Use `@TrialsTest` when:
- You want the **simplest** syntax
- Your team prefers **annotation-based** configuration
- You're comfortable with **string references**
- You rarely refactor field names

### Use `dynamicTests` when:
- **Type safety** is important
- You refactor **frequently**
- You want **IDE refactoring support**
- You prefer **explicit** over implicit

---

## Assertion Library Compatibility

The strongly-typed approach works with **any** assertion library compatible with JUnit5:

### Scala
```scala
// Expecty
import com.eed3si9n.expecty.Expecty.assert
assert(condition)

// Standard library
assert(condition)

// ScalaTest (with JUnit5 runner)
import org.scalatest.matchers.should.Matchers.*
result should be (expected)
```

### Java
```java
// JUnit5 assertions
import static org.junit.jupiter.api.Assertions.*;
assertEquals(expected, actual);

// Hamcrest
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
assertThat(actual, equalTo(expected));

// AssertJ
import static org.assertj.core.api.Assertions.*;
assertThat(actual).isEqualTo(expected);
```

---

## Summary Pattern

The typical pattern for strongly-typed JUnit5 integration:

### Scala
```scala
@TestFactory
def testName(): DynamicTests = {
  trials
    .withLimit(n)
    .dynamicTests { testCase =>
      // Test code
    }
}
```

### Java
```java
@TestFactory
Iterator<DynamicTest> testName() {
    return JUnit5.dynamicTests(
        trials.withLimit(n),
        testCase -> {
            // Test code
        }
    );
}
```

---

{: .note-title }
> Key Takeaways
>
> - **`@TestFactory`** - JUnit5's annotation for dynamic test generation
> - **`dynamicTests`** (Scala) - Extension method for type-safe integration
> - **`JUnit5.dynamicTests`** (Java) - Static method for type-safe integration
> - **`DynamicTests`** - Scala type alias (hides Java types)
> - **Compile-time type checking** - Mismatches caught before running
> - **Refactoring-friendly** - IDE tools understand the connections
> - **Same features** as `@TrialsTest` (shrinkage, replay, configuration)
> - **Works with any assertion library** compatible with JUnit5
> - **Slightly more verbose** than annotations, but type-safe
> - **Supports ganged trials and tuple unpacking** just like `@TrialsTest`