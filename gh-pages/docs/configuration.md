---
layout: default
title: "Configuration buttons, dials and levers"
parent: Wiki Content
nav_order: 7
---

# Configuration buttons, dials and levers
{: .no_toc }

Case limit strategies, seeding, complexity, controlling shrinking
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## Case Limit Strategies

Remember that test that failed with strings ending in "are" from the previous topic? Let's mess around and have the test fail if "are" comes anywhere in the string - but again, we don't allow just plain "are" by itself:

```java
try {
    final String suffix = "are";

    final int suffixLength = suffix.length();

    api().characters('a', 'z').collections(Builder::stringBuilder).filter(caze -> caze.length() >
                                                                                  suffixLength).withLimit(20000).supplyTo(input -> {
        try {
            assertThat(input, not(containsString(suffix)));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

This test drags on for a **long** time, and yields:

```
ared
arew
gare
qare
tare
pare
arev
area
arem
arec
care
iare
eare
jare
aret
bare
arei
ares
hare
areo
arex
arek
sare
areb
aren
uare
ware
fare
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: not a string containing "are"
     but: was "fare"
```

We've lost "rare" and ended up with "fare", which is a good enough trade. What a long time that test took to run, though. Maybe this was connected to using a limit of 20000 - perhaps that was too much? We could mess around with the limit and see if we can still get a shrunk failing test case in a reasonable amount of time, but suppose instead that we start with a time budget in mind - we'll wait 5 seconds for a failure to be found and no more, we're busy people, time is money, gotta hustle, get out of the road:

```java
try {
    final String suffix = "are";

    final int suffixLength = suffix.length();

    api()
            .characters('a', 'z')
            .collections(Builder::stringBuilder)
            .filter(caze -> caze.length() >
                            suffixLength)
            .withStrategy(cycle -> CasesLimitStrategy.timed(
                    Duration.ofSeconds(5)))
            .supplyTo(input -> {
                try {
                    assertThat(input, not(containsString(suffix)));
                } catch (Throwable throwable) {
                    System.out.println(input);
                    throw throwable;
                }
            });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

This results in:

```
ared
arew
gare
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: not a string containing "are"
     but: was "gare"
```

From the point of view of the test case being shrunk, this is just as good as the previous offering, but took a lot less time.

Now, if we actually time this test, it still takes longer than 5 seconds to run - so what's going on?

Let's peek some more at the progress by modifying what's passed to `.withStrategy`:

```java
final Instant startTime = Instant.now();

try {
    final String suffix = "are";

    final int suffixLength = suffix.length();

    api()
            .characters('a', 'z')
            .collections(Builder::stringBuilder)
            .filter(caze -> caze.length() >
                            suffixLength)
            .withStrategy(cycle -> {
                final Instant rightNow = Instant.now();

                System.out.format("Elapsed time in seconds: %s, number of previous cycles:  %s\n",
                                  Duration
                                          .between(startTime, rightNow)
                                          .getSeconds(),
                                  cycle.numberOfPreviousCycles());

                return CasesLimitStrategy.timed(
                        Duration.ofSeconds(5));
            })
            .supplyTo(input -> {
                try {
                    assertThat(input, not(containsString(suffix)));
                } catch (Throwable throwable) {
                    System.out.println(input);
                    throw throwable;
                }
            });
} catch (TrialsScaffolding.TrialException exception) {
    final Instant rightNow = Instant.now();

    System.out.format("Total elapsed time in seconds: %s\n",
                      Duration
                              .between(startTime, rightNow)
                              .getSeconds());
    System.out.println(exception);
}
```

We can see elapsed times:

```
Elapsed time in seconds: 0, number of previous cycles:  0
ared
Elapsed time in seconds: 4, number of previous cycles:  1
arew
Elapsed time in seconds: 5, number of previous cycles:  2
gare
Elapsed time in seconds: 6, number of previous cycles:  3
Elapsed time in seconds: 11, number of previous cycles:  4
Total elapsed time in seconds: 16
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: not a string containing "are"
     but: was "gare"
```

Aha! So Americium works in cycles - the strategy's time limit is applied to each cycle. The first cycle is the default one, if no failing trial is encountered then that's it. Otherwise Americium starts shrinking; each succeeding cycle represents an attempt by Americium to produce a failing trial at a new level of shrinkage - we see above that in this case all but the last two yielded a failure.

In general, we can call `.withStrategy` with any callback that takes a `CaseSupplyCycle` - that tells us where we've got to overall - and returns a `CasesLimitStrategy` - this controls how long Americium will persevere with next cycle.

There are two canned implementations of `CasesLimitStrategy` - the aforementioned `CasesLimitStrategy.timed` and `CasesLimitStrategy.counted`. The latter is given a limiting number of test cases to produce in the cycle, and a _maximum starvation ratio_.

Starvation refers to potential test cases discarded by Americium because they don't pass filtration, are too complex (we'll get to this shortly), or are built up via duplicate recipes; these test cases do not result in trials. The ratio is between the number of test cases that result in starvation versus those that make it through to a trial, regardless of whether the trial is a success or failure. So a value of 0 would mean that we simply will not tolerate any starvation and will give up as soon as any takes place, whereas 1 would mean that we expect as many cases to be discarded as those that come to trial, so will keep going as long the starvation doesn't outpace the trials.

