---
layout: default
title: Building Up Test Cases
parent: Getting Started
nav_order: 3
reviewed: true
---

# Building Up Test Cases
{: .no_toc }

Collections, mapping, filtering, flat-mapping and recursion
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## Beyond Simple Types

It's great to be able to call on `TrialsApi` to provide canned trials of integers, longs, doubles, strings and Booleans; even better that we can supply our own choices of test cases to draw on, and mix between them as well as throwing in a special case.

However, tests frequently require much more **complex test cases** that might be a fully configured system under test, perhaps interacting with some complex query or plan of interactions. If so, how do we build up these complex test cases?

We have five possibilities:

1. Make a trials of **collections** out of one (or several) trials of a base element type
2. Transform from a trials of one type to a trials of another - **mapping**
3. **Filtering** out test cases we don't want
4. Combine the test cases of several trials together to make a trials of a type assembled from the individual cases - **flat-mapping**
5. Putting the previous techniques together in a **recursive definition** of a trials

---

## Collections

Let's test reducing a stream of integers by summation - surely we would expect positive integers to sum to a non-negative value?
```java
import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

final Trials<ImmutableList<Integer>> lists = 
    api().integers(1, Integer.MAX_VALUE).immutableLists();

lists.withLimit(100).supplyTo(list -> {
    assertThat(list.stream().reduce(Integer::sum).orElse(0), 
               greaterThanOrEqualTo(0));
});
```

Um, no:
```
java.lang.AssertionError: 
Expected: a value equal to or greater than <0>
     but: <-2117575447> was less than <0>

Case:
[1013174718, 1164217131]
```

Anyway, we called `Trials.immutableLists` on our `Trials<Integer>` and got a `Trials<ImmutableList<Integer>>`. This yields test cases that are lists of varying size from empty and singleton lists to very large ones indeed that are populated with elements drawn from the original trials instance.

### Available Collection Methods

There are several such methods available:

| Collection                    | Java                   | Scala         |
|:------------------------------|:-----------------------|:--------------|
| **List**                      | `.immutableLists`      | `.lists`      |
| **Set**                       | `.immutableSets`       | `.sets`       |
| **Set** *(sorted by element)* | `.immutableSortedSets` | `.sortedSets` |
| **Map**                       | `.immutableMaps`       | `.maps`       |
| **Map** *(sorted by key)*     | `.immutableSortedMaps` | `.sortedMaps` |
| **Custom Collection**         | `.collections`         | `.several`    |

### Cartesian Product Collections

In the Java API, there is also a nice method `TrialsApi.immutableLists`:
```java
final Trials<ImmutableList<Integer>> lists = api().immutableLists(List.of(
    api().choose(0, 1, 2),
    api().choose(-1, -2),
    api().only(99)));

lists.withLimit(10).supplyTo(System.out::println);
```

You'll get the idea:
```
[0, -1, 99]
[1, -1, 99]
[2, -1, 99]
[1, -2, 99]
[0, -2, 99]
[2, -2, 99]
```

Observe how Americium works its way through the **Cartesian product** of the various contributions from the underlying trials. Again, there is a `TrialsApi.collections` in the Java API for those who need other collections.

{: .note }
> In the Scala API, `TrialsApi.sequences` does the same job.

---

## Mapping

Let's revisit the example from the previous topic:
```java
final Trials<Integer> evens = api().integers(0, 19).map(x -> 2 * x);

final Trials<Integer> odds = api().integers(0, 19).map(x -> 1 + 2 * x);

final Trials<Integer> trials =
    api().alternateWithWeights(Map.entry(1, evens), Map.entry(2, odds));

trials.withLimit(10).supplyTo(System.out::println);
```

This gives us:
```
18
3
33
19
31
36
37
30
21
1
```

Again, the odd numbers occur roughly twice as often as the even numbers. See how we have used **`.map`** to transform the result of `.integers` into two completely distinct trials instances - one generating even numbers and the other odd numbers.

### Transforming Types with Mapping

We could also transform the type if needs be:
```java
final Trials<String> asteriskRuns =
    api().choose(1, 2, 6, 9).map(repeats -> {
        final StringBuffer buffer = new StringBuffer();
        
        int countDown = repeats;
        
        while (0 < countDown--) {
            buffer.append('*');
        }
        
        return buffer.toString();
    });

asteriskRuns.withLimit(10).supplyTo(System.out::println);
```

Yielding:
```
*********
******
*
**
```

---

## Filtering

When we made trials for even and odd numbers above, we used a **synthetic approach** - the mapping forces the trials to generate the right kind of numbers by construction. We can also use a more **brute-force approach** and throw away test cases that don't suit:
```java
final Trials<Integer> numbers = api().integers(0, 39);

final Trials<Integer> evens = numbers.filter(x -> 0 == x % 2);

final Trials<Integer> odds = numbers.filter(x -> 1 == x % 2);
```

{: .warning }
> **Be careful with this approach** - as long as most cases pass the filter, all will be well, but if the filter works by sieving through the vast majority of test cases in search of a tiny number of golden nuggets, this is likely to exhaust Americium and you won't see many trials being executed. **Prefer the synthetic approach** in those cases!

---

## Flat-mapping

