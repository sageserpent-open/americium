---
layout: default
title: Unique IDs
parent: Advanced Techniques
nav_order: 2
---

# Unique IDs
{: .no_toc }

Creating readable, unique identifiers that shrink well
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem

When testing systems that use **identifiers** (user IDs, account IDs, transaction IDs, etc.), you need IDs that are:

1. **Unique** within a test case
2. **Readable** when debugging (not random UUIDs)
3. **Shrink well** when tests fail
4. **Repeatable** across test runs

Random approaches have problems:
```java
// Random UUIDs - not readable
UUID.randomUUID()  
// → "7f3e9c2a-4b1d-4e5f-9a8c-1d2e3f4a5b6c" 😵

// Random integers - might collide!
api().integers()
// → Duplicates possible, defeats uniqueness

// Sequential counters - not repeatable
int id = counter++;
// → Different values on each test run
```

What you want: **`api().uniqueIds()`**

---

## The Solution: `api().uniqueIds()`

Americium provides a built-in trial type for generating unique identifiers:
```java
import static com.sageserpent.americium.java.Trials.api;

final Trials<Integer> uniqueIds = api().uniqueIds();

uniqueIds.withLimit(10).supplyTo(id -> {
    System.out.println("ID: " + id);
});
```

Output:
```
ID: 0
ID: 1
ID: 2
ID: 3
ID: 4
ID: 5
ID: 6
ID: 7
ID: 8
ID: 9
```

---

## Properties of Unique IDs

### ✅ Small, Readable Integers

IDs are **small integers** starting from 0:
```
0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, ...
```

Not:
```
7f3e9c2a-4b1d-4e5f-9a8c-1d2e3f4a5b6c  ← UUID chaos
8472619384726  ← Giant random number
```

---

### ✅ Unique Within Test Case

Within a **single test case**, IDs are guaranteed unique:
```java
api().uniqueIds()
    .immutableListsOfSize(100)
    .withLimit(1)
    .supplyTo(ids -> {
        // All 100 IDs are distinct!
        assertThat(ids.stream().distinct().count(), is(100L));
    });
```

---

### ✅ Repeatable Across Runs

With the same seed, you get the **same IDs**:
```java
api().uniqueIds()
    .withLimit(5)
    .withSeed(42)
    .supplyTo(System.out::println);

// Always produces: 0, 1, 2, 3, 4
```

---

### ✅ Shrink Well

When a test fails, IDs shrink toward **smaller values**:
```java
api().uniqueIds()
    .immutableLists()
    .withLimit(100)
    .supplyTo(ids -> {
        // Some test that fails...
    });

// Initial failure: [47, 23, 91, 15, 88, 62]
// After shrinkage: [0, 1, 2]  ← Much easier to debug!
```

---

## Basic Usage

### Single IDs
```java
final Trials<Integer> accountIds = api().uniqueIds();

accountIds.withLimit(10).supplyTo(id -> {
    Account account = new Account(id);
    // Test account operations...
});
```

---

### Lists of IDs
```java
final Trials<ImmutableList<Integer>> userIds = 
    api().uniqueIds().immutableLists();

userIds.withLimit(50).supplyTo(ids -> {
    // Create users with unique IDs
    ids.forEach(id -> system.createUser(id));
    // Test multi-user scenarios...
});
```

---

### Making IDs Readable

Map to strings for better readability:
```java
final Trials<String> groupNames = 
    api().uniqueIds().map(id -> String.format("Group-%d", id));

groupNames.withLimit(5).supplyTo(System.out::println);
```

Output:
```
Group-0
Group-1
Group-2
Group-3
Group-4
```

Much nicer than `Group-7f3e9c2a-4b1d-4e5f-9a8c-1d2e3f4a5b6c`!

---

## Real-World Example: Bank Account System

