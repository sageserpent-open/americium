---
layout: default
title: Variations in Making Trials
parent: Getting Started
nav_order: 2
---

# Variations in Making a Trials Instance
{: .no_toc }

Choices, alternation, special cases
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Beyond Built-in Types

Supplying arbitrary integers, doubles, booleans etc is all very well, but sometimes we have things like **enumerations** that range over a fixed number of possibilities that are all equally as important - and unknown to Americium. What do we do then?

---

## Explicit Choices with `.choose()`

Let's revisit our simple test from the beginning - just print out the values of an enumeration:
```java
import javax.net.ssl.SSLEngineResult;

final Trials<SSLEngineResult.Status> trials = 
    Trials.api().choose(SSLEngineResult.Status.values());

trials.withLimit(10).supplyTo(System.out::println);
```

This passes:
```
CLOSED
OK
BUFFER_UNDERFLOW
BUFFER_OVERFLOW
```

Note how there are only four enumeration values in `SSLEngineResult.Status`, so even though we specified a limit of 10, Americium did not bother repeating the same trials.

{: .note }
> **`TrialsApi.choose`** can be used with any kind of type - the idea is that we want to explicitly set out the permitted values for Americium to choose from. They are not chosen in any particular order, though - and indeed the order of the output does not match the order of declaration of the enumeration's values in this case.

There are overloads of `TrialsApi.choose` that take each choice as a separate argument, or as an array (this was used above) or as an `Iterable`.

### Small Primes Example

So if we want to test a system with small primes as inputs and can't be bothered to Google for the Sieve of Eratosthenes, we simply set out our primes explicitly:
```java
Trials
    .api()
    .choose(1, 3, 5, 7, 11, 13, 17, 19, 23)
    .withLimit(10)
    .supplyTo(System.out::println);
```

---

## Mixing Different Trials with `.alternate()`

How about mixing up different kinds of trials that produce the same type of test case, but in different ways? Let's say we have a system under test that deals with any old integer, but we suspect that small values might be a problem, so we want to make sure we've covered a small range, say 1 to 10 while dipping in to a wider pool of numbers that might get quite large.

We could just give Americium a very large limit, cross our fingers and hope that we cover 1 to 10 in the process, but there is a much better way ... **alternation**.
```java
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.sageserpent.americium.java.Trials.api;

final Trials<Integer> anyOldIntegers = api().integers();

final Trials<Integer> veryImportantIntegers = api().choose(
    Stream.iterate(1, x -> 1 + x)
          .limit(10)
          .collect(Collectors.toList()));

final Trials<Integer> trials =
    api().alternate(anyOldIntegers, veryImportantIntegers);

trials.withLimit(20).supplyTo(System.out::println);
```

This yields:
```
7
-1955330156
4
1531869968
3
1806495736
5
1987104774
1840758034
8
2
203172046
1
-1111845053
1658077352
10
6
-685852310
-1423277862
9
```

See how the important numbers from 1 to 10 were all covered, but we had some general ones thrown in.

---

## Special Case Values with `.only()`

Finally, alternation is handy when we want to mix in some special case value, often a **sentinel value**, into the trials. This is like alternating between some trials and a choice of one value, but there is shorthand for this that is better:
```java
final Trials<Integer> anyOldNumberOfBytes = api().nonNegativeIntegers();

final Trials<Integer> noBytesSentinel = api().only(-1);

final Trials<Integer> trials =
    api().alternate(anyOldNumberOfBytes, noBytesSentinel);

trials.withLimit(10).supplyTo(System.out::println);
```

This prints:
```
-1
-1057358237
2128104420
637157859
1272878179
445778542
633033882
1885261282
273004927
-1367005977
```

Now we can ask `TrialsApi` to yield arbitrary values via its convenience methods, specify our own choice of values, alternate between different ways of producing the same kind of test case and throw in a special case too.

---

## Weighted Choices and Alternation

One last thing - we can **weight** both choices and alternation, let's see this with alternation:
```java
final Trials<Integer> evens = api().choose(
    Stream.iterate(0, x -> 2 + x).limit(20).collect(Collectors.toList()));

final Trials<Integer> odds = api().choose(
    Stream.iterate(1, x -> 2 + x).limit(20).collect(Collectors.toList()));

final Trials<Integer> trials =
    api().alternateWithWeights(
        Map.entry(1, evens), 
        Map.entry(2, odds));

trials.withLimit(10).supplyTo(System.out::println);
```

Observe that odd numbers occur roughly twice as often as even numbers:
```
12
17
19
3
24
3
19
20
5
31
```

{: .note }
> Those pesky eagle-eyed readers will note that **19 is duplicated** in the output! This is subtle, it comes down to Americium not actually enforcing uniqueness of test cases for reasons that are explained in the design and implementation topic - rather it enforces uniqueness in the manner in which a test case was internally derived.

That uniqueness actually holds because the weighting is implemented as if the weighted alternatives were duplicated in proportion to their weights - this means that the implementation doesn't try to unfairly 'cycle' weighted alternatives to make up the frequency, instead it picks fairly (and randomly) between all of the duplicates taken together across all the alternatives.

There is a `TrialsApi.chooseWithWeights` to complement `TrialsApi.alternateWithWeights`, try it out.

---

## Teaser: Simpler Generation Ahead

The generation of the even and odd numbers can be simplified and made more powerful - on to the next section...

---

{: .note-title }
> Key Takeaways
>
> - **`.choose()`** - Pick from explicit values (enums, arrays, iterables)
> - **`.alternate()`** - Mix different trial types that produce the same kind of values
> - **`.only()`** - Create a trial that yields a single sentinel value
> - **`.alternateWithWeights()` / `.chooseWithWeights()`** - Control frequency of alternatives
> - Choices don't shrink (all values equally valid)
> - Alternation lets you ensure important values are covered while also testing general cases