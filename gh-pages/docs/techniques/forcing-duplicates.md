---
layout: default
title: Forcing Duplicates
parent: Advanced Techniques
nav_order: 1
---

# Forcing Duplicates
{: .no_toc }

Generating test cases guaranteed to contain duplicate values
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Problem

Many data structures need to handle **duplicate values** correctly - sets, maps, deduplication algorithms, etc. But when you generate random test data, duplicates are **rare**:
```java
// Generate lists of integers
api().integers(1, 1000000).immutableLists()

// Probability of duplicates is LOW with such a large range!
// Might need millions of trials to find bugs that only appear with duplicates
```

You could reduce the range to increase duplicate probability:
```java
api().integers(1, 10).immutableLists()

// Better, but still relies on chance
// Duplicates happen, but not consistently
```

What if you need to **guarantee** that test cases contain duplicates?

---

## The Solution

Generate test cases where values are **explicitly drawn from a limited pool**:
```java
api().integers(1, 10).flatMap(n ->           // Pick a list size (1-10 elements)
    api().integers(-1000, 1000)              // Generate a pool of n values
        .immutableListsOfSize(n)
        .flatMap(choices ->                  // Now pick FROM those values
            api().choose(choices)            // Choose from the pool
                .immutableListsOfSize(n)))   // Make a list of size n
```

### How It Works

1. **Pick a size** `n` (e.g., 5)
2. **Generate a pool** of `n` distinct values (e.g., `[42, -17, 99, 3, -8]`)
3. **Build a list of size `n`** by choosing **from that pool**

Since you're choosing `n` items from a pool of only `n` values, **duplicates are guaranteed** (by the pigeonhole principle)!

---

## Understanding the Pattern

Let's break down what's happening step by step:

### Step 1: Choose List Size
```java
api().integers(1, 10)  // n ∈ [1, 10]
```

Example: `n = 5`

---

### Step 2: Generate Pool of Values
```java
.flatMap(n ->
    api().integers(-1000, 1000).immutableListsOfSize(n)
    // ...
)
```

Example pool: `[42, -17, 99, 3, -8]` (5 values, potentially all distinct)

---

### Step 3: Choose FROM the Pool
```java
.flatMap(choices ->
    api().choose(choices).immutableListsOfSize(n))
```

Now build a list of size 5 by choosing from `[42, -17, 99, 3, -8]`:

**Possible result:** `[42, 99, 42, 3, 99]`

Notice: **42 and 99 appear twice** - duplicates guaranteed!

---

## Why Duplicates Are Guaranteed

**Pigeonhole Principle:** If you choose `n` items from a pool of `n` values, at least one value **must** be chosen multiple times (unless all choices are distinct, which becomes increasingly unlikely as `n` grows).

With the pattern above:
- Small `n` (e.g., 2): Duplicates possible but not guaranteed
- Medium `n` (e.g., 5): Duplicates very likely
- Large `n` (e.g., 10): Duplicates virtually certain

For **absolute guarantee**, use a pool **smaller** than the list size:
```java
api().integers(1, 10).flatMap(n ->
    api().integers(-1000, 1000)
        .immutableListsOfSize(n / 2)         // Pool size: n/2
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)))   // List size: n
```

Now you're choosing `n` items from a pool of `n/2` values - **duplicates 100% guaranteed**!

---

## Variations

### Force Many Duplicates

Want lots of duplicates? Use an even smaller pool:
```java
api().integers(5, 20).flatMap(n ->
    api().integers(-1000, 1000)
        .immutableListsOfSize(3)             // Always 3 unique values
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)))   // List of 5-20 items
```

With 20 items from a pool of 3, you'll have **lots** of repetition!

---

### Controlled Duplicate Frequency

Want to control the expected duplicate rate?
```java
api().integers(10, 50).flatMap(n ->
    api().integers(1, 100)
        .immutableListsOfSize(n * 2 / 3)     // Pool = 2/3 of list size
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)))
```

Larger pool = fewer duplicates  
Smaller pool = more duplicates

---

## Fixing the Tiers Bug

Remember our **cliffhanger** from the Reproduction section? The injected bug:
```java
    void add(Element element) {
    final int index = Collections.binarySearch(storage, element);

    if (0 >= index) /* <<----- INJECTED FAULT */ {
        storage.add(-(index + 1), element);
    } else {
        storage.add(index, element);
    }
}
```

With 30 trials, we didn't catch it. Let's try to fix that with by turning the dials right up:
```java
testCases.withLimit(11000) /* <<----- BIG BUDGET! */.supplyTo(testCase -> {
final ImmutableList<Integer> queryValues = testCase._1();
final ImmutableList<Integer> feedSequence = testCase._2();

// The rest of the test body ...
```
This chugs through **89489** trials through the discovery and shrinkage cycles, and eventually we get the final shrunk test case:

```
Exception in thread "main" Trial exception with underlying cause:
java.lang.IndexOutOfBoundsException: Index: -1, Size: 1
Provoked by test case:
[[0, 0],[0, 0]]
```

We can see the problem now: the feed sequence has just one duplicated value that we expect not to be ejected (because the minimised test case omits any other values); that triggers the off-by-one array bounds error.

The thing is, we allow the query values to vary all over the place:

```java
final Trials<ImmutableList<Integer>> queryValueLists = api()
        .integers(-1000, 1000)
        .immutableLists()
        .filter(list -> !list.isEmpty());
```

So yes, Americium will eventually generate duplicates, and hopefully this will include a duplicated first-tier value, but we need to be patient. Very patient.

Now we can apply our trick to force duplicates...

```java
// Efficient with forced duplicates...  
final Trials<ImmutableList<Integer>> queryValueLists = api()
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

Don't forget to go back to just 30 trials:

```java
testCases.withLimit(30) /* <<----- LOW BUDGET REINSTATED! */.supplyTo(testCase -> {
final ImmutableList<Integer> queryValues = testCase._1();
final ImmutableList<Integer> feedSequence = testCase._2();

// The rest of the test body ...
```

We discover the bug, only this time, it takes **68** trials to get that shrunk test case.

That's a **99.9% reduction** in trials needed!

---

## When to Use This Technique

### ✅ Use when:
- Testing **set** implementations (duplicates should be handled)
- Testing **map** implementations (duplicate keys need special handling)
- Testing **deduplication** algorithms
- Testing **ranking** or **ordering** with equal values
- Bug hunting when you suspect duplicates trigger failures

### ❌ Don't use when:
- Testing algorithms that explicitly require **all unique** values
- The overhead of nested flat-maps slows generation too much
- You want to test the "no duplicates" case

---

## Combining with Other Techniques

### Force Duplicates + Permutations

Test that a sorted structure handles duplicate values correctly:
```java
api().integers(1, 5).flatMap(n ->
    api().integers(1, 10)
        .immutableListsOfSize(n)
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)
                .flatMap(values ->
                    api().indexPermutations(n).map(perm -> {
                        // Permute the duplicate-containing list
                        return perm.map(values::get);
                    }))))
```

Now you test with **duplicates in different orders**!

---

### Force Duplicates + Filtering

If you need duplicates but not **all** duplicates:
```java
api().integers(1, 10).flatMap(n ->
    api().integers(-1000, 1000)
        .immutableListsOfSize(n)
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)
                .filter(list -> {
                    // Only keep lists with at least 2 but not all duplicates
                    long uniqueCount = list.stream().distinct().count();
                    return uniqueCount > 1 && uniqueCount < list.size();
                })))
```

{: .warning }
> Be careful with filtering - it can lead to inefficiency! Use sparingly.

---

## Performance Considerations

The nested flat-map pattern **does** add overhead:
```java
api().integers(1, 10)              // Outer trials
    .flatMap(n ->                  // Flat-map overhead
        api().integers(-1000, 1000).immutableListsOfSize(n)  // Middle trials
            .flatMap(choices ->    // Another flat-map overhead
                api().choose(choices).immutableListsOfSize(n)))  // Inner trials
```

However, the **trade-off is worth it** when you need duplicates:
- **Without:** 11,000 trials to find bug
- **With:** 30 trials to find bug

Even with overhead, **30 trials is much faster than 11,000**!

---

## Shrinkage Behavior

How do duplicate-forced test cases shrink?

Example failing case:
```
[42, -17, 99, 42, 99, 3, -8, 42]
```

Americium will try to shrink:
1. **List size** - Remove elements: `[42, 99, 42, 99, 42]`
2. **Values toward zero** - `[2, 5, 2, 5, 2]`
3. **Both simultaneously** - `[0, 1, 0]`

The **duplicates are preserved** during shrinkage because they're inherent to the generation pattern!

---

{: .note-title }
> Key Takeaways
>
> - **Generate pool, then choose from pool** - Guarantees duplicates
> - **Pattern:** `integers(n).flatMap(n -> pool(n).flatMap(choose(pool).listOfSize(n)))`
> - **Pool size < list size** - More duplicates
> - **Pool size = list size** - Some duplicates (likely)
> - **Pool size > list size** - Fewer duplicates
> - **Massive efficiency gain** - 11,000 trials → 30 trials (99.7% reduction!)
> - **Shrinkage preserves pattern** - Duplicates maintained during shrinking
> - Use for sets, maps, deduplication, ranking with equal values
> - Trade-off: Slight generation overhead for huge efficiency gain