Let's test a banking system that handles multiple accounts:
```java
import com.sageserpent.americium.java.CashBoxAccounts;

final Trials<ImmutableList<CashBoxAccounts.OperationId>> testPlans = 
    api().uniqueIds()
        .map(CashBoxAccounts.OperationId::new)
        .immutableLists();

testPlans.withLimit(100).supplyTo(plan -> {
    final CashBoxAccounts cashBoxAccounts = new CashBoxAccounts();
    
    for (final CashBoxAccounts.OperationId operationId : plan) {
        // Each operation has a unique ID
        final int operationType = random.nextInt(4);
        
        switch (operationType) {
            case 0:
                cashBoxAccounts.open(operationId);
                break;
            case 1:
                Trials.whenever(
                    cashBoxAccounts.isOpen(operationId),
                    () -> cashBoxAccounts.deposit(operationId, randomAmount())
                );
                break;
            case 2:
                Trials.whenever(
                    cashBoxAccounts.canWithdraw(operationId, randomAmount()),
                    () -> cashBoxAccounts.withdrawal(operationId, randomAmount())
                );
                break;
            case 3:
                cashBoxAccounts.close(operationId);
                break;
        }
    }
    
    // Verify account invariants
    assertThat(cashBoxAccounts.balance(), greaterThanOrEqualTo(0));
});
```

When this test fails, you'll see something like:
```
Failed with operation IDs: [0, 1, 2, 0, 3]
                            ↑        ↑
                    Same account operated on twice!
```

**Much clearer** than:
```
Failed with operation IDs: [7f3e9c2a..., 4b1d2e3f..., 9a8c1d2e..., 7f3e9c2a..., 5f6c7d8e...]
```

---

## Advanced Pattern: Multi-Entity Systems

Testing systems with multiple entity types (users, groups, resources):
```java
// Generate users, groups, and documents with unique IDs
final Trials<TestScenario> scenarios = 
    api().uniqueIds().immutableLists().flatMap(userIds ->
        api().uniqueIds().immutableLists().flatMap(groupIds ->
            api().uniqueIds().immutableLists().map(docIds ->
                new TestScenario(userIds, groupIds, docIds))));

scenarios.withLimit(50).supplyTo(scenario -> {
    // Users: [0, 1, 2, 3]
    // Groups: [0, 1, 2]  ← Unique within groups (can overlap with users!)
    // Docs: [0, 1, 2, 3, 4]  ← Unique within docs
    
    // Set up entities
    scenario.userIds.forEach(id -> system.createUser("User-" + id));
    scenario.groupIds.forEach(id -> system.createGroup("Group-" + id));
    scenario.docIds.forEach(id -> system.createDocument("Doc-" + id));
    
    // Test multi-entity interactions...
});
```

{: .note }
> **Important:** Unique IDs are unique **per trials instance**, not globally! Different `api().uniqueIds()` calls produce independent sequences.

---

## Combining with Other Techniques

### Unique IDs + Permutations

Test operations in different orders:
```java
api().uniqueIds()
    .immutableListsOfSize(10)
    .flatMap(ids ->
        api().indexPermutations(10).map(perm -> {
            // Permute the order of operations on these accounts
            return perm.map(ids::get);
        }))
    .withLimit(100)
    .supplyTo(orderedIds -> {
        orderedIds.forEach(id -> {
            performOperation(id);
        });
    });
```

---

### Unique IDs + Choices

Mix unique IDs with random choices:
```java
api().uniqueIds()
    .immutableLists()
    .flatMap(accountIds ->
        api().choose("deposit", "withdraw", "transfer")
            .immutableLists()
            .map(operations ->
                new TestPlan(accountIds, operations)))
    .withLimit(100)
    .supplyTo(plan -> {
        // Unique accounts, random operations
    });
```

---

## Scala Example

In Scala, unique IDs work beautifully with for-comprehensions:
```scala
val scenarios = for {
  userIds <- api.uniqueIds().immutableLists()
  groupIds <- api.uniqueIds().immutableLists()
  actions <- api.choose("create", "delete", "update").immutableLists()
} yield TestScenario(userIds, groupIds, actions)

scenarios.withLimit(100).supplyTo { scenario =>
  scenario.userIds.foreach(id => system.createUser(s"User-$id"))
  scenario.groupIds.foreach(id => system.createGroup(s"Group-$id"))
  scenario.actions.foreach(performAction)
}
```

