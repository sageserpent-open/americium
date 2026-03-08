---
layout: default
title: Design and Implementation
parent: Reference
nav_order: 3
---

# Design and Implementation
{: .no_toc }

Yes, do pay attention to the man behind the curtain
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## Congratulations!

Congratulations, you have reached the final topic - by now you are a seasoned expert in the practice of the Americium way.

May you design by contract, and all your preconditions be met - that is your invariant. ⚖️

Perhaps you'd like to work on this project yourself? Let's have a chat about what's going on under the hood...

---

## Building, Directory Layout and Releases

Americium is built as an **SBT project**, using cross-platform support to target Scala 2.13 and Scala 3.

The **gha-scala-library-release-workflow** is used to publish library JARs to Sonatype, running as a manually-triggered CI action on GitHub.

This workflow drives the `release` task in SBT, supplied by the plugin **sbt-release**. As mentioned in `build.sbt`, the support for cross-building from sbt-release is disabled; SBT's built-in cross-building is used instead.

### Directory Structure

Sources are organized in typical Maven/SBT directory roots:

- `./src/main/scala` - Cross-platform shared Scala sources
- `./src/main/scala-2.13` - Sources specific to Scala 2.13
- `./src/main/scala-3` - Sources specific to Scala 3
- `./src/main/java` - Java code

There is a similar hierarchy under `./src/test`.

---

## Artifacts (Since Version 2.0.0)

Americium is published as **three separate artifacts**:

### 1. `americium` (Core Framework)

The core framework for generating test cases with integrated shrinkage.

**Packages:**
- `com.sageserpent.americium` - Scala client APIs, API implementations and tests
- `com.sageserpent.americium.java` - Java client APIs, forwarding implementations and tests
- `com.sageserpent.americium.examples` - Example tests (Scala)
- `com.sageserpent.americium.java.examples` - Example tests (Java)
- `com.sageserpent.americium.generation` - Low-level support for test case generation
- `com.sageserpent.americium.storage` - Storage for recipe reproduction

---

### 2. `americium-junit5` (JUnit5 Integration)

Deep integration with JUnit5, allowing individual replay of test cases.

**Packages:**
- `com.sageserpent.americium.junit5` - Tight JUnit5 integration via `dynamicTests` (Scala)
- `com.sageserpent.americium.junit5.java` - Loose integration via annotations and tight integration via `JUnit5` (Java)
- `com.sageserpent.americium.junit5.storage` - Storage for JUnit5 replay

---

### 3. `americium-utilities` (Utility Libraries)

Handy utilities used by Americium that may be useful outside a testing context.

**Packages:**
- `com.sageserpent.americium.utilities` - General utilities

---

### Package Naming Notes

{: .note }
> **Asymmetry:** There is no `.scala` sub-package. Scala client APIs were implemented first and taken as default for historical reasons.

{: .note }
> **Language mixing:** Sources in the `.java` sub-package may be written in Java or Scala, even though they target Java clients.

---

## Release Process

Releases are triggered **manually** off branch `master` via GitHub. After each release is published to Sonatype:

1. Branch `checkTheLibrary` takes a merge from `master`
2. **All non-test sources are deleted** in the merge
3. `build.sbt` is updated to reference the published version of Americium
4. This allows the same test suite to **validate the published artifacts**

{: .warning }
> **Never merge `checkTheLibrary` back into `master`** - that would delete all the non-test sources! It plays constant catch-up with `master` instead.

---

## TrialsApi

There are **parallel Scala and Java implementations** of the Scala and Java forms of `TrialsApi`.

The Java implementation simply **delegates** to the Scala API, using things like `Int.(un)box` to negotiate the gap between:
- **Scala's idea**: Generic type parameter is a primitive type (`Int`)
- **Java's idea**: All generic type parameters are reference types (`Integer`)

### Collection Conversions

Around the codebase you will see use of the Java ↔ Scala collection conversions in `scala.collection.convert`, both to:
- Convert Java iterables/collections passed as arguments to the Scala equivalent
- Convert back where the test case type is a collection

### API Implementation Source

The ultimate source of the API implementation objects is **`TrialsApis`**. As these objects are immutable, they are held as simple Scala `val` bindings and reused via calls to `Trials.api`.

Note how the definition of `TrialsApis.javaApi` is tied to that of `TrialsApis.scalaApi` in a forwarding implementation.

---

## CaseFactory

The lowest level implementations of the Scala API mostly construct instances of `TrialsImplementation`. Several methods use **`.stream`**, passing a custom `CaseFactory` to the call.

### The CaseFactory Trait

