---
layout: default
title: "All about shrinkage"
parent: Wiki Content
nav_order: 6
---

# All about shrinkage
{: .no_toc }

What it means and how it is achieved
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


We've mentioned shrinkage of test cases that fail their trials, and we know that Americium carries on trialing test cases until it settles on a maximally shrunk test case, at which point it lets the exception out, wrapped up inside a `TrialsFactoring.TrialsException`. So far, the examples of shrunk test cases look sensible enough, and they could be seen to improve when Americium is given a better budget via `.withLimit` - but what does shrinkage really mean?

There are two aspects to shrinking a test case:
1. The _distance_ of a scalar value from some choice of a maximally shrunk value.
2. The _complexity_ of a structured test case.

## Distance Shrinkage

An example of the distance aspect can be seen with:

```java
try {
    api().doubles(-1e10, 1e10).withLimit(50).supplyTo(input -> {
        double root = Math.sqrt(input);
        try {
            assertThat(Double.isNaN(root), is(false));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

We're peeking at the failing cases in the test prior to Americium throwing the final `TrialException`:

```
-9.105215576676373E9
-8.6881081547096E9
-2.1320432384270747E9
-1.0794530527479506E9
-3.933015970732623E8
-9.183900331240854E7
-4.5931761134104E7
-3.508276029329806E7
-5021004.9676793255
-1696895.33830504
-1004189.6789330263
-571909.6722478328
-404243.60335425945
-93369.2146621451
-83588.7870024568
-13330.019412140715
-8695.34222429553
-6065.403802764423
-1166.2699171308914
-1127.3063430503917
-303.38285352351534
-188.64786072462712
-72.06401439886594
-13.348024122637748
-7.339791923115929
-4.9339153455115605
-1.2664547004419962
-0.8866716952674047
-0.35113861037577854
-0.15060597083685345
-0.05695786073692255
-0.002145267470610168
-0.002023332673281586
-9.146492157413588E-4
-6.367410938790119E-5
-5.2134945666137966E-5
-3.5475095083725705E-6
-2.0111950299606107E-6
-3.361026734705064E-7
-2.0816681711721685E-7
-8.998878031629687E-8
-1.0842021724855044E-8
-9.75781955236954E-9
-3.2526065174565133E-9
-1.0842021724855044E-9
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: is <false>
     but: was <true>