---

## When Uniqueness is Per-Trial

Remember: unique IDs are unique **within a single test case**, not across trials:
```java
api().uniqueIds()
    .withLimit(5)
    .supplyTo(id -> {
        System.out.println("Trial ID: " + id);
    });
```

Output:
```
Trial ID: 0  ← Each trial sees this same ID
Trial ID: 0  ← Not unique across trials!
Trial ID: 0
Trial ID: 0
Trial ID: 0
```

For **multiple IDs per trial**, use collections:
```java
api().uniqueIds()
    .immutableListsOfSize(3)
    .withLimit(5)
    .supplyTo(ids -> {
        System.out.println("IDs this trial: " + ids);
    });
```

Output:
```
IDs this trial: [0, 1, 2]  ← Unique within this trial
IDs this trial: [0, 1, 2]  ← Same pattern, different trial
IDs this trial: [0, 1, 2]
IDs this trial: [0, 1, 2]
IDs this trial: [0, 1, 2]
```

---

## Shrinkage Example

See unique IDs shrink in action:
```java
api().uniqueIds()
    .immutableLists()
    .withLimit(100)
    .supplyTo(ids -> {
        // Simulate a bug that only happens with ID 5
        if (ids.contains(5)) {
            throw new AssertionError("Bug with ID 5! List: " + ids);
        }
    });
```

Shrinkage progression:
```
Initial failure: [0, 12, 5, 23, 7, 18, 5, 9]
Shrinking...
Attempt: [0, 5, 7]
Attempt: [0, 5]
Attempt: [5]  ← Maximally shrunk!

Final failure: [5]
```

Perfect! The shrunk case clearly shows **ID 5 is the problem**.

---

## Comparison with Alternatives

| Approach | Readable? | Unique? | Shrinks? | Repeatable? |
|----------|-----------|---------|----------|-------------|
| `UUID.randomUUID()` | ❌ | ✅ | ❌ | ❌ |
| `api().integers()` | ✅ | ❌ | ✅ | ✅ |
| Sequential counter | ✅ | ✅ | ❌ | ❌ |
| **`api().uniqueIds()`** | ✅ | ✅ | ✅ | ✅ |

---

## Best Practices

### ✅ Do:
```java
// Use unique IDs for entity identifiers
api().uniqueIds().map(id -> new User(id))

// Map to readable strings
api().uniqueIds().map(id -> String.format("Account-%d", id))

// Combine with collections for multi-entity tests
api().uniqueIds().immutableLists()
```

### ❌ Don't:
```java
// Don't use for random values (use api().integers() instead)
api().uniqueIds()  // If you don't need uniqueness

// Don't expect global uniqueness across trials instances
val ids1 = api().uniqueIds()
val ids2 = api().uniqueIds()
// These will produce overlapping sequences!

// Don't use for cryptographic purposes
api().uniqueIds()  // These are predictable, not secure
```

---

## Internal Implementation Note

Under the hood, `api().uniqueIds()` generates a **monotonically increasing sequence** starting from 0. The sequence is **deterministic** based on the seed, ensuring repeatability.

Each trials instance maintains its **own counter**, so multiple `api().uniqueIds()` calls produce independent sequences.

---

{: .note-title }
> Key Takeaways
>
> - **`api().uniqueIds()`** - Built-in support for unique identifiers
> - **Small integers** (0, 1, 2, ...) - Readable and debuggable
> - **Unique within test case** - No collisions in the same trial
> - **Repeatable** - Same seed produces same IDs
> - **Shrink well** - Failures reduce to small ID values
> - **Map to strings** - Format as "User-0", "Account-1", etc.
> - **Per-instance uniqueness** - Different trials instances = independent sequences
> - Perfect for testing multi-entity systems (users, accounts, resources)
> - Combine with lists, permutations, and other techniques