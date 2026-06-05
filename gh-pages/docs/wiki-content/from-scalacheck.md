---
layout: default
title: "Arrived from Scalacheck?"
parent: Wiki Content
nav_order: 12
---

# Arrived from Scalacheck?
{: .no_toc }

Welcome to Americium - learn the local language
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Come on in and take the weight off your feet, we're friendly here. Like a nice refreshing glass of integrated shrinkage cordial?

If you want to speak Americium it's expected that you'll do so with a Scalacheck accent - here's a guide to translation:

### `Arbitrary` and `Gen`

First off, _there is no `Arbitrary` analogue_. `Gen[+T]` becomes `Trials[+T]`. Easy.

(Actually, there is something called `Factory` specifically for auto-derivation; that is a bit like `Arbitrary`. We'll get to that in a bit...)

### Getting a `Gen`

This is nearly always explicit (we'll cover auto-derivation in a bit).

Instead of using the companion object `Gen`, either import `Trials.api` or use a method defined on the `Trials[T]` instance itself...

* `Gen.choose`, `Gen.chooseNum`, `Gen.chooseChar`, `Gen.chooseBigInt` etc becomes one of `api.integers`, `api.doubles`, `api.characters`, `api.bigInts` etc, using the overloads that take lower and upper bounds.
* `Gen.double` becomes `api.doubles`, using the overload without any parameters.
* `Gen.long` becomes `api.longs`, using the overload without any parameters.
* `Gen.oneOf` becomes either `api.choose` or `api.alternate`, depending on whether you originally fed in `T` or `Gen[T]`.
* `Gen.const` becomes `api.only`
* `Gen.delay` becomes `api.delay`
* `Gen.frequency` becomes `api.chooseWithWeights`
* `Gen.failed` becomes `api.impossible`
* `Gen.sequence` becomes `api.sequences`
* `Gen.option` becomes `<trials instance>.options`
* `Gen.either` becomes `<trials instance>.or`
* `Gen.stringOf` becomes `<character trials instance>.several` (Calling `several` on a character trials will build string trials by default.)
* `Gen.stringOfN` becomes `<character trials instance>.lotsOfSize` (Calling `lotsOfSize` on a character trials will build string trials by default.)
* `Gen.listOf` becomes `<trials instance>.lists`
* `Gen.listOfN` becomes `<trials instance>.listsOfSize`
* `Gen.containerOf` becomes `<trials instance>.several`
* `Gen.containerOfN` becomes `<trials instance>.lotsOfSize`

**NOTE**: the methods in `TrialsApi` often have names in the plural - so `Gen.double` becomes `api.doubles`, `Gen.sequence` becomes `api.sequences` and so on.

### `Gen` as a monad

`Trials` is a monad too, with a stack-safe implementation. It has a typeclass instance for Cats' `Monad`. So you can `map`, `flatMap`, `filter` and `mapFilter` till you drop. There is even `withFilter` so you can enjoy the full-fat for-comprehension flavour.

### Plugging in test lambdas

Throw away `Prop.forAll` and call `<trials instance>.withLimit(<limit>).supplyTo` instead.

If you have several `Gen` instances, then gang together the corresponding trials with `.and`:

`(<trials one> and <trials two> and <trials three>).withLimit(<limit>).supplyTo`.

### `Test.Parameters`

Substitute for `Test.Parameters.withMinSuccessfulTests` and `Test.Parameters.withMaxDiscardRatio` with `<trials instance>.withStrategy(_ => CasesLimitStrategy.counted(<maximumNumberOfCases>, <maximumStarvationRatio>))`.

Substitute for `Test.Parameters.withInitialSeed` with `<supply-to syntax>.withSeed`, where `<supplyToSyntax>` is `<trials instance>.withLimit(<limit>)` etc.

Substitute for `Test.Parameters.withMaxSize` with `<supply-to syntax>.withComplexityLimit`, where `<supplyToSyntax>` is `<trials instance>.withLimit(<limit>)` etc.

### Sized generators

Substitute `Gen.size` with `api.complexities`. This will reveal the appropriate complexity to your own code that builds up trials. Typically this is done in a flat-map: `api.complexities.flatMap(complexity => <trials expression using the complexity>)`.

### Custom `Shrink` instances

Delete them and dance a jig!

### Scalacheck-Shapeless

Your auto-derivation needs are met by `com.sageserpent.americium.Factory`, which uses the marvellous Magnolia library under the hood.

If you have a case class hierarchy rooted at `Root` and you want `Trials<Root>`, then pull in an implicitly derived instance via:

Scala 2.13

```scala
implicitly[Factory[Root]].trials
```

Scala 3

```scala
given evidence: Factory[Root] = Factory.autoDerived // May need this if `Root` has a recursive definition like, say, `List[T]`.

implicitly[Factory[Root]].trials
```

As with Scalacheck-Shapeless, you will probably need to supply a few of your own implicit definitions to bootstrap the auto-derivation, it's the same drill only this time it is evidences for various `Factory[T]` instantiations that you need to supply.

As a guide, here are the built-in implicit definitions for [Scala 2.13](https://raw.githubusercontent.com/sageserpent-open/americium/c53549899b4929c1403bad4cb79d3b3e7af75f94/src/main/scala-2.13/com/sageserpent/americium/Factory.scala#L21) and [Scala 3](https://raw.githubusercontent.com/sageserpent-open/americium/c53549899b4929c1403bad4cb79d3b3e7af75f94/src/main/scala-3/com/sageserpent/americium/Factory.scala#L18).

***
Next topic: [JUnit5 again...]({% link docs/wiki-content/junit5-typed-tests.md %})