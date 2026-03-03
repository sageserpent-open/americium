---
layout: default
title: Permutations
parent: Advanced Techniques
nav_order: 3
---

# Permutations
{: .no_toc }

Testing order-dependent behavior with index permutations
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
   {:toc}

---

## The Problem

Many algorithms are **order-dependent**:

- **Sorting** algorithms should produce the same result regardless of input order
- **Merging** operations should maintain relative order
- **Sequential processing** might have bugs that only appear in certain orderings
- **Concurrent systems** might have race conditions triggered by specific event orderings

How do you test that behavior is **invariant under reordering**?

**Naive approach:**
```java
// Generate random lists - but orderings are random and hard to control
api().integers().immutableLists()
```

**Better approach:**
- Generate **correct output**
- **Permute it** to create input
- Verify the algorithm **reconstructs the correct output**

---

## The Solution: `api().indexPermutations()`

Americium provides a way to generate **permutations of indices**:
```java
api().indexPermutations(5)
    .withLimit(10)
    .supplyTo(permutation -> {
        System.out.println(permutation);
    });
```

Output:
```
[0, 1, 2, 3, 4]  ← Identity permutation
[1, 0, 2, 3, 4]  ← Swap first two
[0, 2, 1, 3, 4]  ← Swap second and third
[3, 1, 2, 0, 4]  ← More complex
...
```

---

## How It Works

`api().indexPermutations(n)` generates **permutations of indices** `[0, n-1]`.

You can think of it as: "Given a list of size `n`, these are all the ways to reorder it."

### Example with n=3

All permutations of `[0, 1, 2]`:
```
[0, 1, 2]  ← Original order
[0, 2, 1]  ← Swap last two
[1, 0, 2]  ← Swap first two
[1, 2, 0]  ← Rotate right
[2, 0, 1]  ← Rotate left
[2, 1, 0]  ← Reverse
```

There are **n! = 3! = 6** total permutations.

---

## Basic Usage: Testing Sorting

The classic use case - test that sorting works regardless of input order:
```java
api().only(15).flatMap(size -> {
    final ImmutableList<Integer> sourceCollection = 
        IntStream.range(0, size)
            .boxed()
            .collect(ImmutableList.toImmutableList());
    
    return api()
        .indexPermutations(size)
        .map(permutation -> {
            // Apply permutation to create shuffled input
            return permutation.stream()
                .map(sourceCollection::get)
                .collect(ImmutableList.toImmutableList());
        });
})
.withLimit(100)
.supplyTo(shuffledInput -> {
    // Sort the shuffled input
    List<Integer> sorted = new ArrayList<>(shuffledInput);
    Collections.sort(sorted);
    
    // Should get back [0, 1, 2, ..., 14]
    assertThat(sorted, 
        equalTo(IntStream.range(0, 15)
            .boxed()
            .collect(Collectors.toList())));
});
```

### What This Tests

1. Create the **correct output**: `[0, 1, 2, ..., 14]`
2. **Permute** it to create shuffled inputs
3. **Sort** each shuffled input
4. Verify we get back the **original correct output**

This tests sorting with **all possible input orderings** (up to the trial limit).

---

## Shrinkage Toward Identity

When a test fails, permutations shrink toward the **identity permutation** `[0, 1, 2, ..., n-1]`:
```java
api().indexPermutations(10)
    .withLimit(1000)
    .supplyTo(perm -> {
        // Simulate a bug when index 5 comes before index 3
        int index5Pos = perm.indexOf(5);
        int index3Pos = perm.indexOf(3);
        
        if (index5Pos < index3Pos) {
            throw new AssertionError("Bug! Permutation: " + perm);
        }
    });
```

Shrinkage:
```
Initial failure: [5, 1, 7, 3, 0, 2, 9, 6, 4, 8]
Shrinking...
[5, 1, 3, 2, 0, 4, 6, 7, 8, 9]
[5, 1, 3, 0, 2, 4, 6, 7, 8, 9]
[5, 3, 0, 1, 2, 4, 6, 7, 8, 9]
[5, 3, 0, 1, 2, 4, 6, 7, 8, 9]  ← Close to identity with minimal swap!
```

The shrunk case shows **the minimal permutation** that still triggers the bug.

---

## Alternative Shrinkage: Minimal Swap

You can configure permutations to shrink toward the **minimal swap** instead of identity:
```java
// This is an advanced feature - check API docs for exact syntax
```

**Identity shrinkage** (default): `[2, 1, 0, 3, 4]` → `[0, 1, 2, 3, 4]`  
**Minimal swap**: `[2, 1, 0, 3, 4]` → `[1, 0, 2, 3, 4]` (just one swap)

---

## Real-World Example: SortedMap