In its Scala guise:
```scala
trait CaseFactory[+Case] {
  def apply(input: BigInt): Case
  
  def lowerBoundInput: BigInt
  
  def upperBoundInput: BigInt
  
  def maximallyShrunkInput: BigInt
}
```

This underpins the **'streaming' methods** in `TrialsApi`, used to generate test cases over some domain (contrast with `.choose` that picks from test cases given explicitly by client code).

---

### How It Works

**`CaseFactory`** has the notion of an **output range** over `Case`, which contains all the possible test cases that could be generated.

The particular test case generated (via `.apply`) is controlled by an **input domain** over `BigInt`:

- The input domain is the entire interval `[lowerBoundInput, upperBoundInput]`
- Typically, `Case` has a total ordering (say a numeric type)
- We expect `.apply` to be a **monotonic increasing function** of `BigInt`
- The value of `.apply(maximallyShrunkInput)` is the **maximally shrunk value** of the test case type

Since Americium can consult the bounds of the input domain and `.maximallyShrunkInput`, it can drive the case factory correctly to generate test cases.

---

### Example: Bytes

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

See how there is a simple **narrowing conversion** from domain `BigInt` to range `Byte`, and the input domain bounds are chosen according to the bounds of `Byte`.

---

### Example: BigDecimals

What happens when we transform the integral domain of `BigInt` into something like `BigDecimal` or `Double`, that while technically countable, is approximately an Archimedean field?

The implementation of `TrialsApi.bigDecimals` chooses either:

1. **Stick with `[Long.MinValue, Long.MaxValue]`** if this can be easily interpolated to the actual bounds while retaining reasonable precision
2. **Expand beyond `Long`** if it can't achieve at least `numberOfSubdivisionsOfDoubleUnity` precision

There is **fancy footwork** to ensure bounds and `.maximallyShrunkInput` transform to exactly the corresponding test case values. We split interpolation into two halves:
- Lower bound → maximally shrunk value
- Maximally shrunk value → upper bound

This ensures the anchor points hit the desired test case values spot-on.

---

## Trials

There are **conversions between the two flavours** of `Trials` - both APIs have a conversion method that yields a corresponding interface/trait in the other language.

This actually yields **the same object as `this`**, only the nominal interface/trait type changes to reflect the crossover.

### Implementation Structure

Ultimately the implementation of both flavours is realized by:

1. **Two skeleton implementation traits** specific to each flavour (`TrialsSkeletalImplementation`)
2. **One concrete implementation class** (`TrialsImplementation`) that extends both skeleton traits

The skeleton traits house implementation boilerplate:
- The **Java skeleton** delegates to the Scala form via conversion methods
- The **Scala skeleton** is implemented as a combination of:
    - Wrapping `Generation` within `TrialsImplementation`
    - Delegation to some stray methods in `TrialsImplementation`

---

### Why Skeletons?

The skeleton traits are a bit messy, but they:
- ✅ Avoid sprawl in `TrialsImplementation` (which is already large)
- ✅ Partition the overrides for Scala and Java forms nicely where they have differing type signatures
- ✅ Keep common overrides in `TrialsImplementation` (see `.withLimit` and `.withStrategy`)

---

### TrialsImplementation as Facade

`TrialsImplementation` is really a **facade** - it keeps hold of an instance of `Generation` which is the underlying state used to formulate test cases.

The generation is **shared** with instances implementing `SupplyToSyntax` that it creates in calls to:
- `.withStrategy`
- `.withLimit(s)` (indirectly by delegation to `.withStrategy`)
- `.withRecipe`

---

## Generation and Its Two Interpreters

`Trials` is a **monadic type** supporting mapping, flat-mapping, and filtering. As far as clients are concerned, it:
- Offers the usual monadic composition approach
- Has lots of magic factory methods (what `TrialsApi` offers)
- Supplies test cases via conversion to `SupplyToSyntax`

For `.withLimit(s)` and `.withStrategy`: **potentially many test cases** can be produced  
For `.withRecipe`: **only one specific test case** can ever be produced

---

### The Free Monad Approach

The difference in behavior is implemented by a **free monad** (`Generation`) and its two interpreters.

Using a free monad supports all API monadic operations using straightforward delegation from the Scala skeleton implementation, wrapping the resulting instances of `Generation` back up in a `TrialsImplementation`.

**Why separate `Generation` from `Trials`?**

`Generation` is simply a type definition around the **Cats `Free` class hierarchy**. The latter cannot be amended to implement `Trials` as an interface, nor can it have additional methods added.

So there are **two levels of delegation**:
1. Java trials instance → Scala trials instance
2. Scala trials instance → `Generation` instance