## Using the fluent configuration API

By default, Americium uses a fixed internal seed for its pseudorandom behaviour - this is what ultimately drives the variation in test cases from one trial to another in a call to `.supplyTo`. So by default, each time a test using Americium is run, it will see the same progression of test cases in its trials - well, at least up to the first failing trial or the end of the cycle.

That's nice when we want to know that our tests are repeatable, but there is a tradeoff - sticking to the same test cases means that if we really want to cover a lot of trials, then we'll have to have a test that runs for a long time; this tends to be unpopular for both local development (we want fast feedback) and CI pipelines (we want new builds / deployments asap).

Rather than sacrifice timeliness, we can cover a wider base of trials by allowing the seed to vary from one run to another. If we want explicit control of the seed, then we use a fluent configuration method in `SupplyToSyntax` - so this comes between the `.withLimit/withStrategy` and the `.supplyTo`:

```<trials instance>.withLimit(10).withSeed(<some long value>).supplyTo(<test lambda>);```

We can set the seed according to the date or CI build number, host name or whatever.

In fact, we can just let Americium to this for us implicitly by setting a JVM property:

```-Dtrials.nondeterministic=true```

This will use `java.util.Random` in non-repeatable seed mode, so different runs should yield different progression of test cases (as long as the trials instance allows variation, that is). This property takes precedence over an explicit seed when set to true.

Remember complexity being mentioned in the context of shrinkage? Americium imposes a complexity limit by default; this prevents it from generating monstrously long lists via `.immutableLists` etc - this is especially important if trials are formulated by recursion, as we have seen before in the calculator example.

We configure an explicit complexity limit like this:

```<trials instance>.withLimit(10).withComplexityLimit(<some integer value, at least 1>).supplyTo(<test lambda>);```

There is a subtlety with setting a complexity limit: say we have a list of lists, where the nested lists all have a specified length:

```java
api()
        .integers(1, 5)
        .immutableListsOfSize(50)
        .immutableLists()
        .withLimit(10)
        .withComplexityLimit(4)
        .supplyTo(System.out::println);
```

Yes, crazy, but the customer is always right ... anyway, this yields:

```
[[3, 2, 4, 3, 3, 4, 3, 2, 2, 5, 2, 3, 3, 3, 3, 5, 2, 1, 5, 2, 1, 3, 4, 4, 3, 2, 3, 3, 1, 3, 3, 1, 2, 4, 3, 1, 3, 3, 4, 3, 4, 4, 4, 2, 2, 1, 2, 4, 4, 4], [5, 4, 3, 2, 5, 1, 5, 4, 2, 4, 2, 4, 4, 3, 5, 1, 2, 1, 2, 4, 4, 5, 5, 2, 3, 2, 2, 3, 2, 1, 4, 4, 5, 2, 4, 4, 3, 3, 5, 1, 1, 4, 2, 3, 4, 3, 1, 1, 1, 4], [4, 4, 5, 4, 2, 5, 4, 3, 3, 3, 2, 4, 2, 2, 3, 2, 3, 2, 4, 4, 4, 4, 2, 3, 3, 2, 4, 3, 3, 2, 5, 2, 2, 2, 3, 5, 5, 1, 2, 4, 1, 5, 3, 1, 5, 3, 4, 3, 4, 3]]
[]
[[4, 4, 3, 4, 2, 5, 3, 2, 3, 4, 1, 3, 2, 1, 4, 4, 2, 3, 2, 4, 3, 5, 2, 2, 4, 3, 2, 4, 3, 5, 4, 3, 3, 4, 3, 3, 4, 3, 3, 3, 4, 1, 4, 2, 2, 4, 3, 2, 3, 3]]
[[2, 2, 4, 2, 3, 5, 4, 2, 3, 4, 2, 5, 4, 4, 3, 2, 3, 5, 2, 4, 3, 5, 2, 4, 2, 2, 5, 4, 2, 2, 2, 1, 3, 1, 2, 3, 2, 3, 4, 3, 2, 2, 1, 1, 4, 1, 2, 1, 5, 3]]
[[2, 4, 1, 4, 2, 5, 5, 4, 4, 5, 3, 2, 3, 4, 1, 1, 1, 2, 4, 4, 2, 4, 2, 5, 2, 1, 1, 3, 2, 4, 3, 4, 3, 3, 2, 3, 1, 2, 3, 5, 4, 1, 1, 2, 2, 1, 3, 2, 2, 4]]
[[4, 5, 2, 4, 3, 3, 3, 2, 3, 2, 2, 4, 2, 2, 3, 2, 4, 5, 2, 3, 3, 2, 4, 2, 5, 2, 4, 3, 3, 4, 4, 3, 2, 2, 1, 1, 3, 4, 3, 1, 4, 4, 4, 4, 3, 4, 4, 4, 4, 3]]
[[3, 3, 3, 3, 4, 3, 5, 4, 1, 2, 2, 3, 4, 3, 2, 2, 4, 2, 4, 1, 4, 2, 4, 2, 3, 3, 3, 5, 5, 1, 4, 4, 3, 2, 3, 3, 4, 3, 2, 1, 2, 1, 4, 3, 2, 3, 1, 3, 4, 4]]
[[2, 3, 3, 3, 2, 2, 3, 2, 5, 2, 4, 4, 4, 3, 2, 5, 2, 2, 4, 2, 5, 4, 1, 3, 4, 2, 5, 3, 2, 3, 2, 4, 5, 4, 2, 3, 3, 3, 5, 3, 4, 5, 2, 2, 5, 3, 2, 1, 3, 4], [2, 2, 2, 5, 3, 1, 3, 3, 5, 2, 2, 2, 2, 4, 4, 4, 5, 2, 1, 2, 3, 1, 1, 2, 4, 3, 3, 3, 3, 5, 3, 4, 3, 3, 2, 4, 4, 3, 1, 1, 4, 3, 1, 3, 4, 5, 5, 3, 4, 2], [1, 5, 2, 4, 4, 3, 4, 3, 1, 3, 5, 4, 4, 3, 1, 2, 4, 3, 5, 4, 3, 2, 2, 4, 3, 5, 4, 2, 5, 2, 4, 4, 3, 4, 3, 4, 4, 5, 1, 1, 4, 2, 3, 3, 4, 3, 5, 2, 2, 3]]
[[5, 4, 2, 5, 2, 4, 1, 2, 2, 3, 4, 4, 2, 4, 2, 3, 2, 1, 2, 5, 2, 4, 2, 1, 4, 5, 4, 2, 3, 2, 1, 3, 2, 2, 4, 2, 3, 2, 1, 1, 4, 3, 2, 3, 2, 4, 3, 4, 3, 4]]
[[2, 3, 4, 3, 4, 3, 3, 4, 5, 2, 5, 2, 3, 2, 2, 1, 2, 3, 2, 4, 3, 1, 4, 1, 2, 3, 4, 2, 5, 3, 1, 5, 2, 2, 1, 1, 4, 4, 4, 4, 2, 2, 1, 5, 3, 5, 1, 3, 2, 1], [4, 2, 3, 2, 4, 2, 3, 4, 4, 2, 3, 3, 5, 4, 1, 3, 3, 4, 2, 4, 3, 2, 5, 5, 4, 4, 2, 2, 1, 2, 3, 3, 1, 1, 4, 2, 4, 4, 4, 5, 3, 2, 2, 5, 4, 4, 5, 3, 3, 3], [2, 4, 3, 3, 4, 5, 4, 3, 1, 2, 3, 5, 3, 2, 2, 2, 2, 4, 1, 3, 5, 2, 4, 4, 3, 2, 3, 4, 2, 2, 2, 5, 1, 2, 4, 2, 4, 3, 2, 3, 3, 3, 4, 2, 2, 3, 4, 3, 3, 2]]
```

