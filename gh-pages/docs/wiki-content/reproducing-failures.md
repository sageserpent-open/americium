---
layout: default
title: "Reproducing a failing test case quickly"
parent: Wiki Content
nav_order: 5
---

# Reproducing a failing test case quickly
{: .no_toc }

Recipes and recipe hashes
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Here's a tricky bit of implementation:

```java
class Tiers<Element extends Comparable<Element>> {
    final int worstTier;

    final List<Element> storage;

    public Tiers(int worstTier) {
        this.worstTier = worstTier;
        storage = new ArrayList<>(worstTier) {
            @Override
            public void add(int index, Element element) {
                if (size() < worstTier) {
                    super.add(index, element);
                } else {
                    for (int shiftDestination = 0;
                         shiftDestination < index; ++shiftDestination) {
                        super.set(shiftDestination,
                                  super.get(1 + shiftDestination));
                    }

                    super.set(index, element);
                }
            }
        };
    }

    void add(Element element) {
        final int index = Collections.binarySearch(storage, element);

        if (0 > index) {
            storage.add(-(index + 1), element);
        } else {
            storage.add(index, element);
        }
    }

    Optional<Element> at(int tier) {
        return 0 < tier && tier <= storage.size()
               ? Optional.of(storage.get(storage.size() - tier))
               :
               Optional.empty();
    }
}
```

The purpose of `Tiers` is to take a series of elements, and arrange the elements by tier, where tier 1 is the highest element, tier 2 is the next highest and so on down to a fixed worst tier. Elements that don't make the grade (or are surpassed later) are summarily ejected.

Testing this is quite involved and allows a demonstration of a realistic property-based test. There is a lot of code to follow, but the gist of it is to start with a list of query values that we expect the tiers instance to end up with, then present them in a feed sequence to the tiers instance, surrounding each query value with a run of background values that are constructed to be less than all of the query values.

This approach explores the full range of possibilities - we may have duplicates in the query values (so presumably they should occupy adjacent tiers). Query values may be interspersed with empty lists, in which case they can start or end the feed sequence, or come in adjacent clumps. The query values aren't presented in any particular order, nor are the background values. All we can say is that because the query values are the highest by construction, then as long as we set the number of tiers to be the number of query values, then they should all make it through to the final assessment.

So, on with the test:

```java
final Trials<ImmutableList<Integer>> queryValueLists = api()
        .integers(-1000, 1000)
        .immutableLists()
        .filter(list -> !list.isEmpty());


final Trials<Tuple2<ImmutableList<Integer>, ImmutableList<Integer>>>
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

try {
    testCases.withLimit(40).supplyTo(testCase -> {
        final ImmutableList<Integer> queryValues = testCase._1();
        final ImmutableList<Integer> feedSequence = testCase._2();

        final int worstTier = queryValues.size();

        final Tiers<Integer> tiers = new Tiers<>(worstTier);

        feedSequence.forEach(tiers::add);

        final ImmutableList.Builder<Integer> builder =
                ImmutableList.<Integer>builder();

        int tier = worstTier;

        int previousTierOccupant = Integer.MIN_VALUE;

        do {
            final Integer tierOccupant = tiers.at(tier).get();

            assertThat(tierOccupant, greaterThanOrEqualTo(previousTierOccupant));

            builder.add(tierOccupant);

            previousTierOccupant = tierOccupant;
        } while (1 < tier--);

        final ImmutableList<Integer> arrangedByRank = builder.build();

        assertThat(arrangedByRank, containsInAnyOrder(queryValues.toArray()));
    });
} catch (TrialsFactoring.TrialException e) {
    System.out.println(e);
}
```

By now, we won't be surprised to find that it doesn't work:

```
Trial exception with underlying cause:
java.lang.IndexOutOfBoundsException: Index 1 out of bounds for length 1
Case:
[[0],[-1, 0]]
Reproduce via Java property:
trials.recipeHash=b91fa06969da28bd58ca711b7b50ff75
Reproduce via Java property:
trials.recipe="[{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":0}},{\"ChoiceOf\":{\"index\":0}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":-1}},{\"ChoiceOf\":{\"index\":0}},{\"ChoiceOf\":{\"index\":0}}]"
```

That failing test case looks nice and shrunk - so how do we debug this? We really don't want to sit in the debugger ploughing through all the initial test failures (there are plenty of them), not to mention all the other trials that succeeded before we start analysing the final maximally shrunk test case's trial - we just want to run a single trial for that maximally shrunk case straightaway and get to debugging _that_.