Testing that a `SortedMap` maintains order:
```java
api().only(15).flatMap(size -> {
    final List<Integer> keys = 
        IntStream.range(0, size)
            .boxed()
            .collect(Collectors.toList());
    
    final List<Integer> values = 
        IntStream.range(0, size)
            .boxed()
            .collect(Collectors.toList());
    
    return api()
        .indexPermutations(size)
        .map(permutation -> {
            // Create a SortedMap with permuted insertion order
            SortedMap<Integer, Integer> map = new TreeMap<>();
            
            for (int idx : permutation) {
                map.put(keys.get(idx), values.get(idx));
            }
            
            // Verify size
            assume(map.size() == size);
            
            // Verify values in sorted order
            assume(ImmutableList.copyOf(map.values()).equals(values));
            
            return map;
        });
})
.withLimit(15)
.supplyTo(sortedMap -> {
    Trials.whenever(sortedMap.size() > 0, () -> {
        // Verify map is actually sorted
        List<Integer> keysList = new ArrayList<>(sortedMap.keySet());
        
        for (int i = 0; i < keysList.size() - 1; i++) {
            assertThat(
                "Keys should be in order",
                keysList.get(i),
                lessThan(keysList.get(i + 1))
            );
        }
    });
});
```

---

## Combining with Other Techniques

### Permutations + Unique IDs

Test account operations in different orders:
```java
api().uniqueIds()
    .immutableListsOfSize(10)
    .flatMap(accountIds ->
        api().indexPermutations(10).map(perm -> {
            // Operate on accounts in permuted order
            return perm.stream()
                .map(accountIds::get)
                .collect(ImmutableList.toImmutableList());
        }))
    .withLimit(100)
    .supplyTo(orderedAccountIds -> {
        orderedAccountIds.forEach(id -> {
            openAccount(id);
            deposit(id, 100);
            withdraw(id, 50);
        });
        
        // Verify final state is consistent
        assertThat(totalBalance(), equalTo(500));  // 10 accounts * $50 each
    });
```

---

### Permutations + Forcing Duplicates

Test sorting with duplicate values:
```java
api().integers(1, 5).flatMap(n ->
    api().integers(1, 10)
        .immutableListsOfSize(n)
        .flatMap(choices ->
            api().choose(choices)
                .immutableListsOfSize(n)
                .flatMap(values ->
                    api().indexPermutations(n).map(perm -> {
                        // Permute list with duplicates
                        return perm.stream()
                            .map(values::get)
                            .collect(ImmutableList.toImmutableList());
                    }))))
.withLimit(100)
.supplyTo(permutedWithDuplicates -> {
    List<Integer> sorted = new ArrayList<>(permutedWithDuplicates);
    Collections.sort(sorted);
    
    // Verify sorted list properties
    // ...
});
```

---

## Performance Considerations

### Factorial Explosion

Be aware: the number of permutations grows **factorially**:

| Size | Permutations |
|------|--------------|
| 3 | 6 |
| 5 | 120 |
| 7 | 5,040 |
| 10 | 3,628,800 |
| 15 | 1,307,674,368,000 |

For size 15, you can't possibly test **all** permutations. Americium samples from the space.

### Recommendation

For testing purposes:
- **Small sizes** (3-7): Can test many/all permutations
- **Medium sizes** (8-15): Test a representative sample
- **Large sizes** (16+): Permutations become less practical

---

## When to Use Permutations

### ✅ Use when:
- Testing **sorting** algorithms
- Testing **order-invariant** operations
- Finding **ordering-dependent** bugs
- Testing **merge** or **interleaving** operations
- Verifying **concurrent** event orderings

### ❌ Don't use when:
- Order genuinely **doesn't matter** (just use random lists)
- Size is **too large** (factorial explosion)
- You need **specific** orderings (use `.choose()` instead)

---

## Scala Example

Permutations work beautifully with Scala for-comprehensions:
```scala
val sortingTests = for {
  size <- api.only(10)
  permutation <- api.indexPermutations(size)
} yield {
  val original = (0 until size).toList
  permutation.map(original).toList
}

sortingTests.withLimit(100).supplyTo { shuffled =>
  val sorted = shuffled.sorted
  assert(sorted == (0 until shuffled.size).toList)
}
```

---

## JUnit5 Integration Example
```java
public class SortingTest {
    @TrialsTest(trials = "permutedLists", casesLimit = 100)
    void sortingShouldWorkRegardlessOfInputOrder(List<Integer> input) {
        List<Integer> sorted = new ArrayList<>(input);
        Collections.sort(sorted);
        
        // Should be [0, 1, 2, ..., input.size()-1]
        assertThat(sorted, 
            equalTo(IntStream.range(0, input.size())
                .boxed()
                .collect(Collectors.toList())));
    }
    
    private static final Trials<List<Integer>> permutedLists =
        api().only(10).flatMap(size -> {
            final List<Integer> original = 
                IntStream.range(0, size)
                    .boxed()
                    .collect(Collectors.toList());
            
            return api()
                .indexPermutations(size)
                .map(perm -> 
                    perm.stream()
                        .map(original::get)
                        .collect(Collectors.toList()));
        });
}
```

---

{: .note-title }
> Key Takeaways
>
> - **`api().indexPermutations(n)`** - Generates permutations of indices [0, n-1]
> - **Classic pattern:** Generate correct output → permute → verify reconstruction
> - **Shrinks to identity** - `[0, 1, 2, ..., n-1]` by default
> - **Alternative:** Shrink to minimal swap
> - **Factorial explosion** - n! permutations (test samples for large n)
> - Perfect for testing sorting, merging, order-invariant operations
> - Combine with unique IDs, duplicates, and other techniques
> - Helps find ordering-dependent bugs