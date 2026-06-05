---
layout: default
title: "JUnit5 again"
parent: Wiki Content
nav_order: 13
---

# JUnit5 again
{: .no_toc }

Strongly typed test supply
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


The JUnit5 integration discussed before is great if you're used to JUnit5's `@ParameterizedTest` annotation, but if you've used Scalacheck, you may be a little put off by the _stringly_ typed code that has is forced on us by Java annotations - to wire up the supplier of test cases, we have to specify the *name* of a field in the test class.

Who knows what actual test cases might be supplied, or even if the field has been renamed in the meantime? Sure, we'll see a runtime error with some diagnostic, but...

Fear not - whether you are writing tests in Java or Scala, it is possible to integrate with JUnit5 with strongly-typed coupling between the test case supply and the actual parameterised test. So the supplier of test cases has to supply test cases whose type and number match the types and number of the arguments to the parameterised test, and this will be checked at compile time.

Yes, I wrote _JUnit5_ and _Scala_ in the same sentence; it is feasible to use JUnit5 directly with Scala test and system-under-test code using the Scala flavour of the `Trials` API. You are free to pull in whatever assertion library works with JUnit5 and Scala - I've been experimenting with [Expecty](https://github.com/eed3si9n/expecty), so far it works very nicely as the primary assertion language, using JUnit5's `assertThrows` to check that an exception is thrown.

What does this look like? Let's cutover the permutations example from before:

```scala
// Junit5...
import org.junit.jupiter.api.TestFactory
// Integration with JUnit5...
import com.sageserpent.americium.junit5.*
// Expecty assertions...
import com.eed3si9n.expecty.Expecty.assert

```

```scala
class DemonstrateJUnit5Integration {
  @TestFactory
  def thingsShouldBeInOrder(): DynamicTests = {
    val permutations: Trials[SortedMap[Int, Int]] =
      api.only(15).flatMap { size =>
        val sourceCollection = 0 until size

        api
          .indexPermutations(size)
          .map(indices => {
            val permutation = SortedMap.from(indices.zip(sourceCollection))

            assume(permutation.size == size)

            assume(SortedSet.from(permutation.values).toSeq == sourceCollection)

            permutation
          })
      }

      permutations
        .withLimit(15)
        .dynamicTests { permuted =>
          Trials.whenever(permuted.nonEmpty) {
            assert(permuted.values zip permuted.values.tail forall {
              case (left, right) =>
                left <= right
            })
          }
        }
  }
}
```

What's changed? For one thing, we have a test method that is decorated with JUnit5's `@TestFactory` annotation, this expects the method to yield some kind of Java abstraction of a series of `DynamicTest` instances. To avoid having to pollute your nice Scala test code with the names of coarse and ill-mannered Java types, there is a Scala type alias `DynamicTests` (note the plural) that is pulled in from the `import com.sageserpent.americium.junit5.*` import.

The rest of the code looks much the same - only instead of a call to `supplyTo` we see a call to `dynamicTests`, also pulled in via that import. This packages up our parameterised test and supply into something that JUnit5 can use, the rub being that we have used a type-checked call to connect the supplier and the parameterised test.

Those eagle-eyed readers will note the use of an assertion - this is pulled in by `import com.eed3si9n.expecty.Expecty.assert`, although the standard `Predef.assert` would also do. Try both out and see the difference!

The method `dynamicTests` is overloaded so that trials instances can be ganged together to supply multi-argument parameterised tests. This technique also works with trials whose test cases are tuple types too.

Note that the `Trials` instance in the example is the _Scala_ form, even though we are using JUnit5.

Java folk have not been neglected: there is a module class `com.sageserpent.americium.junit5.java.JUnit5` that provides a similar experience, only this time we do use the _Java_ form of `Trials`. Here's an example taken from the Javadoc for `JUnit5.dynamicTests`:

```java

import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import com.sageserpent.americium.junit5.java.JUnit5;
import java.util.Iterator;

class DemonstrateJUnit5Integration{
     @TestFactory
     Iterator<DynamicTest> dynamicTestsExample() {
         final TrialsScaffolding.SupplyToSyntax<Integer> supplier =
                 Trials.api().integers().withLimit(10);

         return JUnit5.dynamicTests(
                 supplier,
                 // The parameterised test: it just prints out the test case...
                 testCase -> {
                     System.out.format("Test case %d\n", testCase);
                 });
     }
}
```

This time we can't use the Scala type alias so we see `Iterator<DynamicTest>` in all of its glory, and revel in it because Scala developers are just namby-pamby purists anyway.

Again, there are overloads of `JUnit5.dynamicTests`, and these work both with ganged trials and trials of tuples.

***
Next topic: [Design and Implementation...]({% link docs/wiki-content/design-and-implementation.md %})