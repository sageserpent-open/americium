---
layout: default
title: "JUnit5 Integration"
parent: Wiki Content
nav_order: 9
---

# JUnit5 Integration
{: .no_toc }

Going with the flow
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Americium takes a lean and mean approach; other than supplying test cases and shrinking them down, it doesn't have much to say about how we want to structure tests, what kind of assertion language to use, the best approach to sharing tests between implementations, setup and teardown and so forth.

However, JUnit is commonly used in Java development, and it is likely that your favourite IDE sports some kind of integration with it. In the JUnit5 incarnation, there is a nice `@ParameterizedTest` annotation that allows a test taking one or more parameters to be run several times against varying actual arguments. Sounds familiar?

Now, the support for parameterised testing in JUnit5 out of the box is nice, but rudimentary - either we have to supply the test cases by hand as an explicit list in code or a file, or generate them in some arbitrary manner that JUnit5 does not help with. There is no concept of test case shrinkage either - the individual tests either pass or fail, and that's it.

It does generalise test setup and teardown to work with each test run, and it looks cool though to see lots of test runs in IntelliJ...

Not to be outdone, Americium offers an _optional_ integration with JUnit5 that expresses the trials framework in a similar fashion to `@ParameterizedTest` - this includes the support for generalising setup and teardown and the funky IDE integration.

> **Artifact Required:** The JUnit5 integration features described on this page are provided by the separate `americium-junit5` artifact (since version 1.26.0). Add this dependency to your project:
> ```scala
> libraryDependencies += "com.sageserpent" %% "americium-junit5" % "x.y.z"
> ```

> **Package Information**
>
> **Java users:** Import from `com.sageserpent.americium.junit5`
> ```java
> import com.sageserpent.americium.junit5.TrialsTest;
> import com.sageserpent.americium.junit5.ConfiguredTrialsTest;
> ```
>
> **Scala users:** Import from `com.sageserpent.americium.junit5`
> ```scala
> import com.sageserpent.americium.junit5.*
> ```

## @TrialsTest

Remember the `Tiers` example? Let's integrate this with JUnit5:

```java
import com.sageserpent.americium.junit5.TrialsTest;

public class TiersTest {
    private final static Trials<ImmutableList<Integer>> queryValueLists = api()
            .integers(-1000, 1000)
            .immutableLists()
            .filter(list -> !list.isEmpty());


    private final static Trials<Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>>>
            testCases =
            queryValueLists.flatMap(queryValues -> {
                final int minimumQueryValue =
                        queryValues.stream().min(Integer::compareTo).get();

                // A background is a (possibly empty) run of values that are
                // all less than the query values.
                final Trials<ImmutableList<Integer>> backgrounds = api()
                        .integers(Integer.MIN_VALUE, minimumQueryValue - 1)
                        .immutableLists();

                // A section is either a query value in a singleton list, or
                // a background.
                final List<Trials<ImmutableList<Integer>>> sectionTrials =
                        queryValues
                                .stream()
                                .flatMap(queryValue ->
                                                 Stream.of(api().only(
                                                                   ImmutableList.of(
                                                                           queryValue)),
                                                           backgrounds))
                                .collect(Collectors.toList());

                sectionTrials.add(0, backgrounds);

                // Glue the trials together and flatten the sections they
                // yield into a single feed sequence per trial.
                final Trials<ImmutableList<Integer>> feedSequences =
                        api().immutableLists(sectionTrials).map(sections -> {
                            final ImmutableList.Builder<Integer> builder =
                                    ImmutableList.builder();
                            sections.forEach(builder::addAll);
                            return builder.build();
                        });
                return feedSequences.map(feedSequence -> Tuple.tuple(queryValues,
                                                                     feedSequence));
            });

    @TrialsTest(trials = "testCases", casesLimit = 10)
    void tiersShouldRetainTheLargestElements(Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>> testCase) {
        final ImmutableList<Integer> queryValues = testCase._1();
        final ImmutableList<Integer> feedSequence = testCase._2();

        System.out.format("Query values: %s, feed sequence: %s\n",
                          queryValues,
                          feedSequence);

        final int worstTier = queryValues.size();

        final Tiers<Integer> tiers = new Tiers<>(worstTier);

        feedSequence.forEach(tiers::add);

        final ImmutableList.Builder<Integer> builder =
                ImmutableList.builder();

        int tier = worstTier;

        int previousTierOccupant = Integer.MIN_VALUE;

        do {
            final Integer tierOccupant = tiers.at(tier).get();

            assertThat(tierOccupant,
                       greaterThanOrEqualTo(previousTierOccupant));

            builder.add(tierOccupant);

            previousTierOccupant = tierOccupant;
        } while (1 < tier--);

        final ImmutableList<Integer> arrangedByRank = builder.build();

        assertThat(arrangedByRank,
                   containsInAnyOrder(queryValues.toArray()));
    }
}
```

We've decanted the trials instances into the test class, `TiersTest` as final static fields - they are immutable, so that fits nicely. The test code itself goes into a method that looks a lot like a test annotated with `@TrialsTest` instead of the usual `@Test` that ships with JUnit5:

```java
@TrialsTest(trials = "testCases", casesLimit = 10)
    void tiersShouldRetainTheLargestElements(Tuple2<ImmutableList<Integer>,
            ImmutableList<Integer>> testCase) .....
```

If you've used `@ParameterisedTest` before, this should seem familiar - note that Americium's integration is simpler; `@ParameterisedTest` needs a supporting `@ValueSource` / `@MethodSource` or something similar to set up the injection of test cases, whereas `@TrialsTest` does it all in one place.

The annotation parameters `trials` names the static field that has the trials instance used to supply the test cases; this is analogous to `@MethodSource`'s `value` parameter. The parameter `casesLimit` does the same thing as `.withLimit`, and there are parameters `complexity` and `shrinkageAttempts` too, if you need them.

The results shown by IntelliJ are: ![](https://raw.githubusercontent.com/sageserpent-open/americium/4ced3f413c91ffee5e5587f48f137593cae03d93/screenshots/TiersInJUnit5.png)

That's nice - we can see each trial's run, the test case passed to each trial and the output from each trial. Other IDEs have similar support for JUnit5. We can execute an individual trial in IntelliJ by right clicking on a trial, check your own favourite IDE for similar functionality.

Picking apart the `Tuple2` looks a bit hokey - it turns out we can do better here:

```java
    @TrialsTest(trials = "testCases", casesLimit = 10)
    void tiersShouldRetainTheLargestElements(ImmutableList<Integer> queryValues,
                                             ImmutableList<Integer> feedSequence) {
        System.out.format("Query values: %s, feed sequence: %s\n",
                          queryValues,
                          feedSequence);

...
```

The integration matches the two arguments with a tupled test case and unpicks it for us prior to running the trial. This approach can be mixed and matched, where tupled and non-tupled trials can be ganged together with `.and` and unpicked into multiple arguments. When multiple trials are ganged together, it is possible to unpick individual tuples into clumps of arguments or leave them as tuple arguments according to preference.

What happens if we use `@BeforeEach` and `@AfterEach` to annotate setup and teardown methods, respectively? The integration will execute test setup before each trial and teardown after each trial - so each trial gets its own clean environment to run in. `@BeforeAll` and `@AfterAll` run prior to all tests in the test suite, be they annotated with `@Test`, `@ParameterizedTest` or `@TrialsTest`.

By default JUnit5 uses per-method test lifecycle for its tests. This mandates that the trials instance named by `@TrialsTest` needs to be _static_, as should the setup and teardown methods annotated with `@BeforeEach` and `@AfterEach`. If we wanted to use non-static definitions, then the test lifecycle would need to be per-class - use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` on the test class.

## Shrinkage

Let's revisit the test for `PoorQualitySetMembershipPredicate`:

```java
public class SetMembershipPredicateTest {
    private final static Trials<ImmutableList<Long>> lists =
            Trials.api().longs().immutableLists();

    private final static Trials<Long> longs = Trials.api().longs();

    @TrialsTest(trials = {"lists", "longs", "lists"}, casesLimit = 10)
    void setMembershipShouldBeRecognisedByThePredicate(
            ImmutableList<Long> leftHandList, long additionalLongToSearchFor,
            ImmutableList<Long> rightHandList) {
        final Predicate<Long> systemUnderTest =
                new PoorQualitySetMembershipPredicate(ImmutableList
                                                              .builder()
                                                              .addAll(leftHandList)
                                                              .add(additionalLongToSearchFor)
                                                              .addAll(rightHandList)
                                                              .build());

        assertThat(systemUnderTest.test(additionalLongToSearchFor),
                   is(true));
    }
}
```

Observe how we ganged together the three trials directly in the `@TrialsTest` annotation, so they can be unpicked directly into the test.

Running it reveals trial failures: ![](https://raw.githubusercontent.com/sageserpent-open/americium/4ced3f413c91ffee5e5587f48f137593cae03d93/screenshots/SetMembershipPredicateTest.png)

The second trial failed, then Americium went into shrinkage mode, which is made evident in the test case display. ~~It is important to highlight shrinkage mode, as trying to right click on a trial run as part of shrinkage will in general **not** manage to run its test case again; only trials run up to and including the first failure can be directly re-run from the IDE, this is a quirk of how JUnit5 works under the hood.~~ (*Since release 1.18.0 you can now directly replay a trial run that was done as part of shrinkage.*)

We also see the final test failure with the maximally shrunk case and a way of reproducing it. The use of the Java properties `trials.recipeHash` and `trials.recipe` works with the JUnit5 integration in the same way as in standalone mode.

## @ConfiguredTrialsTest

For more flexibility in configuration, there is another test annotation, `@ConfiguredTrialsTest` that works with instances of `SupplyToSyntax` instead of `Trials`. Its use is pretty much the same as for `@TrialsTest`, only you use `.withStrategy` etc to configure your `SupplyToSyntax` instance to be just so.

You can only refer to one instance of `SupplyToSyntax`, so if you want to use multiple trials, gang them together first with `.and` and then call `.withStrategy` or `withLimit`.

***
Next topic: [Techniques...]({% link docs/wiki-content/techniques.md %})