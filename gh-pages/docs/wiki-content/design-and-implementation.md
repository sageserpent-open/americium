---
layout: default
title: "Design and Implementation"
parent: Wiki Content
nav_order: 14
---

# Design and Implementation
{: .no_toc }

Yes, do pay attention to the man behind the curtain
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Congratulations, you have reached the final topic - by now you are a seasoned expert in the practice of the Americium way.

May you design by contract, and all your preconditions be met - that is your invariant.

Perhaps you'd like to work on this project yourself? Let's have a chat about what's going on under the hood...

## Building, Directory Layout and Releases

Americium is built as an SBT project, using cross-platform support to target Scala 2.13 and Scala 3. The [gha-scala-library-release-workflow](https://github.com/guardian/gha-scala-library-release-workflow) is used to publish a library JAR to Sonatype, running as a manually-triggered CI action on GitHub.

This workflow drives the `release` task in SBT, supplied by the plugin `sbt-release`. As mentioned in [`build.sbt`](https://raw.githubusercontent.com/sageserpent-open/americium/ad4d2f3d09f527441973d262c18b66e86631f124/build.sbt#L184), the support for cross-building from `sbt-release` is disabled; SBT's built-in cross-building is used instead.

Sources are organised in typical Maven / SBT directory roots - thus published code sits in `./src/main/scala` for cross-platform shared Scala sources, `./src/main/scala-2.13` for sources specific to Scala 2.13, `./src/main/scala-3`for sources specific to Scala 3 and `./src/main/java` for Java code.

There is a similar hierarchy under `.src/test`.

## Artifacts (From 2.0.0)

Americium is published as three separate artifacts:

### 1. `americium` (Core Framework)
The core framework for generating test cases with integrated shrinkage.

**Packages:**
- `com.sageserpent.americium` - Scala client APIs, API implementations and their tests
- `com.sageserpent.americium.java` - Java client APIs, forwarding implementations and additional tests
- `com.sageserpent.americium.examples` - Example tests (Scala)
- `com.sageserpent.americium.java.examples` - Example tests (Java)
- `com.sageserpent.americium.generation` - Low-level support for test case generation
- `com.sageserpent.americium.storage` - Storage for recipe reproduction

### 2. `americium-junit5` (JUnit5 Integration)
Deep integration with JUnit5, allowing individual replay of test cases.

**Packages:**
- `com.sageserpent.americium.junit5` - tight JUnit5 integration via `dynamicTests` (Scala)
- `com.sageserpent.americium.junit5.java` - loose JUnit5 integration via annotations and tight integration via `JUnit5` (Java)
- `com.sageserpent.americium.junit5.storage` - Storage for JUnit5 replay

### 3. `americium-utilities` (Utility Libraries)
Handy utilities used by Americium that may be useful outside a testing context.

**Packages:**
- `com.sageserpent.americium.utilities` - General utilities

**NOTE:** there is an asymmetry in that the Scala client APIs do not have a `.scala` sub-package, they were for historical reasons taken to be default as they were implemented first.

**NOTE:** sources in the `.java` sub-package may be written in Java or Scala, even though they target Java clients.

Releases are triggered manually off branch `master` via GitHub and after each release is published to Sonatype, the branch `checkTheLibrary` takes a merge *from* `master`, deleting all non-test sources in the merge and updating its version of `built.sbt` to reference the published version of Americium. This allows the same test suite to be used to validate the published artefacts. Branch `checkTheLibrary` should *never* be merged back into `master`, as that would delete all of the non-test sources - it plays constant catch-up with `master` instead.

## `TrialsApi`

There are parallel Scala and Java implementations of the Scala and Java forms of `TrialsApi`. The Java implementation simply delegates to the Scala API, using things like `Int.(un)box` to negotiate the gap between Scala's idea of a generic whose type parameter is a primitive type (`Int`) and Java's idea of all generic type parameters being reference types (thus `Integer`).

Around the codebase in general you will also see use of the Java <-> Scala collection conversions in `scala.collection.convert`, both to convert a Java iterable or collection passed as an argument to the Java API to the Scala equivalent and in reverse where the test case type is a collection.

The ultimate source of the API implementation objects is `TrialsApis`. As these objects are immutable, they are held as simple Scala val bindings and reused via calls to `Trials.api`. Note how the definition of `TrialsApis.javaApi` is tied to that of `TrialsApis.scalaApi` in a [forwarding implementation](https://raw.githubusercontent.com/sageserpent-open/americium/aab8e67bb2404f4eaadac5adf8d2303c59f7e38c/src/main/scala/com/sageserpent/americium/TrialsApis.scala#L7).

The lowest level implementations of the Scala API mostly construct instances of `TrialsImplementation`, we'll come back to this later.

In the meantime, observe how several of the methods in the Scala implementation use `.stream`, passing a custom `CaseFactory` to the call; let's talk about this next...

## `CaseFactory`

In its Scala guise:

```scala
trait CaseFactory[+Case] {
  def apply(input: BigInt): Case

  def lowerBoundInput: BigInt

  def upperBoundInput: BigInt

  def maximallyShrunkInput: BigInt
}
```

This underpins the 'streaming' methods in `TrialsApi`, used to _generate_ test cases over some domain (contrast with `.choose` that picks from test cases given explicitly as a collection by the client code).

`CaseFactory` has the notion of an output range over `Case`, which contains all the possible test cases that could be generated. The particular test case generated (via `.apply`) is controlled by an input domain over `BigInt`. The input domain is the entire interval of integers between `.lowerBoundInput` and `.upperBoundInput` - typically, the actual test case type of `Case` has a total ordering, say a numeric type; we expect `.apply` to be a monotonic increasing function of `BigInt` and the value of `.apply(maximallyShrunkInput)` to be the maximally shrunk value of the test case type.

As Americium can consult the bounds of the input domain as well as `.maximallyShrunkInput` (which admittedly is a slightly confusing name - it is not the input that is maximally shrunk, rather the input that yields the maximally shrunk `Case`), then it can drive the case factory correctly to generate test cases.

Observe the implementation of `TrialsApi.bytes`:

```scala
  override def bytes: TrialsImplementation[Byte] =
    stream(new CaseFactory[Byte] {
      override def apply(input: BigInt): Byte   = input.toByte
      override def lowerBoundInput: BigInt      = Byte.MinValue
      override def upperBoundInput: BigInt      = Byte.MaxValue
      override def maximallyShrunkInput: BigInt = 0
    })
```

See how in this case there is a simple narrowing conversion from domain `BigInt` to range `Byte`, and so the input domain bounds are chosen according to the bounds of the output range, `Byte`.

What happens when we have the integral domain of `BigInt` being transformed into an output range of something like `BigDecimal` or `Double`, that while technically is a countable set, is still approximately an Archimedean field, so its values can get much closer together than 1?

Observe the implementation of [`TrialsApi.bigDecimals`](https://raw.githubusercontent.com/sageserpent-open/americium/42fb2263d0fe331a10d9788706bbbcb53b2b194b/src/main/scala/com/sageserpent/americium/TrialsApiImplementation.scala#L231). This takes the approach of using choosing either to stick with the interval `[Long.MinValue; Long.MaxValue]` if this can be easily interpolated to the actual bounds of the _test case range_ type passed to the method while still retaining a reasonable degree of precision. It it can't achieve at least a precision of `numberOfSubdivisionsOfDoubleUnity`, then the input domain is expanded beyond that of `Long` to give the desired minimum precision.

There is some fancy footwork going on, in that the bounds and `.maximallyShrunkInput` have to transform to exactly the corresponding test case values. To achieve this we split the interpolation into two halves, one from the lower bound to the maximally shrunk value, the other from the maximally shrunk value to the upper bound, so we can ensure that the anchor points of the interpolation hit the desired test case values spot on.

## `Trials`

There are conversions between the two flavours of `Trials` - both APIs have a conversion method that yields a corresponding interface or trait in the other language. This actually yields the same object as `this`, only the nominal interface / trait type changes to reflect the crossover to the other language. Switching back and forth between the Java and Scala forms of `Trials` takes place in both the implementations of the Java forms of `TrialsApi` and `Trials`.

Ultimately the implementation of both flavours of `Trials` is realised by two skeleton implementation traits specific to each flavour (`TrialsSkeletalImplementation`) and a concrete implementation class, `TrialsImplementation` that extends both of the skeleton traits. The skeleton traits house implementation boilerplate; the Java skeleton delegates to the Scala form of `Trials` via the aforementioned conversion methods, whereas the Scala skeleton is implemented as a combination of wrapping of `Generation` within `TrialsImplementation` and delegation to some stray methods in `TrialsImplementation`.

As an aside, the skeleton traits are a [bit messy](https://github.com/sageserpent-open/americium/issues/60), but they do avoid sprawl in `TrialsImplementation`, which is pretty large in its own right. They also partition the overrides for the Scala and Java forms of `Trials` nicely where they have differing type signatures; those overrides left in `TrialsImplementation` are mostly common to both forms - see `.withLimit` and `.withStrategy` for example.

`TrialsImplementation` is really a facade; it keeps hold of an instance of `Generation` which is the underlying state used to formulate test cases, sharing the generation with instances implementing `SupplyToSyntax` that it creates in calls to `.withStrategy`, `.withLimit(s)` (indirectly by delegation to `.withStrategy`) and `.withRecipe`.

## `Generation` and its two Interpreters.

`Trials` is a monadic type supporting mapping, flat-mapping and filtering. As far the clients of Americium are concerned, it offers the usual monadic composition approach, has lots of magic factory methods to make instances of it out of thin air (these are what `TrialsApi` offers) and then supplies test cases via conversion to `SupplyToSyntax`. In the case of calling `.withLimits(s)` and `.withStrategy`, potentially many different test cases can be produced, whereas for `.withRecipe` only one specific test case can ever be produced.

The difference in behaviour for the two routes to supplying test cases is implemented by a free monad, `Generation` and its two interpreters. Using a free monad supports all of the API monadic operations using a straightforward delegation approach from the Scala skeleton implementation of `Trials`, wrapping the resulting instances of `Generation` back up in a `TrialsImplementation`.

Having `Generation` distinct from `Trials` or its implementations is necessary, as `Generation` is simply a type definition around the Cats `Free` class hierarchy; the latter cannot be amended to implement `Trials` as an interface, not can it have additional methods added to it to support the complete set of methods in `Trials` over and above the monadic ones.

So there are two levels of delegation going on: a Java trials instance delegates to a Scala trials instance, and the Scala trials instance delegates to an instance of `Generation`.

In itself, a free monad serves only as a framework that provides boilerplate for the monadic operations and a way of integrating in 'magic' instances of the monad that _denote_ behaviour that gives the monad implementation its special flavour. By itself, it doesn't do anything other than allow monadic composition - all you get at the end is just another free monad instance. To make the magic happen - so in this situation, to supply test cases - we have to provide an interpreter that understands what those magic instances mean and make something of them.

Let's break this down - what are these these magic free monad instances? How do the interpreters work?

Look at [how a choice](https://raw.githubusercontent.com/sageserpent-open/americium/d0629045f87023a6cd6b7eaa204efb48f3dc9f2c/src/main/scala/com/sageserpent/americium/TrialsApiImplementation.scala#L66) is implemented by the Scala form of `TrialsApi` - an instance of `TrialsImplementation` is created by an auxiliary constructor from a `GenerationOperation`, here it is a `Choice`. Each case class implementing `GenerationOperation` denotes some kind of magic - in this example, the choosing of a test case from a set of choices. The auxiliary constructor then lifts the `GenerationOperation` into an instance of `Generation` as per the free monad cookbook and thus provides the magical instance.

You will see a bunch of these [generation operations](https://raw.githubusercontent.com/sageserpent-open/americium/d0629045f87023a6cd6b7eaa204efb48f3dc9f2c/src/main/scala/com/sageserpent/americium/generation/GenerationOperation.scala#L7), note that in themselves they are passive - they denote something magical and provide the information needed to perform that magic, so choices of data, or a case factory or whatever, but there is no implementation detail. That job falls to the interpreters...

**NOTE**: if at this point you are wandering what a 'free monad', 'lifting' and 'interpreters', then take a look [at the Cats documentation](https://typelevel.org/cats/datatypes/freemonad.html). There are also some good explanations of free monads in various blog posts, like [this one](https://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html) for example.

**NOTE**: if you're puzzling over the adjective 'magic' in connection with monadic instances, think of the difference between, say a Scala `Option` built via `1.pure[Option]` (using Cats syntax here - this is really `Some(1)` under the hood) and `None`. The first value is an option built via the `.pure` lifting operation, and is just an ordinary value wrapped up into an `Option` - the second is magical, as it denotes the absence of a payload and propagates through a series of maps, flat-maps and filters.

In the same way, we can make a non-magical trials: `1.pure[Trials]`, this is really `TrialsApi.only(1)` under the hood. It is just a value wrapped up in a trials which will end up supplying precisely that value and nothing else - so no shrinkage either, and no complexity cost. On the other hand, `TrialsApi.integers` will supply randomly varying integers that permit shrinkage and count to the overall complexity metric, hence this is magical; similarly `TrialsApi.choose` and `TrialsApi.impossible` are also magical, the last of these being analogous to `None`.

The two interpreters come into play via instances of `SupplyToSyntax`:

1. Calling `Trials.withLimit(s)` or `Trials.withStrategy` creates an instance of `SupplyToSyntaxImplementation`, which is a method-local class defined in `TrialsImplementation.withStrategy`. This extends and completes a large body of hoisted implementation provided by `SupplyToSyntaxSkeletalImplementation`, including [its own interpreter](https://raw.githubusercontent.com/sageserpent-open/americium/0f32a19942c3f84fcfebf8f8eecc6c488f1de247/src/main/scala/com/sageserpent/americium/generation/SupplyToSyntaxSkeletalImplementation.scala#L513). `SupplyToSyntaxImplementation` couples the implementation inherited from `SupplyToSyntaxSkeletalImplementation` to the state of `TrialsImplementation`, principally to pick up `TrialsImplementation.generation`.
2. Calling `Trials.withRecipe` creates an instance of an anonymous subclass of `SupplyToSyntax`; this implementation is very simple, so has not been excised from `TrialsImplementation` - in fact the interpreter it references is left as a helper [within `TrialsImplementation`](https://raw.githubusercontent.com/sageserpent-open/americium/c53549899b4929c1403bad4cb79d3b3e7af75f94/src/main/scala/com/sageserpent/americium/TrialsImplementation.scala#L75).

## Recipe Interpreter and `DecisionStages`

Let's start with the interpreter that `.withRecipe` leads to - it's simpler and gently introduces some background. Looking at its [definition](https://raw.githubusercontent.com/sageserpent-open/americium/c53549899b4929c1403bad4cb79d3b3e7af75f94/src/main/scala/com/sageserpent/americium/TrialsImplementation.scala#L75), we see it takes a `GenerationOperation` - so a denotation of choice, alternation or whatever and yields a `State` monadic value as provided by Cats.

Each call to the interpreter builds a state transforming operation that is parked within a `State` - so the `GenerationOperation` denotes some magic, and the resulting `State` can actually _perform_ it later on.

This interpreter is folded through `Trials.generation` which plays out whatever monadic operations where set up by client code into an  instance of `State`, using the interpreter to translate these operations into corresponding ones in `State`.

The resulting instance of `State` is then run on a `DecisionStages` to apply the state transformations and yield a test case as the result. Easy! Only what does `DecisionStages` mean? What does the interpreter do for each denoted bit of magic?

Cast your eyes around and you will see the interpreter is embedded in [`TrialsImplementation.reproduce`](https://raw.githubusercontent.com/sageserpent-open/americium/c53549899b4929c1403bad4cb79d3b3e7af75f94/src/main/scala/com/sageserpent/americium/TrialsImplementation.scala#L68), which is an overload shadowing the implementation of `Trials.reproduce`. That second method takes a recipe, parses it into a `DecisionStages` and then delegates to the overload with the interpreter, so we realise that `DecisionStages` encodes a test case, presumably as a list of `Decision` instances.

A `Decision` is some specific choice or specific input to a case factory; so what the interpreter does is to transform the state - a list of decisions that encodes a test case - by peeling off the leading decision, using the generation operation to provide context to execute that decision. So if for example we have a `ChoiceOf` decision, the interpreter will use the corresponding `Choice` generation operation to provide the possible choices of test case, selecting one with `ChoiceOf.index`.

The effect of folding the interpreter and then running the state transformations on a `DecisionStages` is to run through all of the decisions and build corresponding parts of the test case. Those parts are put together by the `State` monadic operations that mirror the ones that originally were composed by the client.

So if the client wrote `api.choose(1, 5, -3).map(_ * 9)`, then if a decision stages of `List(ChoiceOf(2))` is interpreted, this will select -3 and then pass it through a mapping operation mirrored in `State` to supply -27.

In a similar fashion, a `FactoryInputOf` decision is used to feed input to a case factory provided by the generation operation to build up part of a test case.

Bear in mind, the translation of the magic into state transformations is what the interpreter does - the mirroring of the monadic operations composed by the client comes from folding the interpreter; that part is what Cats' `Free` offers.

## Generic Interpreter and `DecisionStagesInReverseOrder`

Now for the big, scary one! This time the interpreter transforms a `GenerationOperation` into a `StateT` layered over `Option` - so the result of folding the interpreter yields an optional result, which is a pair of the state being transformed and a test case. This is in contrast with the interpreter used for test case reproduction, which always yields a test case (given a correct recipe) and which ignores the final state after folding.

The [state being transformed](https://raw.githubusercontent.com/sageserpent-open/americium/0f32a19942c3f84fcfebf8f8eecc6c488f1de247/src/main/scala/com/sageserpent/americium/generation/SupplyToSyntaxSkeletalImplementation.scala#L253) is richer - it consists of an optional `DecisionsStages`, a `DecisionStagesInReverseOrder`, a complexity and a cost. With the exception of the first piece, all of these are _built up_ as the state is transformed; this is in contrast to the other interpreter which uses the state purely as a source of information - here the state is being for the most part defined in tandem with each part of the test case.

The idea is for the interpreter to select parts of test case using the generation operation it is working one, using a pseudorandom behaviour to perform either selection from a choice or feeding of an input to a case factory. This by itself is enough to generate the test case, so why transform the state?

Well, for one thing, if we want to reproduce a test case, we will need the `DecisionStages` to drive this in the other interpreter - so as the interpreter selects a choice or feeds a case factory input, it records what is did in a decision and adds it to a list in reverse order, namely `DecisionStagesInReverseOrder`. Remember that the other interpreter picks apart a `DecisionStages` from left to right - here the interpreter is adding the decision stages left to right, so they are defined in reverse order.

**NOTE**: `DecisionStagesInReverseOrder` is _not_ a simple list, rather it is a _hash-consed_ difference list, this is done so that reversing the list can be deferred as long as possible and avoids a very nasty performance hit where `DecisionStagesInReverseOrder` is used a a key when detecting duplicate test case formulations - it is cheap to compute the hash of a hash-consed data structure.

Not only does the state need to track the decision stages, it also needs to keep track of the complexity of the test case, so that a complexity limit can be applied as a test case is being formulated. At first glance, it appears that the complexity is simply the number of decision stages being recorded, but it is possible to suspend the complexity count in order to allow generation of explicitly sized collections; this requires the complexity count to be decoupled from the number of decision stages in places.

There is also a cost, and this is used as part of shrinkage. In fact, shrinkage can also make use of the decision stages recorded to potentially reproduce a test case, this is what the optional part of the state is for - they allow previously recorded decision stages to be recycled back into subsequent calls to the interpreter to guide shrinkage.

## Shrinkage

Shrinkage of a test case takes place via two coordinated activities within `SupplyToSyntaxSkeletalImplementation` - at a high level, there is a Fs2 `Stream` that grabs test cases from the interpreter and supplies then to the test code; this stream monitors whether the test succeeds or fails for a given trial execution and can influence the upstream generation of test cases done by [`.cases`](https://raw.githubusercontent.com/sageserpent-open/americium/0f32a19942c3f84fcfebf8f8eecc6c488f1de247/src/main/scala/com/sageserpent/americium/generation/SupplyToSyntaxSkeletalImplementation.scala#L233).

At a low level, the interpreter's generation of a test case is modulated by a context held by its enclosing method, namely `.cases` - this fixes:

1. The scale deflation level.
1. The decision stages to guide shrinkage.

Shrinkage takes place in two ways in Americium - firstly, if a test case is produced via a `CaseFactory`, then as mentioned above, the case factory has bounds for its inputs and a preferred maximally shrunk input. The scale deflation level modulates the interpreter so that instead of using an input chosen randomly between the lower and upper bound, it will constrict the range of the input, bringing the effective bounds closer together around the maximally shrunk input as the deflation level increases to a maximum. This means a test case built out of many streamed parts is shrunk down to a test case built out of shrunk parts, as long as shrinkage keeps discovering better failing test cases.

Secondly, guided shrinkage can occur - here the decision stages recorded when a failing test case was built up are used as a template to constrain the generation of further test cases. This is similar to the reproduction of a failing test case described above, only this time the interpreter mixes up the approaches - for a choice, it will try to reproduce the exact same choice for the guide decision if the context of the test case currently being built permits that, in other words, the new test case isn't radically different in how it is formulated from the guide test case. For a case factory, the interpreter will try to mix two approaches - the first is to use the guide decision as a bound and make a random input between the guide decision's bound and the maximally shrunk input, the second is the aforementioned use of scale deflation, which ignores the guide decision completely.

The higher level logic starts with just scale deflation, but if progressive scale deflation fails to improve on the previous shrunk test case, it will switch to _panic mode_ and start to perform guided shrinkage. If guided shrinkage succeeds in finding a better failing test case, shrinkage will resume in just scale deflation mode again.

Whether shrinkage is improving or not is dictated by both the number of decision stages needed to build a test case - the fewer, the better, and as a tiebreaker whether the cost is lower in comparison with the previously failing test case.

The cost of a test case is the sum of the contributions from each case factory invocation - each contribution is the square of the difference between the input used to drive the case factory and the maximally shrunk input. Choices are always zero cost, so the cost measures how well the contributions from case factories have been shrunk down to the preferred values.

The aforementioned Fs2 stream is provided by [`.shrinkableCases`](https://raw.githubusercontent.com/sageserpent-open/americium/0f32a19942c3f84fcfebf8f8eecc6c488f1de247/src/main/scala/com/sageserpent/americium/generation/SupplyToSyntaxSkeletalImplementation.scala#L697), this contains the logic for using what is generated by `.cases`, monitoring downstream failed trials and the high-level shrinkage logic. It produces a stream of `TestIntegrationContext` instances, these provide a test case and callbacks that the test code uses via an implicit context set up around the call to the test itself. It is these callbacks that signal to the streaming logic in `shrinkableCases` whether a test case is being rejected due to inline filtration in the test, or whether shrinkage needs to proceed due to a failed trial.

## Wrapping Up

There is a fair bit to cover above, and you will definitely need to poke around the sources to get a full picture - the text is really just a bunch of pointers to important pieces of the puzzle, so you know where to start breaking down the codebase.

One thing that should be apparent - the codebase drifts around from a _high-church_ Scala approach using free monads and streams to a more workaday _just-the-basics pure functional_ Scala to an _imperative Scala-as-a-better-Java_ style to out-an-out usage of _Java sources_. Bear in mind that this is intended as a black-box tool - as long as it passes its tests, and they are rigorous, the code style is of less concern - whatever gets the job done!

Also bear in mind this is as much levelled at the Java community as the Scala community, it just happens to be implemented mostly in Scala because it's ~~cooler and way more fun~~much easier to express the implementation concepts.

In other words, put your efforts more into good testing first and coding style second. Having said that, the codebase has been refactored umpteen times and is definitely ripe for more improvement, so don't hold back if you really need to stir the pot.

If you do decide to hack on the codebase, may I ask that you use the provided Scalaformat setup and stick to it, using auto-formatting. There is also a file for Java formatting using IntelliJ too.

Getting pull requests that are largely format rewrites with a couple of subtle changes hidden within them and no supporting tests will not be expeditious. :grin: If it's any consolation, the auto-formatting isn't my favourite either, but the convenience of not faffing around correcting my lousy typing skills has long triumphed over any attachment I have to any particular code style. Prefer convenience and consistency of pull requests.

Happy hacking!
