---
layout: default
title: "Techniques"
parent: Wiki Content
nav_order: 10
---

# Techniques
{: .no_toc }

Impress your friends with sleights of hand
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## Forcing lots of duplicates

Remember the `Tiers` example? There was a cliffhanger where a fault was injected, but the test still passed. Naughty.

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

This is definitely a bug, so why isn't Americium exposing it?

We start by increasing the limit from 10 in the JUnit5 version of the test, and after a lot of experimentation we hit on:

```java
    @TrialsTest(trials = "testCases", casesLimit = 11000)
    void tiersShouldRetainTheLargestElements(ImmutableList<Integer> queryValues,
                                             ImmutableList<Integer> feedSequence){
...
}
```

This yields:

![](https://raw.githubusercontent.com/sageserpent-open/americium/5d86a9bd622efa94b5059c4d8c68fa8c15a02a12/screenshots/TiersInJUnit5WithAVeryHighLimit.png)

Don't be fooled by the times reported for the tests - IntelliJ has a nervous breakdown while it processes the 89490 trials over all of the cycles and takes a while to recover. Fortunately all of the individual trials were quite snappy, but imagine if there was some IO involved in the test - that would take a _long_ time to run.

We can see the problem now: this is something to do with adjacent duplicates being presented to the tiers instance; the thing is, we allow the query values to vary all over the place:

```java
    private final static Trials<ImmutableList<Integer>> queryValueLists = api()
            .integers(-1000, 1000)
            .immutableLists()
            .filter(list -> !list.isEmpty());
```

So yes, Americium will eventually generate duplicates, but we need to be patient. Very patient.

Now we could simply force the issue by restricting those query values down to a small range, say -1 to 1, but even that requires the limit to be increased, and we want to have the full generality in our test and go stomping around over lots of integers.

There is a trick to encourage duplication of values in collections:

```java
    private final static Trials<ImmutableList<Integer>> queryValueLists = api()
            .integers(1, 10)
            .flatMap(numberOfChoices -> api()
                    .integers(-1000, 1000)
                    .immutableListsOfSize(
                            numberOfChoices)
                    .flatMap(choices -> api()
                            .choose(choices)
                            .immutableListsOfSize(
                                    numberOfChoices)));
```

What we are doing here is to make choices of integer values in the outer flat-map sequence, then inject a _specific_ group of choices into the inner flat-map. That inner flat-map then builds a list whose elements are taken from just that group, so there is a high probability of duplication; we are building a list with `numberOfChoices` elements, each of these elements has a value belonging to the same pool of size `numberOfChoices` - so unless all the element choices conspire to avoid each other, we'll see the same value get picked twice or maybe more.

Pushing the limit up to a modest 30 yields:

![](https://raw.githubusercontent.com/sageserpent-open/americium/329e894664eb183af43e48d84a5be239d6994f5a/screenshots/TiersInJUnit5WithBetterDuplicateValueGeneration.png)

That's just 69 trials to result in the optimally shrunk case, much better.

## Unique ids when building `Trials`

Here'a a model of a group of participants, extended to allow subgroups to be made members of groups too. Typical corporate distribution list stuff.

```java
public class Group {
    private final String name;

    private final List<Either<Group, String>> members;

    public Group(String name, List<Either<Group, String>> members) {
        this.name = name;
        this.members = members;
    }

    public void checkUniquenessOfNames() {
        gatherNamesInto(new HashSet<>());
    }

    void gatherNamesInto(Set<String> gatheredNames) {
        Preconditions.checkState(gatheredNames.add(name));

        members.forEach(either -> either.bipeek(
                group -> group.gatherNamesInto(gatheredNames),
                participant -> {
                    Preconditions.checkState(gatheredNames.add(participant));
                }));
    }

    public int maximumDepth() {
        return 1 + members
                .stream()
                .map(either -> either.fold(Group::maximumDepth,
                                           unused -> 0))
                .reduce(0, Integer::max);
    }

    @Override
    public String toString() {
        return String.format("Group: %s, maximum depth: %d, members: (%s)",
                             name,
                             maximumDepth(),
                             members
                                     .stream()
                                     .map(either -> either.fold(Group::toString,
                                                                Function.identity()))
                                     .collect(Collectors.toList()));
    }
}
```

When we test over instances of `Group`, we want to build up some decent structure for our test cases, so some kind of recursive formulation comes to mind. All very well, except that we expect the groups and participants that are direct or indirect members of our top-level group to have unique names. How do we ensure this when names are presumably chosen arbitrarily?

Yes, we could do something like this:

```java
api().delay(() -> api().only(UUID.randomUUID())).map(UUID::toString)
```

That gives us unique names, but they are unreadable - our tiny human minds prefer simple readable things like small integers! The lack of repeatability between runs due to using `UUID` is also annoying.

Behold `TrialsApi.uniqueIds`:

```java
class Module {
    public static Trials<Group> groups() {
        final Trials<String> groupNames =
                api()
                        .uniqueIds()
                        .map(id -> String.format("Group-%d", id));

        final Trials<String> participantNames =
                api()
                        .uniqueIds()
                        .map(id -> String.format("Participant-%d", id));


        final Trials<ImmutableList<Either<Group, String>>> memberLists =
                api()
                        .delay(Module::groups)
                        .or(participantNames)
                        .immutableLists();

        return groupNames.flatMap(name -> memberLists.map(members -> new Group(
                name,
                members)));
    }
}

Module.groups().withLimit(10).supplyTo(group -> {
    System.out.println(group);
    group.checkUniquenessOfNames();
});
```

This test passes nicely:

```
Group: Group-0, maximum depth: 2, members: ([Group: Group-1, maximum depth: 1, members: ([])])
Group: Group-0, maximum depth: 1, members: ([])
Group: Group-0, maximum depth: 1, members: ([Participant-1])
Group: Group-0, maximum depth: 1, members: ([Participant-2, Participant-1])
Group: Group-0, maximum depth: 6, members: ([Participant-28, Participant-27, Group: Group-6, maximum depth: 5, members: ([Group: Group-14, maximum depth: 4, members: ([Group: Group-20, maximum depth: 3, members: ([Participant-26, Group: Group-24, maximum depth: 2, members: ([Group: Group-25, maximum depth: 1, members: ([])]), Group: Group-22, maximum depth: 1, members: ([Participant-23]), Group: Group-21, maximum depth: 1, members: ([])]), Group: Group-15, maximum depth: 2, members: ([Group: Group-18, maximum depth: 1, members: ([Participant-19]), Group: Group-17, maximum depth: 1, members: ([]), Group: Group-16, maximum depth: 1, members: ([])])]), Group: Group-10, maximum depth: 2, members: ([Participant-13, Group: Group-11, maximum depth: 1, members: ([Participant-12])]), Group: Group-9, maximum depth: 1, members: ([]), Participant-8, Participant-7]), Participant-5, Group: Group-4, maximum depth: 1, members: ([]), Participant-3, Group: Group-1, maximum depth: 1, members: ([Participant-2])])
Group: Group-0, maximum depth: 5, members: ([Participant-9, Participant-8, Group: Group-4, maximum depth: 4, members: ([Group: Group-5, maximum depth: 3, members: ([Group: Group-6, maximum depth: 2, members: ([Group: Group-7, maximum depth: 1, members: ([])])])]), Group: Group-2, maximum depth: 1, members: ([Participant-3]), Group: Group-1, maximum depth: 1, members: ([])])
Group: Group-0, maximum depth: 4, members: ([Group: Group-7, maximum depth: 1, members: ([]), Participant-6, Participant-5, Group: Group-1, maximum depth: 3, members: ([Group: Group-2, maximum depth: 2, members: ([Participant-4, Group: Group-3, maximum depth: 1, members: ([])])])])
Group: Group-0, maximum depth: 3, members: ([Group: Group-2, maximum depth: 2, members: ([Group: Group-3, maximum depth: 1, members: ([Participant-7, Participant-6, Participant-5, Participant-4])]), Participant-1])
Group: Group-0, maximum depth: 2, members: ([Participant-5, Group: Group-1, maximum depth: 1, members: ([Participant-4, Participant-3, Participant-2])])
Group: Group-0, maximum depth: 2, members: ([Group: Group-2, maximum depth: 1, members: ([]), Participant-1])
```

See how within each test case, the integer ids yielded by `TrialsApi.uniqueIds` are all different. They are nevertheless repeatable and if a test case fails, reproducable with the same values. The uniqueness only holds *within* a given test case - we won't see any 'creep' of ids from one test case to another, specific ids can and will reappear across unrelated test cases.

## Permutations

A common pattern in property-based testing to to take an expected output that is correct by construction, then demolish it and use the resulting pieces as input to the test.

For example, it is easy to generate a run of increasing values; we'll do this in Scala for a change:

```scala
api
  .choose(0 until 5)
  .several[Vector[Int]]
  .flatMap(increments =>
    api.choose(-10 to 10).map(base => increments.scanLeft[Int](base)(_ + _))
  )
  .withLimit(10)
  .supplyTo(println)
```

This yields:

```
Vector(7, 8)
Vector(3)
Vector(6, 10)
Vector(4)
Vector(-7, -5)
Vector(-5)
Vector(-3, 0, 2, 2)
Vector(1)
Vector(-4)
Vector(-1, 3, 6)
```

Note the presence of duplicates too, that's nice.

Now we have that technique in hand, we can test yet another sorting algorithm by permuting these test cases, feeding them into the sorting algorithm and verifying that what comes out is the same as what we started with.

We'll demonstrate using `TrialsApi.indexPermutations` to do this, let's see what it does first:

```scala
api.indexPermutations(0).withLimit(15).supplyTo(println)
// Vector()

api.indexPermutations(1).withLimit(15).supplyTo(println)
// Vector(0)

api.indexPermutations(2).withLimit(15).supplyTo(println)
// Vector(1, 0)
// Vector(0, 1)

api.indexPermutations(3).withLimit(15).supplyTo(println)
// Vector(1, 0, 2)
// Vector(2, 1, 0)
// Vector(1, 2, 0)
// Vector(0, 2, 1)
// Vector(0, 1, 2)
// Vector(2, 0, 1)

api.indexPermutations(4).withLimit(50).supplyTo(println)
// Vector(2, 0, 3, 1)
// Vector(3, 1, 2, 0)
// Vector(2, 3, 0, 1)
// Vector(2, 3, 1, 0)
// Vector(2, 0, 1, 3)
// Vector(1, 2, 0, 3)
// Vector(1, 0, 2, 3)
// etc...
```

This yields permutations of a range of integers from zero up to but not including the size of the range. You are encouraged to think of these integers as being indices into some implied collection of items of the same size, hence the name of the method.

Here's an example where we take a nice, simple collection of `0 until size`, permute it, then feed it to a test that will fail if at least two adjacent elements are not in non-strict ascending order:

```scala
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

try {
  permutations
    .withLimit(15)
    .supplyTo { permuted =>
      Trials.whenever(permuted.nonEmpty) {
        permuted.values zip permuted.values.tail foreach { case (left, right) =>
          if (left > right) {
            println(permuted.values)

            throw new RuntimeException
          }
        }
      }
    }
} catch {
  case exception: permutations.TrialException =>
    println(exception)
}
```

Here, we transform the permutations generated by `api.indexPermutations(size)`, zipping `indices` with `sourceCollection` and turning that into a map sorted on the indices; this rearranges the original elements in `sourceCollection` according to the _inverse_ of the index permutation.

We could have also used `indices` as a direct permutation of integers in its own right; this works if the collection being permuted is a zero-relative range of contiguous integers, but here we have stuck with an explicit rearrangement of the original collection.

We could also have permuted on-the-fly by composing lookups to yield a lambda that accesses permuted items:

```scala
val inPermutationAt: Int => Int = indices.andThen(sourceCollection)
```

That last approach works well if you don't want to use the collection as a collection with a size, ability to add / remove elements etc - you just need random access to what's in it.

Anyway, shrinkage proceeds like this:

```
Iterable(8, 1, 13, 12, 11, 9, 6, 4, 10, 14, 0, 3, 5, 7, 2)
Iterable(8, 5, 7, 0, 1, 4, 3, 13, 6, 2, 11, 10, 14, 9, 12)
Iterable(9, 4, 1, 0, 5, 3, 2, 7, 8, 6, 12, 13, 11, 10, 14)
Iterable(4, 1, 5, 0, 2, 3, 6, 8, 11, 9, 13, 10, 7, 14, 12)
Iterable(1, 0, 2, 4, 3, 5, 6, 9, 8, 7, 11, 14, 10, 12, 13)
Iterable(1, 0, 2, 4, 3, 5, 6, 8, 7, 11, 10, 9, 12, 13, 14)
Iterable(1, 0, 2, 3, 4, 5, 6, 8, 7, 9, 10, 11, 12, 13, 14)
Iterable(1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 13)
Iterable(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 12, 13)
Iterable(0, 1, 2, 3, 4, 5, 6, 7, 9, 8, 10, 11, 12, 13, 14)
Trial exception with underlying cause:
java.lang.RuntimeException
Case:
TreeMap(0 -> 0, 1 -> 1, 2 -> 2, 3 -> 3, 4 -> 4, 5 -> 5, 6 -> 6, 7 -> 7, 8 -> 9, 9 -> 8, 10 -> 10, 11 -> 11, 12 -> 12, 13 -> 13, 14 -> 14)
Reproduce via Java property:
trials.recipeHash=75462f61356dc6336a88b40dd7a4f2fa
```

So the higgledy-piggledy permutation that gave us the first failing trial has been straightened out to a more tractable failing test case with just one pair of mapped values the wrong way around - 9 and 8. Much better!

## Alternate Picking

Put your guitar back down, we're talking about picking items between sequences to make a merged sequence. Suppose we have sequences of positive odd, positive even and negative numbers:

```java
final List<Integer> odds = Stream
        .iterate(1, x -> 2 + x)
        .limit(10)
        .collect(Collectors.toList());

final List<Integer> evens = Stream
        .iterate(0, x -> 2 + x)
        .limit(15)
        .collect(Collectors.toList());

final List<Integer> negatives = Stream
        .iterate(-1, x -> x - 1)
        .limit(7)
        .collect(Collectors.toList());
```

Let's see what happens when we pick alternately, shrinking towards round-robin picking on failure:

```java

final Trials<List<Integer>> thingsThatShrinkToRoundRobinPicking =
        api().pickAlternatelyFrom(true,
                                  odds, evens, negatives);

thingsThatShrinkToRoundRobinPicking
        .withLimit(10)
        .supplyTo(System.out::println);
// [0, 1, 3, 5, 2, 4, 7, 9, -1, 11, 13, -2, 15, 6, -3, 17, -4, 19, -5, 8, -6, 10, 12, 14, -7, 16, 18, 20, 22, 24, 26, 28]
// [0, 2, 1, 3, -1, 5, 7, -2, 4, -3, 6, 8, -4, 9, -5, -6, 10, 12, -7, 11, 13, 14, 16, 18, 15, 17, 19, 20, 22, 24, 26, 28]
// [1, 0, 3, 2, 5, 7, 4, -1, 6, -2, 9, 11, 13, 15, 17, 19, -3, 8, 10, -4, -5, -6, -7, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [0, -1, -2, 2, 4, -3, 1, 6, -4, 8, 3, 10, 12, 14, 16, 5, 18, 7, 20, 9, 11, 22, 24, -5, 13, -6, 26, -7, 28, 15, 17, 19]
// [-1, 0, -2, 1, -3, -4, 2, 3, 4, 5, -5, 7, -6, 6, -7, 8, 10, 9, 12, 14, 11, 16, 18, 20, 22, 13, 24, 15, 17, 26, 19, 28]
// [-1, 0, -2, 2, -3, 4, -4, -5, 6, -6, -7, 1, 3, 5, 8, 7, 10, 12, 14, 16, 9, 18, 11, 13, 15, 20, 17, 19, 22, 24, 26, 28]
// [1, 3, -1, 5, 7, -2, -3, 9, 0, 11, 13, -4, 15, -5, -6, 17, 2, 19, 4, -7, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 0, 2, 5, 4, 6, 7, 8, 9, 11, 10, 13, 12, 15, 14, -1, 16, -2, 17, -3, 19, 18, -4, 20, -5, 22, 24, -6, 26, -7, 28]
// [0, 2, -1, -2, -3, 4, 1, -4, 6, 8, -5, -6, 10, -7, 12, 3, 5, 14, 7, 9, 16, 18, 11, 20, 22, 13, 24, 15, 26, 17, 19, 28]
// [0, 1, -1, 3, -2, 5, -3, 7, -4, 9, -5, 11, 2, -6, 4, 6, -7, 8, 13, 15, 17, 10, 19, 12, 14, 16, 18, 20, 22, 24, 26, 28]

thingsThatShrinkToRoundRobinPicking.withLimit(10).supplyTo(sequence -> {
    System.out.println(sequence);
    throw new RuntimeException();
});
// [0, 1, 3, 5, 2, 4, 7, 9, -1, 11, 13, -2, 15, 6, -3, 17, -4, 19, -5, 8, -6, 10, 12, 14, -7, 16, 18, 20, 22, 24, 26, 28]
// [-1, 0, -2, 1, -3, -4, 2, 3, 4, 5, -5, 7, -6, 6, -7, 8, 10, 9, 12, 14, 11, 16, 18, 20, 22, 13, 24, 15, 17, 26, 19, 28]
// [-1, 0, 1, -2, 3, -3, 2, 5, 4, 7, -4, 6, -5, 9, -6, -7, 11, 8, 13, 15, 10, 17, 12, 19, 14, 16, 18, 20, 22, 24, 26, 28]
// [-1, 0, 1, -2, 2, -3, 3, 4, 5, 6, -4, 7, 8, -5, 9, -6, 10, 11, 13, -7, 12, 15, 14, 17, 16, 19, 18, 20, 22, 24, 26, 28]
// [0, -1, 2, 1, -2, 4, 3, -3, 6, -4, 5, 8, 7, -5, 10, 9, -6, 12, -7, 11, 14, 16, 13, 18, 15, 20, 17, 22, 19, 24, 26, 28]
// [1, 0, -1, 3, 2, -2, 5, 4, -3, 6, 7, -4, 9, 8, -5, 11, 10, -6, 12, 13, -7, 15, 14, 17, 16, 19, 18, 20, 22, 24, 26, 28]
// [1, 0, -1, 3, 2, -2, 5, 4, -3, 6, 7, -4, 9, 8, -5, 11, 10, -6, 13, 12, 15, 14, -7, 17, 16, 19, 18, 20, 22, 24, 26, 28]
// [1, 0, -1, 3, 2, -2, 5, 4, -3, 6, 7, -4, 8, 9, -5, 10, 11, -6, 12, 13, 14, 15, -7, 16, 17, 18, 19, 20, 22, 24, 26, 28]
// [1, 0, -1, 3, 2, -2, 5, 4, -3, 6, 7, -4, 8, 9, -5, 10, 11, -6, 12, 13, -7, 14, 15, 16, 17, 18, 19, 20, 22, 24, 26, 28]
// [1, 0, -1, 3, 2, -2, 5, 4, -3, 7, 6, -4, 9, 8, -5, 11, 10, -6, 13, 12, -7, 15, 14, 17, 16, 19, 18, 20, 22, 24, 26, 28]
```

The trials resulting from `TrialsApi.pickAlternatelyFrom` yield cases that are merges of the three sequences, respecting the order of the elements in each sequence, but alternating between them in a manner that varies from case to case.

Failures cause shrinkage towards a round-robin merge of the three sequences, so we see their elements interleaved in the maximally shrunk case. All of the sequences will be drained regardless of length, so we see a trailing section just from the even number sequence.

Let's change the first argument to `TrialsApi.pickAlternatelyFrom` to be false:

```java
final Trials<List<Integer>> thingsThatShrinkToConcatenation =
        api().pickAlternatelyFrom(false,
                                  odds, evens, negatives);

thingsThatShrinkToConcatenation.withLimit(10).supplyTo(System.out::println);
// [0, 2, -1, 1, -2, 4, -3, 3, 5, -4, 6, -5, 8, 10, 12, 14, -6, 16, -7, 18, 20, 22, 7, 24, 26, 9, 11, 28, 13, 15, 17, 19]
// [-1, -2, 1, 0, 3, 5, 2, 7, -3, 9, 11, -4, 4, 6, 13, 8, 10, -5, -6, 15, 12, 17, -7, 14, 19, 16, 18, 20, 22, 24, 26, 28]
// [0, 1, -1, 3, 5, -2, 7, 9, 2, -3, 11, 4, -4, 6, 8, 10, -5, -6, 12, -7, 13, 14, 15, 16, 17, 18, 19, 20, 22, 24, 26, 28]
// [1, 3, 0, 2, 5, -1, 4, 7, 6, 9, 8, 11, 10, -2, 12, 13, 15, 17, 14, 16, 19, -3, 18, -4, -5, 20, 22, 24, -6, -7, 26, 28]
// [0, 1, 3, 2, 5, 7, 4, -1, 6, -2, -3, -4, 9, 11, 13, 8, 15, 10, 17, 12, 19, -5, -6, 14, -7, 16, 18, 20, 22, 24, 26, 28]
// [0, 1, 2, 3, 4, -1, 6, -2, 5, 7, -3, 9, 8, 11, 13, 15, -4, 10, 17, 19, 12, 14, -5, -6, -7, 16, 18, 20, 22, 24, 26, 28]
// [0, 1, -1, 3, 2, 5, 7, 4, -2, -3, 6, -4, 9, -5, -6, 11, -7, 8, 10, 12, 13, 15, 14, 16, 18, 17, 20, 19, 22, 24, 26, 28]
// [0, 1, 2, 3, 4, 6, 5, 8, 10, 7, 12, -1, 14, -2, 16, -3, -4, 9, -5, 11, 13, -6, -7, 15, 18, 17, 20, 19, 22, 24, 26, 28]
// [1, 3, -1, 5, 0, 7, 2, -2, 9, 11, -3, -4, 13, -5, 15, -6, 17, -7, 19, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 5, 0, -1, 2, 4, 6, -2, 8, -3, 10, -4, 12, -5, 14, -6, -7, 16, 7, 9, 18, 20, 11, 22, 24, 26, 28, 13, 15, 17, 19]

thingsThatShrinkToConcatenation.withLimit(10).supplyTo(sequence -> {
    System.out.println(sequence);
    throw new RuntimeException();
// [0, 2, -1, 1, -2, 4, -3, 3, 5, -4, 6, -5, 8, 10, 12, 14, -6, 16, -7, 18, 20, 22, 7, 24, 26, 9, 11, 28, 13, 15, 17, 19]
// [0, 1, 3, 2, 5, 7, 4, -1, 6, -2, -3, -4, 9, 11, 13, 8, 15, 10, 17, 12, 19, -5, -6, 14, -7, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, -1, 5, 0, 7, 2, -2, 9, 11, -3, -4, 13, -5, 15, -6, 17, -7, 19, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 5, 0, -1, 2, 4, 6, -2, 8, -3, 10, -4, 12, -5, 14, -6, -7, 16, 7, 9, 18, 20, 11, 22, 24, 26, 28, 13, 15, 17, 19]
// [0, -1, -2, 2, -3, -4, 4, -5, -6, -7, 6, 1, 8, 10, 12, 14, 16, 18, 3, 5, 7, 9, 20, 22, 11, 13, 15, 24, 17, 26, 19, 28]
// [0, 2, 1, -1, 3, 5, -2, -3, -4, 7, -5, 9, 11, 13, 15, -6, -7, 17, 19, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 5, 0, 7, 9, 11, 13, 15, 2, 4, 17, 19, 6, -1, -2, 8, -3, -4, -5, -6, -7, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 5, 0, 7, 9, 11, 13, 15, 2, 4, 17, 19, 6, 8, 10, -1, 12, 14, 16, 18, 20, 22, 24, 26, 28, -2, -3, -4, -5, -6, -7]
// [1, 3, 5, 0, 7, 9, 11, 13, 15, 17, 19, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, -1, -2, -3, -4, -5, -6, -7]
// [1, 3, 5, 0, 2, 4, 6, 8, 10, 12, 14, 7, 9, 11, 13, 15, 17, 19, 16, 18, 20, 22, 24, 26, 28, -1, -2, -3, -4, -5, -6, -7]
// [1, 3, 5, 7, 9, 11, 13, 15, 17, 19, -1, -2, -3, -4, -5, -6, -7, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28]
// [1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, -1, -2, -3, -4, -5, -6, -7]
```

This doesn't significantly affect how cases are generated in the absence of failures - again, they are just merges of the three sequences that respect each sequence's element order.

However, failures cause shrinkage towards sequential drainage of the sequences, which at maximal shrinkage is their concatenation.

## Complexity Budgeting

Let's build test cases that are trees. This is useful for job interviewing, where the idea is to pressure a candidate into sweating over a puzzle that bears no relation to what we really want them to do, but does provide a convenient excuse for not hiring if the panel doesn't instinctively like them.

They present a tree structure in Scala:

```scala
import scala.collection.immutable.SortedMap
import cats.instances.map._
import cats.kernel.Monoid
import cats.syntax.monoid

sealed trait Tree {
  def maximumDepth: Int

  def depthFrequencies: SortedMap[Int, Int]
}

case class Leaf(value: Int) extends Tree {
  override def maximumDepth: Int = 1

  override def depthFrequencies: SortedMap[Int, Int] = SortedMap(1 -> 1)
}

case class Branching(subtrees: List[Tree]) extends Tree {
  require(subtrees.nonEmpty)

  override def maximumDepth: Int =
    1 + subtrees.map(_.maximumDepth).max

  override def depthFrequencies: SortedMap[Int, Int] = subtrees
    .map(_.depthFrequencies)
    // INTERVIEW BONUS QUESTION: what does this line mean?
    .reduce[SortedMap[Int, Int]](Monoid.combine)
    .map { case (value, frequency) => (1 + value) -> frequency }
}
```

We feel puzzled and anxious. The question is, how would we write a smoke test for such a thing?

Let's try this:

```scala
def trees: Trials[Tree] = api.alternate(
  api.uniqueIds.map(Leaf.apply),
  api
    .integers(1, 5)
    .flatMap(numberOfSubtrees =>
      trees.listsOfSize(numberOfSubtrees).map(Branching.apply)
    )
)

trees.withLimit(10).supplyTo { tree =>
  println(
    s"Tree: $tree,\nmaximum depth: ${tree.maximumDepth} with depth frequencies: ${tree.depthFrequencies}\n"
  )
}
```

That didn't work very well...

```
Tree: Leaf(0),
maximum depth: 1 with depth frequencies: TreeMap(1 -> 1)

Tree: Branching(List(Branching(List(Leaf(3), Branching(List(Leaf(2), Leaf(1))))), Leaf(0))),
maximum depth: 4 with depth frequencies: TreeMap(2 -> 1, 3 -> 1, 4 -> 2)

Tree: Branching(List(Leaf(2), Leaf(1), Leaf(0))),
maximum depth: 2 with depth frequencies: TreeMap(2 -> 3)

Tree: Branching(List(Leaf(1), Leaf(0))),
maximum depth: 2 with depth frequencies: TreeMap(2 -> 2)
```

It seems the generation ran out of steam early, so we just got a few stunted trees. Part of the problem is that it is too easy to recurse to very deep levels of complexity, and this breaches the default complexity limit, so very few cases actually get generated. Those that do have very low complexity, hence their lack of size.

An interviewer sighs and writes something down, pausing to consult their mobile device inscrutably. Could we do better?

We redouble our efforts, and this time we get this...

```scala
def statelyTrees: Trials[Tree] = api.complexities.flatMap(complexity =>
  api.alternateWithWeights(
    complexity -> api.uniqueIds.map(Leaf.apply),
    2 -> api
      .integers(1, 5)
      .flatMap(numberOfSubtrees =>
        statelyTrees.listsOfSize(numberOfSubtrees).map(Branching.apply)
      )
  )
)

statelyTrees.withLimit(10).supplyTo { tree =>
  println(
    s"stately tree: $tree,\nmaximum depth: ${tree.maximumDepth} with depth frequencies: ${tree.depthFrequencies}\n"
  )
}
```

This yields:

```
stately tree: Branching(List(Leaf(19), Branching(List(Leaf(18), Branching(List(Leaf(17), Leaf(16), Leaf(15))), Branching(List(Branching(List(Leaf(14), Leaf(13))), Branching(List(Leaf(12))), Branching(List(Leaf(11), Leaf(10), Leaf(9), Branching(List(Leaf(8), Leaf(7), Leaf(6), Leaf(5))), Leaf(4))), Leaf(3), Leaf(2))), Leaf(1))), Branching(List(Leaf(0))))),
maximum depth: 6 with depth frequencies: TreeMap(2 -> 1, 3 -> 3, 4 -> 5, 5 -> 7, 6 -> 4)

stately tree: Branching(List(Leaf(1), Leaf(0))),
maximum depth: 2 with depth frequencies: TreeMap(2 -> 2)

stately tree: Branching(List(Branching(List(Leaf(2), Leaf(1))), Leaf(0))),
maximum depth: 3 with depth frequencies: TreeMap(2 -> 1, 3 -> 2)

stately tree: Branching(List(Branching(List(Leaf(14), Branching(List(Branching(List(Leaf(13), Leaf(12))), Leaf(11))))), Branching(List(Leaf(10), Leaf(9))), Branching(List(Leaf(8), Branching(List(Branching(List(Leaf(7), Leaf(6), Leaf(5))), Leaf(4), Branching(List(Leaf(3))), Leaf(2))), Leaf(1))), Leaf(0))),
maximum depth: 5 with depth frequencies: TreeMap(2 -> 1, 3 -> 5, 4 -> 3, 5 -> 6)

stately tree: Branching(List(Leaf(1), Leaf(0))),
maximum depth: 2 with depth frequencies: TreeMap(2 -> 2)

stately tree: Branching(List(Leaf(31), Branching(List(Leaf(30), Leaf(29), Leaf(28), Leaf(27))), Branching(List(Leaf(26), Branching(List(Branching(List(Leaf(25), Leaf(24), Leaf(23), Leaf(22))), Leaf(21), Branching(List(Leaf(20), Leaf(19), Branching(List(Leaf(18), Branching(List(Leaf(17))), Leaf(16))))), Leaf(15), Branching(List(Leaf(14), Branching(List(Leaf(13), Leaf(12), Leaf(11), Leaf(10))))))), Leaf(9))), Branching(List(Leaf(8), Branching(List(Branching(List(Leaf(7))), Branching(List(Leaf(6), Leaf(5), Leaf(4), Leaf(3), Leaf(2))), Branching(List(Leaf(1))), Leaf(0))))))),
maximum depth: 7 with depth frequencies: TreeMap(2 -> 1, 3 -> 7, 4 -> 3, 5 -> 14, 6 -> 6, 7 -> 1)

etc...
```

That's more like it, looks like an offer might be on its way. No, the interviewer didn't understand the technique, and has got in a huff. Bad luck, try somewhere else...

What's going on here is driven by the alternation between a leaf node and further branching in `statelyTrees`. The terminating leaf node alternative is weighted by the complexity of its calling context, whereas the branching gets a fixed weight.

This means that to start with, branching is favoured, which deepens the potential tree being built.

Now as the recursion deepens through calls to `statelyTrees`, the complexity retrieved by `api.complexities` goes up, so as the potential tree gets deeper, the alternation swings round to favour leaves over branching. This stops runaway recursion and keeps the test cases below the default complexity limit.

***
Next topic: [The competition...]({% link docs/wiki-content/competition.md %})