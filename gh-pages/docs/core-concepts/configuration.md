---
layout: default
title: Configuration Options
parent: Core Concepts
nav_order: 4
---

# Configuration Options
{: .no_toc }

Case limits, seeding, complexity limits, and shrinkage control
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Beyond Simple Limits

So far we've used `.withLimit()` to control how many test cases to generate. But Americium offers much more sophisticated configuration for different testing scenarios.

Let's explore the full range of options.

---

## Case Limit Strategies

### Time-Based Limits

Instead of a fixed number of cases, you can specify a **time budget**:
```java
import java.time.Duration;

trials
    .withStrategy(cycle -> 
        CasesLimitStrategy.timed(Duration.ofSeconds(5)))
    .supplyTo(testCase -> {
        // Test runs for approximately 5 seconds
    });
```

This is useful for:
- **Performance testing** - Run as many cases as possible in a fixed time
- **CI/CD** - Limit test duration regardless of complexity
- **Exploratory testing** - Spend fixed time exploring the input space

{: .note }
> The `cycle` parameter represents the current shrinkage cycle - we'll explain this shortly.

---

### Count-Based Limits with Starvation Tolerance

First, what do we mean by 'starvation'? When Americium supplies a test case to a trial, this doesn't always result in the trial being run.

Reasons include:
- A call to `filter` blocking the synthesis of a test case prior to it being supplied.
- Duplicates of test cases already supplied are suppressed.
- The test case is too complex. (We'll cover this later in the Complexity Budgeting section under Advanced Techniques).
- Rejection of a trial by the test itself.

This blocking of supply is what we mean by starvation; we need to heed this because we don't want Americium to keep fruitlessly retrying the supply in an infinite loop.

Fortunately, the `withLimit()` method is actually shorthand for a built-in strategy that handles starvation automatically. If you write a trials instance that keeps starving its test, this built-in strategy will eventually decide to terminate the trials.

We can exert finer control though...
```java
// When starvation doesn't happen, these are equivalent.
trials.withLimit(100)

trials.withStrategy(cycle -> 
    CasesLimitStrategy.counted(100, 0.0))
```

The second parameter of `withStrategy` is the **starvation ratio**. It controls how much filtering/rejection is acceptable:
```java
trials.withStrategy(cycle -> 
    CasesLimitStrategy.counted(
        100,    // Maximum cases
        0.2))   // Allow 20% rejection rate
```

If more than 20% of attempted test cases cause starvation, the trials are terminated.

{: .warning }
> **High rejection rates** suggest your trial specification is inefficient. Consider redesigning to generate valid cases directly rather than filtering.

---

## Understanding Cycles

When you see `cycle -> CasesLimitStrategy...`, what's that about?

**Cycles** represent phases of the testing process:

- **Cycle 0** - Initial exploration (finding a failing case)
- **Cycle 1+** - Shrinkage attempts (finding simpler failing cases)

You can configure **different limits for each cycle**:
```java
trials.withStrategy(cycle -> {
    if (cycle.isInitial) {
        // Exploration: try many cases, accept a large degree of starvation.
        return CasesLimitStrategy.counted(1000, 1);
    } else {
        // Shrinkage: don't try too hard, further shrinkage is just a 'nice to have'.
        return CasesLimitStrategy.counted(500, 0.2);
    }
});
```

Or use time budgets that vary:
```java
trials.withStrategy(cycle -> {
    if (cycle.isInitial) {
        // Exploration: want a quick test and don't want to look at everything.
        return CasesLimitStrategy.timed(Duration.ofSeconds(2));
    } else {
        // Shrinkage: if we do see a failed test case, work hard to shrink it down.
        return CasesLimitStrategy.timed(Duration.ofSeconds(10));
    }
});
```

---

## Seeding for Reproducibility

### Default Behavior: Repeatable

By default, Americium uses a **fixed internal seed**, so runs are **deterministic**:
```java
trials.withLimit(10).supplyTo(System.out::println);
// Always produces the same sequence!
```

This is great for **local development** - same failures reproduce reliably.

---

### Explicit Seeds

You can control the seed explicitly:
```java
trials
    .withLimit(100)
    .withSeed(42L)  // Use specific seed
    .supplyTo(testCase -> {
        // Deterministic with seed 42
    });
```

This is useful for:
- **Reproducing specific runs** - Share the seed value
- **Parameterized testing** - Run same test with different seeds

---

### Non-Deterministic Mode

For **CI/CD**, you might want **different cases on each run** to explore the input space more thoroughly:
```bash
-Dtrials.nondeterministic=true
```

Now each test run uses a **random seed**, generating different test cases:
```java
final Trials<Integer> trials = api().integers(1, 40);
      
trials.withLimit(5).supplyTo(System.out::println);

// Run 1: test cases 2, 14, 12, 4, 5
// Run 2: test cases 8, 18, 26, 21, 30
// Run 3: test cases 23, 29, 28, 7, 2
```

Over many CI runs, you'll cover much more of the input space!

{: .tip }
> **Best practice:** Use deterministic mode locally, non-deterministic in CI.

---

## Complexity Limits

Remember **complexity shrinkage**? You can limit how complex your test cases get:
```java
trials
    .withLimit(100)
    .withComplexityLimit(50)
    .supplyTo(testCase -> {
        // Test cases limited to 50 degrees of freedom
    });
```

This prevents Americium from generating:
- Deeply nested structures
- Very long lists
- Highly complex recursive values

---

### Subtlety: Fixed-Size Collections

Here's an important detail - **fixed-size collections are exempt** from complexity limits:
```java
api().integers()
    .immutableListsOfSize(100)  // Always 100 elements!
    .withLimit(10)
    .withComplexityLimit(1)    // Doesn't affect list size
    .supplyTo(list -> {
        assert list.size() == 100;  // ✓ Always true
    });
```

Why? You **explicitly requested** 100 elements. Americium respects your specification.

But **varying-size** collections respect the limit:
```java
api().integers()
     .immutableLists() // Varying size
     .withLimit(10)
     .withComplexityLimit(5) // Restricts maximum size
     .supplyTo(System.out::println);

// [-1975667456, -54163312]
// []
// [1610305737]
// [1987104774, 1272878179]
// [1327605531, 633033882]
// [-685852310, -2071825701]
// [-1545019583]
// [-1589526881]
// [637450267]
// [499196904, -440688866]
```

{: .note }
> The complexity limit does not directly count the number of items in the list. Think of it as being a **guide heuristic**; the smaller the limit, the shorter the lists in this example.

---

## Shrinkage Control

### Limiting Shrinkage Attempts

You can control **how hard Americium tries to shrink**:
```java
trials
    .withLimit(100)
    .withShrinkageAttemptsLimit(10)
    .supplyTo(testCase -> {
        // Maximum 10 shrinkage attempts after initial failure
    });
```

Setting this to **0 disables shrinkage entirely**:
```java
trials
    .withShrinkageAttemptsLimit(0)  // No shrinkage!
    .supplyTo(testCase -> {
        // Test fails with initial failing case
    });
```

This is useful for:
- **Debugging shrinkage itself** - See the initial failure
- **Performance** - Skip shrinkage when you just want to know "does it fail?"

---

### Advanced: Shrinkage Stop Callback

For **ultimate control**, provide a callback that decides when to stop:
```java
import com.sageserpent.americium.generation.ShrinkageStop;

trials
    .withLimit(100)
    .withShrinkageStop(new ShrinkageStop() {
        private int trialsCount = 0;
        
        @Override
        public boolean test(int testCase) {
            // Custom logic here: regardless of cycle,
            // give up after doing 150 trials overall.
            return 150 == trialsCount++;
        }
    })
    .supplyTo(testCase -> {
        // Uses custom shrinkage stopping logic
    });
```

Parameters:
- **`shrinkageAttempts`** - Total shrinkage attempts so far
- **`consecutiveFailures`** - Failures since last improvement

This allows sophisticated strategies like:
- "Stop if no improvement in last 10 attempts"
- "Stop if we've been shrinking for 30 seconds"
- "Stop if the test case is 'simple enough'"

---

## Configuration Chaining

All configuration methods can be **chained**:
```java
trials
    .withLimit(200)
    .withSeed(12345L)
    .withComplexityLimit(75)
    .withShrinkageAttemptsLimit(30)
    .supplyTo(testCase -> {
        // Fully configured!
    });
```

Or using the strategy approach:
```java
trials
    .withStrategy(cycle -> 
        cycle == 0 
            ? CasesLimitStrategy.timed(Duration.ofSeconds(10))
            : CasesLimitStrategy.counted(100, 0.15))
    .withSeed(42L)
    .withComplexityLimit(50)
    .withShrinkageStop(myCustomStop)
    .supplyTo(testCase -> {
        // Even more configured!
    });
```

---

## Practical Recommendations

### For Local Development
```java
trials
    .withLimit(100)              // Quick feedback
    .withShrinkageAttemptsLimit(20)  // Good shrinkage
    // Default seed (deterministic)
```

### For CI/CD
```bash
# Run with:
-Dtrials.nondeterministic=true
```
```java
trials
    .withStrategy(cycle -> 
        CasesLimitStrategy.timed(Duration.ofSeconds(30)))
    .withComplexityLimit(100)
    .withShrinkageAttemptsLimit(50)
```

### For Performance Testing
```java
trials
    .withStrategy(cycle -> 
        CasesLimitStrategy.timed(Duration.ofMinutes(5)))
    .withShrinkageAttemptsLimit(0)  // Skip shrinkage
```

### For Debugging
```java
trials
    .withLimit(10)
    .withShrinkageAttemptsLimit(0)  // See raw failures
    .withSeed(specificSeed)         // Reproduce specific run
```

---

{: .note-title }
> Key Takeaways
>
> - **Two limit strategies:** Time-based and count-based
> - **Count-based** supports starvation ratio for rejection tolerance
> - **Cycles:** Cycle 0 = exploration, Cycles 1+ = shrinkage
> - **Seeds:** Fixed by default (repeatable), random with `-Dtrials.nondeterministic=true`
> - **Complexity limits** control test case complexity (but exempt fixed-size collections)
> - **Shrinkage control:** Limit attempts or use custom stop callbacks
> - **Configuration chains** - combine multiple settings
> - Different configurations for different scenarios (local vs CI vs debugging)