---

### What Is a Free Monad?

In itself, a free monad is just a framework that:
- Provides boilerplate for monadic operations
- Offers a way of integrating **'magic' instances** that denote behavior
- By itself, doesn't do anything other than allow monadic composition
- Requires an **interpreter** to make the magic happen

---

### The Magic Instances

Look at how a choice is implemented by the Scala form of `TrialsApi`:

An instance of `TrialsImplementation` is created by an **auxiliary constructor** from a `GenerationOperation` - in this case a `Choice`.

Each case class implementing `GenerationOperation` **denotes** some kind of magic:
- `Choice` - Choosing a test case from a set of choices
- `FactoryInputOf` - Feeding input to a case factory
- And so on...

The auxiliary constructor then **lifts** the `GenerationOperation` into an instance of `Generation` as per the free monad cookbook.

{: .note }
> **Generation operations are passive** - they denote something magical and provide the information needed to perform that magic (choices of data, a case factory, etc.), but there is no implementation detail. That job falls to the interpreters!

---

### Further Reading

If you're wondering what a 'free monad', 'lifting', and 'interpreters' are:
- **Cats documentation:** [typelevel.org/cats](https://typelevel.org/cats/)
- **Blog post example:** [Free Monads in Scala](https://blog.rockthejvm.com/free-monad/)

---

### The Two Interpreters

Interpreters come into play via instances of `SupplyToSyntax`:

#### 1. Recipe Interpreter (`.withRecipe`)

**Created by:** `Trials.withRecipe`  
**Implementation:** Anonymous subclass of `SupplyToSyntax`  
**Interpreter:** Helper method within `TrialsImplementation`

This interpreter is **simpler** - dedicated to reproducing a single test case from a recipe.

---

#### 2. Generic Interpreter (`.withLimit`, `.withStrategy`)

**Created by:** `Trials.withLimit(s)` or `Trials.withStrategy`  
**Implementation:** `SupplyToSyntaxImplementation` (method-local class in `TrialsImplementation.withStrategy`)  
**Extends:** `SupplyToSyntaxSkeletalImplementation`  
**Couples to:** `TrialsImplementation.generation`

This interpreter handles:
- Test case generation
- Shrinkage
- Complexity tracking
- Cost calculation

---

## Recipe Interpreter and DecisionStages

Let's start with the simpler interpreter that `.withRecipe` leads to.

### How It Works

Takes a `GenerationOperation` (a denotation of choice, alternation, etc.) and yields a **`State` monadic value** (provided by Cats).

Each call to the interpreter builds a **state transforming operation** that is parked within a `State`. The `GenerationOperation` denotes some magic, and the resulting `State` can actually perform it later on.

This interpreter is **folded through** `Trials.generation`, which plays out whatever monadic operations were set up by client code into an instance of `State`.

The resulting `State` is then **run on a `DecisionStages`** to apply the state transformations and yield a test case.

---

### DecisionStages

`DecisionStages` encodes a test case as a **list of `Decision` instances**.

A `Decision` is some specific choice or specific input to a case factory. The interpreter:
1. Transforms the state (list of decisions) by peeling off the leading decision
2. Uses the generation operation to provide context to execute that decision
3. For `ChoiceOf` decision → uses corresponding `Choice` generation operation to provide possible choices, selecting one with `ChoiceOf.index`
4. For `FactoryInputOf` decision → feeds input to a case factory

---

### Example

If client wrote: `api.choose(1, 5, -3).map(_ * 9)`

And decision stages is: `List(ChoiceOf(2))`

Then:
1. Select `-3` (index 2 from choices `[1, 5, -3]`)
2. Pass through mapping operation (mirrored in `State`)
3. Supply `-27`

The **translation of magic into state transformations** is what the interpreter does. The **mirroring of monadic operations** comes from folding the interpreter - that's what Cats' `Free` offers.

---

## Generic Interpreter and DecisionStagesInReverseOrder

Now for the big, scary one! 😱

This interpreter transforms a `GenerationOperation` into a **`StateT` layered over `Option`**.

Result of folding yields an **optional result**, which is a pair of:
- The state being transformed
- A test case

---

### Rich State

The state consists of:
1. **Optional `DecisionStages`** - For guiding shrinkage
2. **`DecisionStagesInReverseOrder`** - Being built up
3. **Complexity** - Tracking degrees of freedom
4. **Cost** - Used for shrinkage quality

All except the first are **built up** as state is transformed.

---

### Purpose

The interpreter **selects parts of test case** using the generation operation, using pseudorandom behavior.

But why transform the state?

1. **Recording decisions** - Build `DecisionStagesInReverseOrder` for later reproduction
2. **Tracking complexity** - Apply complexity limits during generation
3. **Calculating cost** - Measure shrinkage quality
4. **Guiding shrinkage** - Reuse decision stages to guide shrinkage attempts

---

### Hash-Consed Difference List

{: .note }
> **`DecisionStagesInReverseOrder`** is not a simple list - it's a **hash-consed difference list**. This allows deferred reversal and avoids a nasty performance hit when used as a key for detecting duplicate test case formulations.

---

## Shrinkage

Shrinkage takes place via **two coordinated activities** within `SupplyToSyntaxSkeletalImplementation`:

### High Level: Fs2 Stream

An Fs2 `Stream` that:
- Grabs test cases from the interpreter
- Supplies them to the test code
- Monitors whether tests succeed or fail
- Influences upstream generation for shrinkage

Provided by `.shrinkableCases`, it produces a stream of `TestIntegrationContext` instances with callbacks.

---

### Low Level: Interpreter Context

The interpreter's generation is **modulated by context** held in `.cases`, which fixes:
- **Scale deflation level**
- **Decision stages to guide shrinkage**

---

### Two Shrinkage Mechanisms

#### 1. Scale Deflation (for `CaseFactory`)

The case factory has bounds and a preferred maximally shrunk input. The scale deflation level modulates the interpreter so that instead of using an input chosen randomly between bounds, it **constricts the range**, bringing effective bounds closer together around the maximally shrunk input as deflation increases.

Result: A test case built from many streamed parts shrinks down to a test case built from shrunk parts.

---

#### 2. Guided Shrinkage (using recorded decisions)

Decision stages recorded when a failing test case was built are used as a **template** to constrain generation of further test cases.

The interpreter **mixes approaches**:
- **For choice:** Try to reproduce the exact same choice if context permits
- **For case factory:** Mix two approaches:
    1. Use guide decision as bound, make random input between guide and maximally shrunk
    2. Use scale deflation (ignores guide)

---

### High-Level Shrinkage Logic

Starts with **just scale deflation**. If progressive scale deflation fails to improve, switches to **panic mode** and performs guided shrinkage.

If guided shrinkage succeeds, resume scale deflation mode again.

---

### Improvement Metrics

Shrinkage improving is dictated by:
1. **Number of decision stages** needed (fewer = better)
2. **Cost** as tiebreaker (lower = better)

**Cost** = sum of contributions from each case factory invocation:
- Each contribution = square of difference between input used and maximally shrunk input
- Choices are zero cost
- Cost measures how well case factory contributions have been shrunk

---

## Wrapping Up

There's a fair bit to cover above, and you'll definitely need to **poke around the sources** to get a full picture. The text is really just pointers to important pieces of the puzzle.

### Code Style

The codebase drifts around from:
- **High-church Scala** (free monads and streams)
- **Workaday pure functional Scala**
- **Imperative Scala-as-a-better-Java**
- **Out-and-out Java sources**

Bear in mind this is a **black-box tool** - as long as it passes its tests (and they are rigorous), code style is less concern. Whatever gets the job done!

Also bear in mind this is leveled at **both the Java and Scala communities**. It just happens to be implemented mostly in Scala because it's cooler and way more fun... much easier to express the implementation concepts. 😉

---

### Contributing Guidelines

If you decide to hack on the codebase:

✅ **Use the provided Scalaformat setup** and stick to it (auto-formatting)  
✅ **Use the IntelliJ Java formatter** for Java files  
✅ **Write tests** for your changes

❌ **Don't submit** format-rewrite PRs with subtle changes hidden within and no tests

The auto-formatting isn't anyone's favorite, but the **convenience of not faffing around** correcting lousy typing has long triumphed over attachment to any particular code style.

**Prefer convenience and consistency of pull requests.**

---

## Happy Hacking! 🎉

{: .note-title }
> Key Takeaways
>
> - **Three artifacts:** `americium` (core), `americium-junit5`, `americium-utilities`
> - **Free monad architecture** - `Generation` with two interpreters
> - **CaseFactory** - Maps `BigInt` input domain to output range
> - **Two interpreters:** Recipe (reproduction) and Generic (generation + shrinkage)
> - **Shrinkage:** Scale deflation + guided shrinkage
> - **State tracking:** Decisions, complexity, cost
> - **Hash-consed data structures** for performance
> - **Fs2 streams** coordinate high-level shrinkage logic
> - **Pragmatic code style** - whatever gets the job done
> - **Contributions welcome** - use auto-formatting and write tests!