Case:
-1.0842021724855044E-9
Reproduce via Java property:
trials.recipeHash=a7e81f2abb74d603481ca51f5580e4cb
Reproduce via Java property:
trials.recipe="[{\"FactoryInputOf\":{\"input\":-1}}]"
```

Observe how the initial failure with a very unwieldy -9.105215576676373E9 is whittled down in _magnitude_ to a more tractable -1.0842021724855044E-9. By default, shrinkage of a numeric value will tend to zero - in this case zero would not have caused a failure, so Americium finished off with a negative number that was fairly close.

The target scalar value doesn't have to zero - we can set our own non-zero value:

```java
try {
    api().doubles(-1e10, 1e10, -152.753).withLimit(50).supplyTo(input -> {
        double root = Math.sqrt(input);
        try {
            assertThat(Double.isNaN(root), is(false));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

This yields:

```
-9.105215576676373E9
-8.6881081547096E9
-2.1320433282770753E9
-1.079453179597778E9
-3.9330173915945214E8
-9.183915167286403E7
-4.59319120782739E7
-3.508291230142994E7
-5021157.413945983
-1697047.9649936275
-1004342.3799185539
-572062.4038285059
-404396.34753387794
-93521.9640299518
-83741.53850673593
-13482.771796209798
-8848.094970657274
-6218.156698316722
-1319.0228741205913
-1280.059325338865
-456.13584622900316
-341.40085772138707
-123.19060478808785
-138.84206515021668
-145.96222640876596
-149.37160509742213
-152.21259671581245
-153.13683395311975
-152.72072156768584
-152.7329599225466
-152.7498341556772
-152.75514526747062
-152.752552542174
-152.75274498697542
-152.7529116245125
-152.75297018877706
-152.75301079756943
-152.75300884058453
-152.75299534660428
-152.7530002471981
-152.75300014094628
-152.75299986122212
-152.7530000097578
-152.7530000065052
-152.752999994579
-152.7529999978316
-152.753
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: is <false>
     but: was <true>
Case:
-152.753
Reproduce via Java property:
trials.recipeHash=2f4aad6ae7b927bc9948ea3bd0361a64
Reproduce via Java property:
trials.recipe="[{\"FactoryInputOf\":{\"input\":-140889774875}}]"
```

In this case shrinkage actually resulted in the target being the maximally shrunk test case, as -152.753 is negative itself.

Characters can be shrunk in this way too:

```java
try {
    api().characters('A', 'z', 'O').withLimit(50).supplyTo(input -> {
        try {
            assertThat(input, lessThan('L'));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

This yields:


```
h
f
`
^
]
O
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: a value less than "L"
     but: "O" was greater than "L"
Case:
O
Reproduce via Java property:
trials.recipeHash=3c74a6051f7644c509f231bb81a28609
Reproduce via Java property:
trials.recipe="[{\"FactoryInputOf\":{\"input\":79}}]"
```

There is a wrinkle - characters can be shrunken towards an explicit target, but the other overloads of the `TrialsApi.characters` do not permit shrinkage of their test cases, because there is no obvious default to shrink to. In the same way, the single declaration of `TrialsApi.booleans` doesn't shrink either - true and false are considered to be equally as good.

In fact, any trials instance that is built using `TrialsApi.choose` does **not** allow shrinkage of its test cases. The reason is that a choice is considered to be selection from a set of equally valid possibilities, which don't even have to be numeric. Consider an enumeration type - which enumeration value would we consider to be 'the smallest'?

Transforming a trials that is shrinkable yields another shrinkable trials, but sticks with applying shrinkage to the source trials:

```java
try {
    api().doubles(-1e10, 1e10, -152.753).map(value -> -value).withLimit(50).supplyTo(input -> {
        double root = Math.sqrt(input);
        try {
            assertThat(Double.isNaN(root), is(false));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

We negate the values, so shrinkage now has to move _positive_ values in source trials in the direction of the target, -152.753. The failure won't occur when the source test values cross over zero, so we see:

```
-3.71491909402933E9
-2.966997489796238E9
-5.358398565224385E8
-4.998735887849297E8
-4.8883307204074246E8
-2.7205589646452326E7
-1.5053735949196579E7
-7771300.103947113
-804220.953759997
-446390.6461383621
-278230.77159202314
-172681.77025061185
-27150.892720295164
-9218.593390984895
-5188.114552733808
-1698.1378875307025
-471.58899564324366
-427.2261595671299
-391.9060095722816
-240.27913820334783
-214.6391853710165
-38.847600078648334
-9.375772407182584
-5.03110396706348
-3.5805135683959626
-0.6183243982281343
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: is <false>
     but: was <true>
Case:
-0.6183243982281343
Reproduce via Java property:
trials.recipeHash=835bd79ac80e0936cfa78465e83baa72
Reproduce via Java property:
trials.recipe="[{\"FactoryInputOf\":{\"input\":570303596}}]"
```

Americium cares about the manner in which a test case is built up, not the actual test case itself, so shrinkage is applied to the raw material - namely the value produced by the source test case.

## Complexity Shrinkage

Here's an amusing exercise:

```java
try {
    final String suffix = "are";

    final int suffixLength = suffix.length();

    api().characters('a', 'z').collections(Builder::stringBuilder).filter(caze -> caze.length() >
                                                                                  suffixLength).withLimit(20000).supplyTo(input -> {
        try {
            assertThat(input, not(endsWith(suffix)));
        } catch (Throwable throwable) {
            System.out.println(input);
            throw throwable;
        }
    });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

We're looking for 'words' that end in the suffix "are" but must be longer than the suffix itself.

The result is appropriate, given how many combinations of letters there are:

```
qzqiare
rare
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: not a string ending with "are"
     but: was "rare"
Case:
rare
Reproduce via Java property:
trials.recipeHash=a231ff98839a0b111a5b4fa213c68fc6
Reproduce via Java property:
trials.recipe="[{\"ChoiceOf\":{\"index\":1}},{\"ChoiceOf\":{\"index\":4}},{\"ChoiceOf\":{\"index\":1}},{\"ChoiceOf\":{\"index\":17}},{\"ChoiceOf\":{\"index\":1}},{\"ChoiceOf\":{\"index\":0}},{\"ChoiceOf\":{\"index\":1}},{\"ChoiceOf\":{\"index\":17}},{\"ChoiceOf\":{\"index\":0}}]"
```

Entertaining coincidence aside, note how shrinkage proceeded from "qzqiare" down to "rare". In this situation, the trials providing the characters will not shrink them as it uses a choice over a character range under the hood. What does happen though is that the strings yielded by the overall trials expression are shrunk down in length - they become less complex.

To reiterate, Americium cares about the manner in which a test case is built up, not the actual test case itself, and here it keeps track of how many degrees of freedom of variation the final test cases has - this is what we mean by complexity. The subexpression `api().characters('a', 'z').collections(Builder::stringBuilder)` is building a string - a kind of collection - whose elements are taken from repeated usages of the same trials instance for characters, namely the `api().characters('a', 'z')` part - each repetition adds another degree of freedom of variation, pushing up the complexity. So a 7-letter string like "qzqiare" will be more complex than a 4-letter string like "rare".

Americium tries to reduce complexity of its failing test cases, and will do so in tandem with distance shrinkage on the components of the overall final test case.

Let's see that in action:

```java
try {
    api()
            .integers(1, 20)
            .flatMap(api().integers(0,
                                    Integer.MAX_VALUE)::immutableListsOfSize)
            .withLimit(15)
            .supplyTo(input -> {
                try {
                    final int sum =
                            input.stream().reduce(Integer::sum).orElse(0);
                    assertThat(sum, not(lessThan(0)));
                } catch (Throwable throwable) {
                    System.out.println(input);
                    throw throwable;
                }
            });
} catch (TrialsScaffolding.TrialException exception) {
    System.out.println(exception);
}
```

So one degree of freedom controls the length of the lists that are reduced, and there are degrees of freedom for each trials that contributed an integer to the overall list test case:

```
[805257284, 301232032, 657251477, 730815669, 1244866410]
[1116753496, 1903765060, 156227821]
[635805210, 70022358, 1813976772]
[2017271885, 1302899322]
[1942017441, 447615689]
Trial exception with underlying cause:
java.lang.AssertionError:
Expected: not a value less than <0>
     but: was <-1905334166>
Case:
[1942017441, 447615689]
Reproduce via Java property:
trials.recipeHash=f94c17dd67adc49fa57b2467a233229e
Reproduce via Java property:
trials.recipe="[{\"FactoryInputOf\":{\"input\":2}},{\"FactoryInputOf\":{\"input\":447615689}},{\"FactoryInputOf\":{\"input\":1942017441}}]"
```

See how the lists contract in size, but there is shrinkage applied to the individual values too. This example is a tricky one, as the shorter the list test cases get, the larger the individual list elements have to be to cause overflow - so shrinkage has to pick its way between just shrinking the distances of all the elements from zero with shrinking the complexity.

It is possible to customise how a trials generates a scalar value and take more control over the lower bound, upper bound and shrinkage target - but we'll leave that for a later topic.

***
Next topic: [Configuration buttons, dials and levers...]({% link docs/wiki-content/configuration.md %})