Let's list strings where there must be at least 1 string item and at most 10:
```java
final Trials<ImmutableList<String>> stringLists = api()
    .integers(1, 10)
    .flatMap(api().strings()::immutableListsOfSize); 

stringLists.withLimit(10).supplyTo(System.out::println);
```

See how we take the output from one trials, the `api.integers(1, 10)` and use it as a length constraint on the specification of another trials instance that yields lists. We do this via a **method reference**, `api().strings()::immutableListsOfSize` that implicitly takes the length - we could also have written `length -> api().strings().immutableListsOfSize(length)`.

### Nested Flat-maps

We can go crazy and nest flat-maps:
```java
final Trials<ImmutableList<String>> filenameLists = api()
    .integers(1, 10)
    .flatMap(api()
        .choose("FilenameOne", "FilenameTwo", "FilenameThree")
        .flatMap(stem -> api()
            .choose("Huey", "Duey", "Louie")
            .map(suffix -> stem + "." + suffix))::immutableListsOfSize);

filenameLists.withLimit(3).supplyTo(System.out::println);
```

We get this:
```
[FilenameOne.Duey, FilenameTwo.Louie, FilenameThree.Duey, 
 FilenameTwo.Huey, FilenameOne.Huey, FilenameTwo.Duey, FilenameThree.Huey]
[FilenameTwo.Huey, FilenameOne.Duey, FilenameTwo.Huey, FilenameTwo.Huey, 
 FilenameTwo.Duey, FilenameThree.Duey, FilenameOne.Huey, FilenameTwo.Huey]
[FilenameThree.Louie, FilenameThree.Louie, FilenameTwo.Duey, 
 FilenameThree.Louie, FilenameOne.Duey, FilenameTwo.Huey]
```

The idea is to make a sequence of progressively nested flat-maps, where the parameters from any of the enclosing flat-maps can be used to control or contribute to the more nested ones; the end result then bubbles back up from the innermost trials instance in the flat-map sequence.

Look carefully and see that the **size parameter** from the outer flat-mapping is used to control the trials made by `.immutableListsOfSize`, whereas the **stem parameter** from the inner flat-mapping contributes directly to the synthesis of a string inside a mapping.

{: .tip }
> Typically, the last entry in the chain only needs a call to `.map` to make the final trials instance, but this is not a hard-and-fast rule.

---

## Recursion

Putting these ideas together, let's build up a trials that supplies string expression test cases for a calculator:
```java
class Module {
    public static Trials<String> calculation() {
        final Trials<String> constants =
            api().integers(1, 100).map(x -> x.toString());

        final Trials<String> unaryOperatorExpression =
            calculation().map(expression -> 
                String.format("-(%s)", expression));

        final Trials<String> binaryOperatorExpression =
            calculation().flatMap(lhs -> api()
                .choose("+", "-", "*", "/")
                .flatMap(operator -> calculation().map(rhs -> 
                    String.format("(%s) %s (%s)", lhs, operator, rhs))));

        return api().alternate(constants,
                               unaryOperatorExpression,
                               binaryOperatorExpression);
    }
}

Module.calculation().withLimit(10).supplyTo(System.out::println);
```

This all seems straightforward enough - but **it doesn't work**, because of infinite recursion. Oops.

### Delayed Evaluation with `api().delay()`

We can remedy this by introducing **delayed evaluation**:
```java
class Module {
    public static Trials<String> calculation() {
        final Trials<String> constants =
            api().integers(1, 100).map(x -> x.toString());

        final Trials<String> unaryOperatorExpression =
            api().delay(() -> calculation().map(expression -> 
                String.format("-(%s)", expression)));

        final Trials<String> binaryOperatorExpression =
            api().delay(() -> calculation().flatMap(lhs -> api()
                .choose("+", "-", "*", "/")
                .flatMap(operator -> calculation().map(rhs -> 
                    String.format("(%s) %s (%s)", lhs, operator, rhs)))));

        return api().alternate(constants,
                               unaryOperatorExpression,
                               binaryOperatorExpression);
    }
}
```

Now it works - **delaying the recursive calls** with `TrialsApi.delay` prevents direct infinite recursion; instead the recursion is done on demand as Americium unfolds the various alternatives.

{: .note }
> Note that only the **leading recursive calls** need to be delayed - those within a flat-map are already implicitly delayed by virtue of being within a lambda expression.

The result is a bit rough around the edges, but can be improved - try experimenting with passing context down the calls to `Module.calculation`:
```
49
(-(-(14))) * (-(-((89) + (-(61)))))
-(-(-(-(85))))
-(7)
71
-((-(((-(-(67))) / (20)) * (-(-(-(-(-(41)))))))) / (-((-((49) * ...
9
(49) + (((57) / (64)) - (31))
-(22)
50
```

---

{: .note-title }
> Key Takeaways
>
> - **Collections** - Transform element trials into collection trials (`.immutableLists()`, `.immutableSets()`, etc.)
> - **Mapping** - Transform types while preserving shrinkage (`.map()`)
> - **Filtering** - Remove unwanted cases (`.filter()`) - use sparingly!
> - **Flat-mapping** - Combine trials where one depends on another (`.flatMap()`)
> - **Recursion** - Use `api().delay()` to prevent infinite recursion
> - Flat-maps can be nested to build complex structures
> - Only leading recursive calls need delaying