The exception from Americium (it's a `TrialsFactoring.TrialException` that wraps up the exception thrown by the test) tells us how - we re-execute our test with a JVM property setting of:
```
-Dtrials.recipeHash=b91fa06969da28bd58ca711b7b50ff75
```

This will force Americium to go directly to the trial we're interested in, and we can get debugging. Of course, this only works if we run the _same test_ as when the recipe hash was generated. Other tests with different trials won't understand the recipe hash and will likely fault.

Once we're done fixing the problem, we remove the property setting and Americium goes back to its usual behaviour of exploring lots of trials. We can keep on repeating the same test case by leaving the property set.

It is possible to work with several different failing tests at the same time, each with their own recipe hash.

The way this works is that Americium records a _recipe_ and its recipe hash in a database located in a temporary folder somewhere in the filesystem. If we want to specify where, we set the JVM property `java.io.tmpdir`. The database name is given by `trials.runDatabase`. These properties are found in `JavaPropertyNames`, but if we don't specify them, defaults will be chosen.

Now, because the reproduction of the trials is driven by the recipe (the recipe hash is simply used as a key to retrieve it), then if say we observe a test failure in a CI box, the recipe hash is of no use to us - our local computer will have its own database that knows nothing about that. To debug the failure, we turn to the related property, `trials.recipe`, which is escaped JSON that can be passed to a test run as a Java property either from the command line or embedded in a shell script or equivalent.

```
-Dtrials.recipe="[{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":0}},{\"ChoiceOf\":{\"index\":0}},{\"ChoiceOf\":{\"index\":1}},{\"FactoryInputOf\":{\"input\":-1}},{\"ChoiceOf\":{\"index\":0}},{\"ChoiceOf\":{\"index\":0}}]"
```

This approach always works, but recipe hashes are nice and compact if you are working with locally running tests to start with.

Where did we go wrong? Ah yes - sometimes an element just drops immediately off the lowest tier as it is added; our element shifting logic is off-by-one, as well the actual pseudo-insertion when all tiers are filled. The fixed version looks like this:

```java
class Tiers<Element extends Comparable<Element>> {
    final int worstTier;

    final List<Element> storage;

    public Tiers(int worstTier) {
        this.worstTier = worstTier;
        storage = new ArrayList<>(worstTier) {
            @Override
            public void add(int index, Element element) {
                if (size() < worstTier) {
                    super.add(index, element);
                } else if (0 < index) {
                    for (int shiftDestination = 0;
                         shiftDestination < index - 1; ++shiftDestination) {
                        super.set(shiftDestination,
                                  super.get(1 + shiftDestination));
                    }

                    super.set(index - 1, element);
                }
            }
        };
    }

    void add(Element element) {
        final int index = Collections.binarySearch(storage, element);

        if (0 > index) {
            storage.add(-(index + 1), element);
        } else {
            storage.add(index, element);
        }
    }

    Optional<Element> at(int tier) {
        return 0 < tier && tier <= storage.size()
               ? Optional.of(storage.get(storage.size() - tier))
               :
               Optional.empty();
    }
}
```

Would the usual approach of writing tests with hand-written scenarios have found that bug? Maybe, but we don't generally have the benefit of hindsight when doing TDD, for instance. Much better to express a fundamental property of the system under test and then show that it holds in a wide variety of trials, because this is exactly what the real world is going to inflict on our code in production. Here, the property is that the highest values fed to a tiers instance will survive as long as they fit in the number of available tiers.

So far, so good - we have looked at a realistic property test, seen various techniques put together for building up complex trials and learned how to directly reproduce a maximally shrunk failing test case in a single trial for problem diagnosis and remedy.

However, there is something amiss - take a look at the code below, taken from the fixed implementation of `Tiers` but with a _fault injected_ into it to 'test the testing':

```java
    void add(Element element) {
        final int index = Collections.binarySearch(storage, element);

        if (0 >= index) /* <<----- FAULT */ {
            storage.add(-(index + 1), element);
        } else {
            storage.add(index, element);
        }
    }
```

This is a useful practice when we want to gain confidence in our testing - if we deliberately inject faults, do our tests find them?

When we try it out, the test _passes_. That's not good, because now there really is a fault. Let's come back to this in a later topic, we can do better by tweaking the trials setup a little. What might the issue be, and how hard would it be to make a test case that exposes the fault?

***

Next topic: [All about shrinkage...]({% link docs/wiki-content/shrinkage.md %})