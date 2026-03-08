---
layout: default
title: Introducing Americium
parent: Getting Started
nav_order: 1
reviewed: true
---

# Introducing Americium to Your Tests
{: .no_toc }

Trials, supplying test cases to tests, shrinkage in action
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Trials API

Americium's API is built around the `Trials<Case>` generic interface. A trials instance supplies test case data fed to a test that takes a single test case as a parameter; the test repeatedly carries out a trial of whatever it tests using varying test cases.

We say that a trials instance **supplies test cases** to a parameterised test, each execution of the test being a **trial**. The type parameter of `Trials` is the type of the supplied test cases.

Loosely speaking, we can think of Trials as being some kind of fountain of test data to start with - but as the name suggests, there is a notion of a test failing or succeeding that is important - we'll come back to this in a bit...

---

## Your First Trial

Let's see a simple example in JShell:
```java
import com.sageserpent.americium.java.Trials;

final Trials<Integer> trials = Trials.api().integers(-5, 5);

trials.withLimit(10).supplyTo(System.out::println);
```

We always start with an **API object** that for Java folk is accessed via a static method in `com.sageserpent.americium.java.Trials` and for Scala folk is accessed via a method in the companion object for `com.sageserpent.americium.Trials` (note that the Scala flavour is the default in the package namespace, Java has to be called out explicitly).

The API objects both have a swathe of methods for getting a trials instance - we use `.integers` and supply lower and upper bounds to the test cases to supply.

Here, we've used `System.out::println` as our parameterised test. Unless our test cases have a very poor implementation of `.toString`, it is unlikely that this 'test' can fail, but it is instructive to see the output:
```
2
-5
5
4
1
-4
0
-3
-2
-1
```

So we asked for a `Trials<Integer>` to supply a range of integer test cases between -5 and 5, and it did that. Huzzah!

Looking closely, we note that there is **no repetition** in the supplied cases, and that one case is missing - the number 3. Now, we asked for a limit of at most 10 cases, and there are 11 integers between -5 and 5 inclusive, so that makes sense.

---

## The Limit is Required

We always have to control the number of trials performed, and this is what `.withLimit` is for. For example:
```java
final Trials<Integer> trials = Trials.api().integers();

trials.withLimit(20).supplyTo(System.out::println);
```

That overload of `.integers` uses a default range spanning the entire domain of integer values, so this combination will churn out:
```
797772800
-1955330156
2128104420
1531869968
637157859
1806495736
...
```

There are a lot of integers, so we need the limit to tell Americium that we're happy that it has tested enough or else it would have to slog through all 2^32 values.

In fact, we cannot avoid the limit - the `.supplyTo` method belongs to another generic interface `SupplyToSyntax<Case>`, and we get that via the bridging call to `.withLimit`, so we're sure some limit is enforced.

---

## Shrinkage in Action

Let's make the test more exciting:
```java
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

final Trials<Integer> trials = Trials.api().integers();

trials.withLimit(1000).supplyTo(x -> {
    final int xSquared = x * x;
    assertThat(xSquared / x, equalTo(x));
});
```

What happens? We see a test failure:
```
java.lang.AssertionError: 
Expected: <-46367>
     but: was <46262>
```

Clearly there has been arithmetic overflow and thus wraparound when the test case was squared. Let's modify that test a little to peek at what's going on...
```java
trials.withLimit(1000).supplyTo(x -> {
    final int xSquared = x * x;
    
    try {
        assertThat(xSquared / x, equalTo(x));
    } catch (Throwable e) {
        System.out.println(e);
        throw e;
    }
});
```

Now we see this:
```
java.lang.AssertionError: Expected: <797772800> but: was <1>
java.lang.AssertionError: Expected: <637157859> but: was <2>
java.lang.AssertionError: Expected: <179318108> but: was <-9>
java.lang.AssertionError: Expected: <83665336> but: was <-23>
java.lang.AssertionError: Expected: <72142783> but: was <-1>
java.lang.AssertionError: Expected: <58037089> but: was <23>
java.lang.AssertionError: Expected: <9360959> but: was <174>
...
java.lang.AssertionError: Expected: <-46367> but: was <46262>
```

Americium notices the very first failing trial, where a whopping **797772800** was squared, then goes into **shrinkage mode**, where it tries to maximise the shrinkage of the test case that causes the test to fail, resulting in the test case of **-46367** that we saw earlier.

While it is shrinking, it intercepts the exceptions thrown by the test and keeps going; once it has settled on a final failing test case, it propagates the exception out of the call to `.supplyTo`, so by default, you will see the maximally shrunk test case.

---

## Understanding "Maximally Shrunk"

The reason for using phrase 'maximally shrunk' and not just plain 'minimised' is that there is no guarantee that the test case is truly a minimum - there may be values that are even more shrunk, nor have we have discussed what we would mean by minimum yet - in this case the values oscillated as shrinkage increased, switching back and forth from positive to negative.

Doing some trial and error manual testing yields the best shrinkage of **-46341**. If we increase the limit in the test above from 1000 to 1500, we will find that Americium roots out the best value of -46341, but even so, that first maximally shrunk case of -46367 wasn't bad. In fact, Americium can shrink down to -47445 if run with a miserly limit of 10 trials.

{: .important }
> **The moral of the story:** If you want to get good shrinkage of your failing test case, give Americium a high enough limit - experiment with it.

---

## More Trials Than the Limit?

The eagle-eyed will observe that more test cases can be supplied to the test than the limit if a failure is observed. We'll get to that in the section about configuration, but for now, the rule is simply that **as long as the trials all succeed, Americium will not supply more test cases than the limit**.

---

## Available Trial Types

To summarise, we're seen how to:
- Get a trials of integers
- Supply test cases from it to a test
- See what happens when a test trial fails

Great! Now take a look at `TrialsApi` in your preferred language API flavour and note the plethora of convenience methods for building trials of:

- Integers, longs, big integers
- Doubles, big decimals
- Bytes, characters, strings
- Booleans
- Instants

Some are overloaded to take ranges, and some have a mysterious **shrinkage target** too - we'll talk about that when we revisit shrinkage in a later topic.

---

{: .note-title }
> Key Takeaways
>
> - `Trials<Case>` is the core interface for generating test cases
> - Always use `.withLimit()` to control how many trials to run
> - **Shrinkage is automatic** - Americium finds minimal failing cases
> - Higher limits → better shrinkage quality
> - Many built-in trial types available via `Trials.api()`