---
layout: default
title: Reproducing Failing Test Cases
parent: Core Concepts
nav_order: 2
---

# Reproducing Failing Test Cases
{: .no_toc }

Recipe hashes, JSON recipes, and the run database
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
   {:toc}

---

## Why Reproduction Matters

When a property test fails and shrinks down to a minimal case, you need to be able to **reproduce that exact failure** reliably. This is crucial for:

- **Debugging** - Run the failing case repeatedly while stepping through code
- **Verification** - Confirm your fix actually resolves the issue
- **CI/CD** - Ensure the same failure reproduces in continuous integration
- **Collaboration** - Share exact failing cases with teammates

Americium provides two complementary mechanisms for reproduction.

---

## Example: Testing Tiers

Let's use a complex example to demonstrate reproduction - testing a `Tiers` data structure:
```java
import static com.sageserpent.americium.java.Trials.api;

final Trials<ImmutableList<Integer>> queryValueLists = api()
    .integers(-1000, 1000)
    .immutableLists()
    .filter(list -> !list.isEmpty());

final Trials<Tuple2<ImmutableList<Integer>, ImmutableList<Integer>>> testCases =
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
            queryValues.stream()
                .flatMap(queryValue ->
                    Stream.of(
                        api().only(ImmutableList.of(queryValue)),
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
            
        return feedSequences.map(feedSequence -> 
            Tuple.tuple(queryValues, feedSequence));
    });

testCases.withLimit(30).supplyTo(testCase -> {
    final ImmutableList<Integer> queryValues = testCase._1();
    final ImmutableList<Integer> feedSequence = testCase._2();

    final int worstTier = queryValues.size();
    final Tiers<Integer> tiers = new Tiers<>(worstTier);

    feedSequence.forEach(tiers::add);

    // Test implementation...
    // (This will fail due to an injected bug)
});
```

---

## Method 1: Recipe Hash (Local Development)

When a test fails, Americium prints a **recipe hash**:
```
Recipe hash for reproduction: b91fa06969da28bd58ca711b7b50ff75
```

To reproduce this exact failure, run with:
```bash
-Dtrials.recipeHash=b91fa06969da28bd58ca711b7b50ff75
```

### How It Works

The recipe hash is stored in a **local database** (in your system's temp directory). When you provide the hash, Americium:

1. Looks up the hash in the database
2. Retrieves the complete recipe
3. Reproduces the exact test case

{: .note }
> **Local only:** Recipe hashes work great for local debugging but don't transfer to other machines or CI environments. The database is in your temp folder and gets cleaned periodically.

---

## Method 2: Recipe JSON (Portable/CI)

For **reproducibility across machines** (especially CI), use the JSON recipe:
```
Recipe JSON for reproduction:
[{"ChoiceOf":{"index":1}},{"ChoiceOf":{"index":0}}, ...]
```

To reproduce, run with:
```bash
-Dtrials.recipe='[{"ChoiceOf":{"index":1}},{"ChoiceOf":{"index":0}}, ...]'
```

### How It Works

The recipe is a **sequence of decisions** Americium made while generating the test case:

- **`ChoiceOf`** - Which choice was picked from an alternation or `.choose()`
- **`FactoryInputOf`** - What input was fed to a case factory (for streams like `.integers()`)

By replaying these decisions, Americium reconstructs the exact same test case on any machine.

{: .tip }
> **CI/CD Best Practice:** Include the recipe JSON in your test failure reports so anyone can reproduce the failure anywhere.

---

## The Run Database

### Location

Recipes are stored in a database located at:
```
{temp-dir}/{database-name}/
```

Where:
- **`temp-dir`** - Java system property `java.io.tmpdir`
- **`database-name`** - Java property `trials.runDatabase` (default: `runDatabase`)

### Customizing the Location
```bash
# Change the database name
-Dtrials.runDatabase=my-custom-db

# Or override the temp directory
-Djava.io.tmpdir=/my/custom/temp
```

### Structure

The database stores:
- **Recipe hash → Recipe mapping** - Full decision sequence for reproduction
- **Recipe outline** - Structural summary of the test case

### Lifecycle

- Created automatically when trials run
- Cleaned up by OS temp directory maintenance
- **Not shared** between developers or CI runs
- Useful for local debugging sessions

---

## Cliffhanger: Injected Fault

In our `Tiers` example, we injected a subtle bug:
```java
if (0 >= index) {  // Bug: should be 0 > index
    throw new IllegalArgumentException();
}
```

This allows index 0 through, causing the assertion to fail. But when we run the test with 30 trials, **it doesn't catch the bug**!

Why? The configuration isn't quite right. We'll fix this in the Configuration topic coming up...

---

## Practical Workflow

Here's how reproduction typically works in practice:

### Local Development

1. Test fails, Americium prints recipe hash
2. Copy the hash
3. Re-run with `-Dtrials.recipeHash=...`
4. Debug the specific failure
5. Fix the code
6. Verify the fix with the same recipe

### CI/CD Pipeline

1. Test fails in CI
2. CI logs include recipe JSON
3. Developer copies recipe JSON
4. Runs locally with `-Dtrials.recipe='...'`
5. Reproduces the exact failure locally
6. Debugs and fixes
7. Pushes fix, CI re-runs and passes

---

{: .note-title }
> Key Takeaways
>
> - **Two reproduction methods:** Recipe hash (local) and recipe JSON (portable)
> - **Recipe hash** - Quick local reproduction via database lookup
> - **Recipe JSON** - Portable reproduction for CI/CD and collaboration
> - **Run database** - Stored in temp directory, manages recipe storage
> - **Recipes encode decisions** - Choice indices and factory inputs
> - Properties: `java.io.tmpdir`, `trials.runDatabase`
> - JSON recipes can be shared across machines and team members