We see that the outer lists are constrained by the complexity limit, but the nested lists have leeway to achieve the desired length. Americium takes the view that if its asked to make a collection of a given size, then the user knows what they're doing and it should comply, but it will still heed the complexity limit elsewhere.

This also holds for things nested inside fixed-size collections:

```java
    api()
            .integers(1, 5)
            .immutableLists()
            .immutableListsOfSize(10)
            .immutableLists()
            .withLimit(15)
            .withComplexityLimit(10)
            .supplyTo(System.out::println);
```

We have increased the complexity limit a little to give the outermost and innermost lists a chance to be built, and we see:

```
[]
[[[1, 3], [], [], [], [4, 4], [2, 1], [], [], [4], []]]
[[[1], [2, 2], [], [], [], [], [], [4], [], []]]
[[[], [], [2, 3, 1, 3], [5], [], [], [], [], [], [5]]]
[[[2, 2, 1], [2, 2], [], [], [], [], [], [], [], []]]
[[[4, 2, 4], [], [3], [], [2, 5], [4, 3, 1, 3], [2], [], [], [4, 2]]]
[[[], [], [], [], [], [], [2, 2, 2], [4], [], []], [[5], [], [2, 2, 3], [1], [], [], [], [2, 1], [], []]]
[[[5], [], [5], [5], [2], [], [], [2, 3], [3, 4], []]]
[[[2], [], [3, 4], [4], [], [4], [3], [], [], []], [[], [], [], [], [2, 4], [], [5, 3, 3, 3], [], [], [2]]]
[[[4, 4, 2, 3], [], [], [], [1], [], [4, 4], [], [], []]]
[[[4], [], [4, 2, 4], [4], [4], [], [], [4], [1], [3]]]
```

The outermost and innermost lists don't get too big, but the intermediate lists are still comprised of precisely 10 items.

Two more fluent configuration settings and we can take a rest ... both of these control shrinking.

We can specify how many levels of successful shrinkage are applied like this:

```<trials instance>.withLimit(10).withShrinkageAttemptsLimit(<some integer value>).supplyTo(<test lambda>);```

If we use 0, then the original failing test case is the one Americium will throw an exception for - so no shrinkage at all. 1 allows one level of shrinkage to be applied, and so on.

For the ultimate in shrinkage control, we have:

```<trials instance>.withLimit(10).withShrinkageStop(<shrinkage stop>).supplyTo(<test lambda>);```

It works in a similar fashion to the callback provided to `.withStrategy`, and both can be used in conjunction. Consult the `ShrinkageStop` interface's Javadoc.

***
Next topic: [Awkward tests...]({% link docs/wiki-content/awkward-tests.md %})