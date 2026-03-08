---
layout: default
title: Multi-Parameter Tests
parent: Core Concepts
nav_order: 1
---

# Multi-Parameter Tests
{: .no_toc }

Using `.and()` to supply multiple independent test case streams
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Problem

So far we've examined tests that take just one test case parameter; in the examples, we've managed to make do with a single parameter, squashing together several pieces of information using a collection or a custom class instance.

However, a test might have a call signature that takes several parameter values, and it may not always be appropriate to aggregate those into a single parameter. **What if we want to supply independently varying streams of test cases to each parameter?**

---

## The Solution: `.and()`

Americium can gang together several trials instances to supply test cases to a test taking more than one test case as a parameter:
```java
import static com.sageserpent.americium.java.Trials.api;

final Trials<ImmutableList<Long>> lists = api().longs().immutableLists();
final Trials<Long> longs = api().longs();

lists
    .and(longs)
    .and(lists)
    .withLimit(10)
    .supplyTo((ImmutableList<Long> leftHandList,
               Long additionalLongToSearchFor,
               ImmutableList<Long> rightHandList) -> {
        System.out.format("Left: %s, long: %s, right: %s\n",
                         leftHandList,
                         additionalLongToSearchFor,
                         rightHandList);
    });
```

This prints:
```
Left: [], long: 2820552734719595162, right: []
Left: [], long: -1955330156, right: [-7084775193041423908]
Left: [-8862448238275326996], long: 2128104420, right: []
Left: [6125466203612653216], long: 1531869968, right: []
Left: [-8862448238275326996, 2820552734719595162], 
      long: 637157859, right: []
...
```

See how we've **ganged together three trials** and supplied test cases to three independent parameters, each one varying according to the specification for its corresponding trials instance.

---

## How It Works

The use of `.and` on a trials instance takes the same generic `Trials` instance interface and yields a generic interface `TrialsScaffolding.FlatteningSyntax`.

This in turn has an `.and` method - and the process repeats. So we can gang together **up to 4 elementary trials** to supply their respective test cases to test parameters that may be unpacked tuples themselves.

{: .note }
> **Important:** The same trials instance can be used **multiple times** in the gang - but this does **not** mean the same test cases will be supplied to the respective parameters!
>
> When we say `.and(longs)`, we're saying we want test cases *of that kind* - the trials instance is a **specification**, not a sequence. Each occurrence will yield independent test cases.

---

## Independent Streams Example

Let's clarify this with an example:
```java
final Trials<Long> longs = api().longs();

longs
    .and(longs)  // Same instance, but independent stream!
    .withLimit(5)
    .supplyTo((Long first, Long second) -> {
        System.out.format("First: %d, Second: %d, Equal? %s\n",
                         first, second, first.equals(second));
    });
```

Output:
```
First: 2820552734719595162, Second: -1955330156, Equal? false
First: 2128104420, Second: 1531869968, Equal? false
First: 637157859, Second: 1806495736, Equal? false
First: 1987104774, Second: 1840758034, Equal? false
First: 1840758034, Second: 203172046, Equal? false
```

Even though we used the same `longs` trials instance twice, **the values are different** for each parameter. They're independent streams drawn from the same specification.

---

## Practical Example: Set Membership Test

### System Under Test

Here's a real-world example testing a (deliberately buggy) set membership predicate:

```java
class PoorQualitySetMembershipPredicate<Element extends Comparable<Element>> implements Predicate<Element> {
    private final Comparable[] elements;

    public PoorQualitySetMembershipPredicate(Collection<Element> elements) {
        this.elements = elements.toArray(Comparable[]::new);
    }

    @Override
    public boolean test(Element element) {
        return 0 <= Arrays.binarySearch(elements, element);
    }
}
```

### Test Approach

This test builds a list from three **independently varying** parts:
1. A left-hand list
2. A specific long we'll search for
3. A right-hand list

Then it verifies that the combined list contains the middle element. The bug in `PoorQualitySetMembershipPredicate` will cause failures that Americium will shrink nicely.

```java
final Trials<ImmutableList<Long>> lists = api().longs().immutableLists();
final Trials<Long> longs = api().longs();

lists
    .and(longs)
    .and(lists)
    .withLimit(10)
    .supplyTo((ImmutableList<Long> leftHandList,
               Long additionalLongToSearchFor,
               ImmutableList<Long> rightHandList) -> {
        
        final Predicate<Long> systemUnderTest =
            new PoorQualitySetMembershipPredicate(
                ImmutableList.builder()
                    .addAll(leftHandList)
                    .add(additionalLongToSearchFor)
                    .addAll(rightHandList)
                    .build());

        assertThat(systemUnderTest.test(additionalLongToSearchFor), 
                   is(true));
    });
```

### Test Verdict

```
Exception in thread "main" Trial exception with underlying cause:
java.lang.AssertionError: 
Expected: is <true>
     but: was <false>
Provoked by test case:
[[1],0,[]]
```

Ah, yes - we didn't sort the contents of the array `PoorQualitySetMembershipPredicate.elements` in the constructor; the subsequent binary search will only work if the elements are already sorted. Let's fix it:

```java
class AwesomeSetMembershipPredicate<Element extends Comparable<Element>> implements Predicate<Element> {
    private final Comparable[] elements;

    public AwesomeSetMembershipPredicate(Collection<Element> elements) {
        this.elements = elements.toArray(Comparable[]::new);

        Arrays.sort(this.elements, Comparator.naturalOrder()); /* <<----- FIX */
    }

    @Override
    public boolean test(Element element) {
        return 0 <= Arrays.binarySearch(elements, element);
    }
}
```

---

## Maximum Arity

Americium supports ganging together **up to 4 elementary trials** before unpacking tuples. So you could theoretically have:
```java
trials1.and(trials2).and(trials3).and(trials4).supplyTo(...)
```

---

{: .note-title }
> Key Takeaways
>
> - **`.and()`** gangs multiple trials to supply independent test case streams
> - Using the same trials instance multiple times creates **independent streams**, not identical values
> - Supports **up to 4 elementary trials** in a gang
> - Each parameter varies independently according to its trials specification
> - Perfect for tests that need multiple unrelated inputs
