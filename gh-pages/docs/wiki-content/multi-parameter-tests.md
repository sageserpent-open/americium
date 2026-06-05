---
layout: default
title: "Multi-parameter tests"
parent: Wiki Content
nav_order: 4
---

# Multi-parameter tests
{: .no_toc }

Supplying independently varying test cases to a trial
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Consider this wondrous artefact:

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

Let's test it:

```java
final Trials<ImmutableList<Long>> lists =
        Trials.api().longs().immutableLists();

final Trials<Long> longs = Trials.api().longs();

lists
        .and(longs)
        .and(lists)
        .withLimit(10)
        .supplyTo((leftHandList, additionalLongToSearchFor,
                   rightHandList) -> {
            final Predicate<Long> systemUnderTest =
                    new PoorQualitySetMembershipPredicate(ImmutableList
                                                                  .builder()
                                                                  .addAll(leftHandList)
                                                                  .add(additionalLongToSearchFor)
                                                                  .addAll(rightHandList)
                                                                  .build());

            assertThat(systemUnderTest.test(additionalLongToSearchFor),
                       is(true));
        });
```

What we're doing here is to supply our test with a test case that is divided into three independently varying inputs - we have a left hand list of integers, a long value that we expect to find and a right hand list of integers. We surround the value we want to find by the two lists, and as these lists vary, so will the system under test - sometimes one or both of these will be empty, so we cover the cases where we are searching for a leading, trailing or singleton element too.

To supply the three inputs, we take three trials instances and gang them together using the `.and` combinator method - this builds a specialised form of a trials object that has custom overloads of `.supplyTo` for tests that either take the corresponding number of arguments, or a single tuple that has the arguments as components.

You may have spotted that `lists` occurs in two places in the gang - so does this mean that the parameters `leftHandList` and `rightHandList` will be the same for every trial? **No** - Americium does not treat trials instances as canned sequences of values, rather as specifications for what kind of values are produced. So when a trial is run, Americium is free to vary the first and third inputs _independently_ of each other: they will produce the same kind of data, namely lists of long values, but in general those values will not coincide in a given trial - although they might every now and then.

Currently up to four elementary trials can be ganged together via `.and`.

What happens when we run the test?

```
java.lang.AssertionError:
Expected: is <true>
     but: was <false>
Case:
[[1],0,[]]
```

Ah yes - we didn't sort the contents of the array `PoorQualitySetMembershipPredicate.elements` in the constructor, so the subsequent binary search will only work if the elements are already sorted. Let's fix it:

```java
class AwesomeSetMembershipPredicate<Element extends Comparable<Element>> implements Predicate<Element> {
    private final Comparable[] elements;

    public AwesomeSetMembershipPredicate(Collection<Element> elements) {
        this.elements = elements.toArray(Comparable[]::new);

        Arrays.sort(this.elements, Comparator.naturalOrder());
    }

    @Override
    public boolean test(Element element) {
        return 0 <= Arrays.binarySearch(elements, element);
    }
}
```

That passes the test nicely.

***
Next topic: [Reproducing a failing test case quickly...]({% link docs/wiki-content/reproducing-failures.md %})