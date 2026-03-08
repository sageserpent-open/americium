---
layout: default
title: All About Shrinkage
parent: Core Concepts
nav_order: 3
---

# All About Shrinkage
{: .no_toc }

Distance shrinkage, complexity shrinkage, and how they work together
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## What is Shrinkage?

When a property test fails, you get a **failing test case**. But that initial failure might be a huge, complex value that's hard to understand. Shrinkage is the process of **automatically finding smaller, simpler failing cases** that are easier to debug.

Americium's shrinkage is **integrated** - you don't write separate shrinkers. It happens automatically based on how you build your trials.

There are **two complementary aspects** to shrinkage in Americium:

1. **Distance Shrinkage** - Values move toward a target (usually zero)
2. **Complexity Shrinkage** - Structures become simpler (fewer elements, shallower nesting)

Let's explore both.

---

## Distance Shrinkage

When Americium generates scalar values (integers, doubles, etc.), it can shrink them toward a **target value**.

### Default Target: Zero

By default, numeric values shrink toward **zero**:
```java
final Trials<Integer> integers = api().integers(-1000, 1000);

integers.withLimit(100).supplyTo(x -> {
    assertThat(x * x, lessThan(100));
});
```

When this fails, you might see:
```
Initial failure:
    x = 847
After shrinkage:
    x = 10
```

The value **shrinks toward zero** because that's the default target. Both 10 and -10 would fail, but Americium found 10 first.

---

### Custom Shrinkage Targets

You can specify **custom targets** for shrinkage:
```java
// Shrink toward -152.753
final Trials<Double> doubles = 
    api().doubles(-1e10, 1e10, -152.753);

doubles.withLimit(100).supplyTo(x -> {
    assertThat(x, lessThan(-100.0));
});
```

Now when the test fails, values will shrink **toward -152.753** rather than toward zero.

### Character Targets

Characters can also have shrinkage targets:
```java
// Shrink toward 'm'
final Trials<Character> characters = 
    api().characters('a', 'z', 'm');

characters.withLimit(100).supplyTo(c -> {
    assertThat(c, lessThan('h'));
});
```

Failures will shrink toward 'm'.

---

### Limitations of Distance Shrinkage

Not all trial types support custom targets:

**`.choose()` doesn't shrink** - All choices are considered equally valid:
```java
final Trials<Integer> primes = 
    api().choose(2, 3, 5, 7, 11, 13, 17, 19);

// All primes are equally valid - no shrinkage between them
```

{: .note }
> This makes sense: if you explicitly chose these values, Americium assumes they're all equally important test cases.

**Some overloads lack obvious defaults** - Methods without range parameters may not have clear shrinkage targets. Check the API documentation for specifics.

---

### Shrinkage Preserves Through Mapping

When you **map** over a trials instance, shrinkage is **preserved**:
```java
final Trials<Integer> integers = api().integers(0, 100);

final Trials<String> strings = integers.map(n -> "Value: " + n);
```

The `strings` trials will shrink because the underlying `integers` shrink toward zero. The string "Value: 0" is the shrinkage target.

{: .important }
> **Key insight:** Shrinkage applies to the **source values**, not the mapped results. The mapping is just applied to whatever shrunk value Americium finds.

---

## Complexity Shrinkage

The second aspect of shrinkage is **structural simplification** - reducing the complexity of the test case.

### Example: String Shrinkage
```java
final String suffix = "are";

final int suffixLength = suffix.length();

final Trials<String> strings = api()
        .characters('a', 'z')
        .strings()
        .filter(caze -> caze.length() >
                        suffixLength);


strings.withLimit(20000).supplyTo(input -> {
    try {
        assertThat(input, not(endsWith(suffix)));
    } catch (Throwable throwable) {
        System.out.println(input);
        throw throwable;
    }
});
```

We're looking for 'words' that end in the suffix "are" but must be longer than the suffix itself.

When this fails:
```
Initial failure:
    "qzqiare"  (7 characters)
After shrinkage:
    "rare"     (4 characters)
```

The string got **shorter** - that's complexity shrinkage. But notice it also found a **minimal substring** that still fails the test.

---

### How Complexity Works

Complexity in Americium is measured by **degrees of freedom** - the number of independent decisions made to construct a test case.

Consider building a list:
- **Empty list**: 1 degree of freedom (decide not to include any elements)
- **1-element list**: 3 degrees of freedom (decide to include an element, decide which element to choose, decide not to include any more elements)


When shrinking, Americium tries to **reduce degrees of freedom**:
```java
final Trials<ImmutableList<Integer>> lists = 
    api().integers(1, 100).immutableLists();

lists.withLimit(100).supplyTo(list -> {
    int sum = list.stream().reduce(0, Integer::sum);
    assertThat(sum, lessThan(50));
});
```

Shrinkage will try to:
1. **Reduce list length** (fewer elements = lower complexity)
2. **Shrink element values** toward zero (distance shrinkage)

You might see:
```
Initial failure:
    [89, 45, 23, 67, 91] (sum = 315)
After shrinkage:
    [50]                 (sum = 50)
```

Both aspects working together: shorter list (complexity) + smaller value (distance).

---

### Why "Degrees of Freedom"?

