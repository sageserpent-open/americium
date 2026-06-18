---
layout: default
title: Unique IDs
parent: Advanced Techniques
nav_order: 2
reviewed: true
---

# Unique IDs
{: .no_toc }

Generating distinct identifiers within a test case using `uniqueIds`
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

Many systems require entities to have **unique identifiers**—think of account IDs, usernames, or primary keys in a database. When writing property tests for such systems, you need to generate these IDs.

If you use `UUID.randomUUID()`, you get uniqueness, but you lose two key benefits of property-based testing:
1. **Repeatability**: Random UUIDs change every time you run the test.
2. **Shrinkability**: A UUID like `5120f880-6d9b-4e58-bffa-2599e00e14fe` is "noise" to a human. If a test fails, you want to see IDs like `0`, `1`, and `2`, which are much easier to track.

Using a simple counter or a static set to track IDs inside a test is also problematic because Americium needs to be able to "replay" the generation process exactly during shrinkage and reproduction.

---

## The Solution: `api().uniqueIds()`

Americium provides **`api().uniqueIds()`**, a specialized trials instance that yields integers that are **guaranteed to be unique within a single test case**.

```java
final Trials<String> accountNames =
        api().uniqueIds().map(id -> String.format("Account-%d", id));
```

### How It Works

- **Scope**: Uniqueness is guaranteed **per trial**. Within one test case, every call to the trials instance derived from `uniqueIds()` will yield a different integer.
- **Freshness**: Across different trials, IDs will start over. You'll see `Account-0` in almost every trial.
- **Repeatability**: For a given recipe (seed), the sequence of IDs generated will be identical.
- **Shrinkability**: IDs are assigned starting from 0. This makes failing test cases much easier to read and allows Americium to shrink the number of unique entities used in a test.

---

## Example: Testing a Directory Structure

Imagine you are testing a group membership system where every group and participant must have a unique name.

```java
public class Group {
    private final String name;
    private final List<Either<Group, String>> members;

    public void checkUniquenessOfNames() {
        // ... recursive check that all names in the hierarchy are unique ...
    }
}
```

You can define a recursive generator using `uniqueIds`:

```java
public static Trials<Group> groups() {
    final Trials<String> groupNames =
            api().uniqueIds().map(id -> String.format("Group-%d", id));

    final Trials<String> participantNames =
            api().uniqueIds().map(id -> String.format("Participant-%d", id));

    final Trials<ImmutableList<Either<Group, String>>> memberLists =
            api().delay(Module::groups)
                 .or(participantNames)
                 .immutableLists();

    return groupNames.flatMap(name ->
        memberLists.map(members -> new Group(name, members)));
}
```

When you run this:
- Every `Group-n` and `Participant-m` name will be distinct.
- If a complex tree of 50 members fails, Americium will try to shrink it down to the smallest tree that still fails, likely using the smallest possible IDs (`Group-0`, `Participant-1`, etc.).

---

## Unique IDs vs. Global Counters

It's tempting to just use an `AtomicInteger` in your test:
```java
// ❌ AVOID THIS
static AtomicInteger counter = new AtomicInteger();
api().only(counter.getAndIncrement());
```

**Why this fails**: Americium works by re-running your generation logic many times. If you use a global counter, the "same" test case will get different IDs every time it is generated during the shrinkage process. This breaks shrinkage and reproduction entirely.

**`api().uniqueIds()`** is built into Americium's decision-tracking engine, so it remains perfectly consistent during re-runs.

---

## Summary

- Use **`api().uniqueIds()`** when you need distinct labels or keys within a test case.
- It provides **readability** by using small, predictable integers.
- It supports **automatic shrinkage**, reducing the number of unique entities to the minimum required to trigger a bug.
- It ensures **perfect repeatability**, unlike `UUID.randomUUID()` or global counters.