Americium doesn't just look at the final test case - it tracks **how the test case was built**:
```java
// These can produce the same list [1, 2, 3] at some point
// But with different complexity!

// Zero complexity: predetermined list
api().only(ImmutableList.of(1, 2, 3))

// Low complexity: each element chosen independently  
api().choose(-1, 1).and(api().choose(-2, 2)).and(api().choose(-3, 3))
    .map((a, b, c) -> ImmutableList.of(a, b, c))

// High complexity: list built up.
api().integers(1, 3).immutableLists(3)
```

The same final value can have different complexity depending on **how it was constructed**.

---

## Combined Shrinkage

The real power comes when **both aspects work together**:
```java
final Trials<ImmutableList<Integer>> lists = 
    api().integers(-100, 100).immutableLists();

lists.withLimit(100).supplyTo(list -> {
    // Fails if any element is negative
    assertThat(list.stream().allMatch(x -> x >= 0), is(true));
});
```

Initial failure might be:
```
[-87, 34, -56, 92, -12, 48, 71, -3]
```

After shrinkage:
```
[-1]
```

What happened?
1. **Complexity shrinkage** → Reduced list from 8 elements to 1
2. **Distance shrinkage** → Shrunk -87 toward zero, landing at -1

Both mechanisms collaborated to find the **minimal failing case**.

---

## How Americium Tracks Construction

This is the clever part: Americium doesn't just see the final value. It records **every decision** made during construction:
```java
api().integers(1, 10)              // Decision: pick an integer
    .flatMap(n ->                  // For each integer...
        api().characters('a', 'z') // Decision: pick a character
            .lotsOfSize(n))        // Repeat n times
```

For this trials, Americium tracks:
- Which integer was chosen (e.g., 5)
- Which characters were chosen (e.g., 'q', 'z', 'a', 'i', 'r')

When shrinking:
- Try smaller integer (5 → 4 → 3 → ...)
- Try characters closer to 'a' ('q' → 'p' → ... → 'a')

Both dimensions shrink **independently and simultaneously**.

---

## Practical Example: Recursive Structures

Consider our calculator expression example from earlier:
```java
public static Trials<String> calculation() {
    final Trials<String> constants =
        api().integers(1, 100).map(x -> x.toString());

    final Trials<String> unaryExpression =
        api().delay(() -> calculation()
            .map(expr -> String.format("-(%s)", expr)));

    final Trials<String> binaryExpression =
        api().delay(() -> calculation().flatMap(lhs -> 
            api().choose("+", "-", "*", "/").flatMap(op -> 
                calculation().map(rhs -> 
                    String.format("(%s) %s (%s)", lhs, op, rhs)))));

    return api().alternate(constants, unaryExpression, binaryExpression);
}
```

When a test fails on a complex expression:
```
((-(((42) + (7)) * ((89) - (3)))) / (((12) * (56)) + (9)))
```

Shrinkage will:
1. **Reduce nesting depth** (complexity shrinkage)
2. **Simplify subexpressions** (complexity shrinkage)
3. **Shrink numeric constants** toward zero (distance shrinkage)

Final shrunk case might be:
```
1
```

Just the simplest expression that still fails the test!

---

## Shrinkage Guarantees

Americium makes **no guarantee** that the shrunk test case is the **absolute minimum**. It aims for "maximally shrunk" not "globally minimal":

- Shrinkage is **heuristic** - it searches for simpler failing cases
- Higher limits generally produce better shrinkage

{: .tip }
> **Want better shrinkage?** Increase the case limit! More trials = more opportunities to find simpler failing cases.

---

## When Shrinkage Stops

Shrinkage stops when:

1. **No simpler case fails** - Can't reduce complexity or distance further
2. **Shrinkage limit reached** - Hit the configured maximum shrinkage attempts
3. **Time budget exhausted** - Using time-based strategy (covered in Configuration)

---

## Mapping and Shrinkage Revisited

Let's emphasize this critical point with a concrete example:
```java
final Trials<Integer> positives = 
    api().integers(1, 1000, 2);  // Shrinks toward 2

final Trials<Integer> negatives = 
    positives.map(x -> -x);   // Maps to negative
```

When `negatives` shrinks, it actually shrinks **the source**:
- Source: 500 → 250 → 125 → ... → 2
- After mapping: -500 → -250 → -125 → ... → **-2**

The shrinkage target is still **2 in the source domain**. The mapping just transforms whatever shrunk value we get.

If you want to shrink directly toward a negative value:
```java
// This shrinks toward -2 directly
final Trials<Integer> negatives = 
    api().integers(-1000, -1, -2);
```

---

{: .note-title }
> Key Takeaways
>
> - **Two types of shrinkage:** Distance (toward targets) and Complexity (simpler structures)
> - **Distance shrinkage** - Scalars shrink toward zero by default, or custom targets
> - **Complexity shrinkage** - Fewer elements, shallower nesting, fewer decisions
> - **Mapping preserves shrinkage** but applies to source values
> - **`.choose()` doesn't shrink** - all choices equally valid
> - **Americium tracks construction** - knows how test cases were built
> - **Both aspects work together** - complexity and distance shrink simultaneously
> - **Higher limits = better shrinkage** - more opportunities to find minimal cases
> - **No guarantee of absolute minimum** - heuristic search for "maximally shrunk" cases
