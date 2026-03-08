---
layout: default
title: Migrating from Scalacheck
parent: Reference
nav_order: 2
---

# Migrating from Scalacheck
{: .no_toc }

Welcome to Americium - learn the local language
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## Come On In

Come on in and take the weight off your feet, we're friendly here. Like a nice refreshing glass of integrated shrinkage cordial? 🍹

If you want to speak Americium, it's expected that you'll do so with a **Scalacheck accent** - here's a guide to translation.

---

## Core Concepts

### `Arbitrary` and `Gen`

First off, **there is no `Arbitrary` analogue**.

`Gen[+T]` becomes `Trials[+T]`. Easy.

(Actually, there is something called `Factory` specifically for auto-derivation; that is a bit like `Arbitrary`. We'll get to that in a bit...)

---

## Getting a Gen

This is nearly always **explicit** in Americium (we'll cover auto-derivation later).

Instead of using the companion object `Gen`, either **import `Trials.api`** or use a method defined on the `Trials[T]` instance itself.

### API Translation Table

| Scalacheck | Americium | Notes |
|------------|-----------|-------|
| `Gen.choose`, `Gen.chooseNum` | `api.integers`, `api.doubles` | Use overloads with bounds |
| `Gen.chooseChar` | `api.characters` | With bounds |
| `Gen.chooseBigInt` | `api.bigInts` | With bounds |
| `Gen.double` | `api.doubles` | No parameters |
| `Gen.long` | `api.longs` | No parameters |
| `Gen.oneOf` | `api.choose` or `api.alternate` | Depends on `T` vs `Gen[T]` |
| `Gen.const` | `api.only` | |
| `Gen.delay` | `api.delay` | |
| `Gen.frequency` | `api.chooseWithWeights` | |
| `Gen.failed` | `api.impossible` | |
| `Gen.sequence` | `api.sequences` | Note plural! |
| `Gen.option` | `<trials>.options` | Instance method |
| `Gen.either` | `<trials>.or` | Instance method |
| `Gen.stringOf` | `<char trials>.several` | Builds strings |
| `Gen.stringOfN` | `<char trials>.lotsOfSize` | Builds strings |
| `Gen.listOf` | `<trials>.lists` | |
| `Gen.listOfN` | `<trials>.listsOfSize` | |
| `Gen.containerOf` | `<trials>.several` | |
| `Gen.containerOfN` | `<trials>.lotsOfSize` | |

{: .note }
> **Naming convention:** TrialsApi methods often have names in the **plural** - so `Gen.double` becomes `api.doubles`, `Gen.sequence` becomes `api.sequences`, and so on.

---

## Examples

### Choosing Numbers

**Scalacheck:**
```scala
Gen.chooseNum(-100, 100)
Gen.choose('a', 'z')
```

**Americium:**
```scala
api.integers(-100, 100)
api.characters('a', 'z')
```

---

### Full-Range Numbers

**Scalacheck:**
```scala
Gen.double
Gen.long
```

**Americium:**
```scala
api.doubles
api.longs
```

---

### Choices

**Scalacheck:**
```scala
Gen.oneOf(1, 2, 3)
Gen.oneOf(gen1, gen2, gen3)
```

**Americium:**
```scala
api.choose(1, 2, 3)              // Values
api.alternate(trials1, trials2, trials3)  // Trials
```

---

### Constants and Special Cases

**Scalacheck:**
```scala
Gen.const(42)
Gen.delay(expensiveGen)
Gen.frequency(
  (1, gen1),
  (2, gen2)
)
Gen.failed
```

**Americium:**
```scala
api.only(42)
api.delay(expensiveTrials)
api.chooseWithWeights(
  Map.entry(1, trials1),
  Map.entry(2, trials2)
)
api.impossible
```

---

### Collections

**Scalacheck:**
```scala
Gen.listOf(gen)
Gen.listOfN(10, gen)
Gen.containerOf[Set, Int](gen)
```

**Americium:**
```scala
trials.lists
trials.listsOfSize(10)
trials.immutableSets  // Or .sets, .several, etc.
```

---

### Options and Eithers

**Scalacheck:**
```scala
Gen.option(gen)
Gen.either(genLeft, genRight)
```

**Americium:**
```scala
trials.options
trialsLeft.or(trialsRight)
```

---

### Strings

**Scalacheck:**
```scala
Gen.stringOf(Gen.alphaChar)
Gen.stringOfN(10, Gen.alphaChar)
```

**Americium:**
```scala
api.characters('a', 'z').several
api.characters('a', 'z').lotsOfSize(10)
```

{: .note }
> Calling `.several` or `.lotsOfSize` on a **character trials** will build **string trials** by default.

---

## Gen as a Monad

`Trials` is a monad too, with a **stack-safe implementation**. It has a typeclass instance for Cats' `Monad`.

So you can `map`, `flatMap`, `filter`, and `mapFilter` till you drop. There is even `withFilter` so you can enjoy the full-fat **for-comprehension** flavour.

### Scalacheck
```scala
for {
  n <- Gen.choose(1, 10)
  s <- Gen.listOfN(n, Gen.alphaChar)
} yield s.mkString
```

### Americium
```scala
for {
  n <- api.integers(1, 10)
  s <- api.characters('a', 'z').lotsOfSize(n)
} yield s
```

Same monadic goodness! 🎉

---

## Running Tests

### Plugging in Test Lambdas

**Scalacheck:**
```scala
Prop.forAll(gen) { testCase =>
  // Test code
}
```

**Americium:**
```scala
trials.withLimit(limit).supplyTo { testCase =>
  // Test code
}
```

---

### Multiple Generators

**Scalacheck:**
```scala
Prop.forAll(gen1, gen2, gen3) { (tc1, tc2, tc3) =>
  // Test code
}
```

**Americium:**
```scala
(trials1 and trials2 and trials3)
  .withLimit(limit)
  .supplyTo { (tc1, tc2, tc3) =>
    // Test code
  }
```

Gang together the corresponding trials with **`.and`**.

---

## Test Parameters

### Minimum Successful Tests and Discard Ratio

**Scalacheck:**
```scala
Test.Parameters.default
  .withMinSuccessfulTests(100)
  .withMaxDiscardRatio(0.2)
```

**Americium:**
```scala
trials.withStrategy(_ => 
  CasesLimitStrategy.counted(
    100,   // Maximum number of cases
    0.2))  // Maximum starvation ratio
```

---

### Initial Seed

**Scalacheck:**
```scala
Test.Parameters.default
  .withInitialSeed(Some(12345L))
```

**Americium:**
```scala
trials
  .withLimit(100)
  .withSeed(12345L)
```

{: .note }
> Note that `withSeed` is called on the **`SupplyToSyntax`** (after `.withLimit()` or `.withStrategy()`), not on `Trials` directly.

---

### Max Size

**Scalacheck:**
```scala
Test.Parameters.default
  .withMaxSize(50)
```

**Americium:**
```scala
trials
  .withLimit(100)
  .withComplexityLimit(50)
```

Again, called on `SupplyToSyntax`.

---

## Sized Generators

**Scalacheck:**
```scala
Gen.size.flatMap { size =>
  Gen.listOfN(size, gen)
}
```

**Americium:**
```scala
api.complexities.flatMap { complexity =>
  // Use complexity to build trials
  trials.lotsOfSize(complexity)
}
```

The `api.complexities` trials reveals the appropriate complexity to your own code that builds up trials. Typically this is done in a flat-map.

---

## Custom Shrink Instances

**Scalacheck:**
```scala
implicit val myShrink: Shrink[MyType] = Shrink { value =>
  // Complex shrinking logic
  Stream(/* shrunk values */)
}
```

**Americium:**
```scala
// Delete them and dance a jig! 🎉
```

**No manual shrinkers needed!** Shrinkage is integrated and derives automatically from how you build your trials.

---

## Auto-Derivation (Scalacheck-Shapeless)

Your auto-derivation needs are met by `com.sageserpent.americium.Factory`, which uses the marvellous **Magnolia** library under the hood.

If you have a case class hierarchy rooted at `Root` and you want `Trials[Root]`, then pull in an implicitly derived instance.

### Scala 2.13
```scala
implicitly[Factory[Root]].trials
```

### Scala 3
```scala
given evidence: Factory[Root] = Factory.autoDerived 
// May need this if `Root` has a recursive definition like, say, `List[T]`

implicitly[Factory[Root]].trials
```

---

### Providing Evidence

As with Scalacheck-Shapeless, you will probably need to supply a few of your own **implicit definitions** to bootstrap the auto-derivation. It's the same drill, only this time it is evidences for various `Factory[T]` instantiations that you need to supply.

**As a guide,** here are the built-in implicit definitions:

- [Scala 2.13 built-ins](https://github.com/sageserpent-open/americium/blob/master/src/main/scala-2.13/com/sageserpent/americium/FactoryImplicits.scala)
- [Scala 3 built-ins](https://github.com/sageserpent-open/americium/blob/master/src/main/scala-3/com/sageserpent/americium/FactoryImplicits.scala)

---

## Complete Migration Example

Here's a side-by-side comparison of a complete test:

### Scalacheck
```scala
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

val genList: Gen[List[Int]] = 
  Gen.listOf(Gen.choose(-100, 100))

val genElement: Gen[Int] = 
  Gen.choose(-100, 100)

forAll(genList, genElement) { (list, element) =>
  val extended = list :+ element
  extended.contains(element)
}
```

### Americium
```scala
import com.sageserpent.americium.Trials

val trialsList: Trials[List[Int]] = 
  api.integers(-100, 100).lists

val trialsElement: Trials[Int] = 
  api.integers(-100, 100)

(trialsList and trialsElement)
  .withLimit(100)
  .supplyTo { (list, element) =>
    val extended = list :+ element
    assert(extended.contains(element))
  }
```

---

## Key Differences Summary

| Concept | Scalacheck | Americium |
|---------|------------|-----------|
| **Generator** | `Gen[T]` | `Trials[T]` |
| **Typeclass** | `Arbitrary[T]` | `Factory[T]` |
| **Shrinking** | `Shrink[T]` | ❌ Integrated! |
| **Running** | `Prop.forAll` | `.withLimit().supplyTo` |
| **Multi-gen** | `forAll(g1, g2)` | `(t1 and t2).supplyTo` |
| **Sized** | `Gen.size` | `api.complexities` |
| **Config** | `Test.Parameters` | `.withStrategy`, `.withSeed`, etc. |
| **API object** | `Gen` | `Trials.api` |

---

## Philosophy Differences

### Scalacheck
- **Implicit derivation** is central
- **Separate shrinkers** for each type
- **Property language** (`Prop`)
- Established ecosystem (many integrations)

### Americium
- **Explicit construction** preferred (auto-derivation optional)
- **Integrated shrinkage** (no separate shrinkers)
- **Direct test lambdas** (`.supplyTo`)
- Lean approach (bring your own assertions)

---

## Common Pitfalls

### Pitfall 1: Looking for `Arbitrary`

**Scalacheck mindset:**
```scala
implicit val arbMyType: Arbitrary[MyType] = Arbitrary(myGen)
```

**Americium approach:**
```scala
val myTrials: Trials[MyType] = /* build it explicitly */
```

Just build your trials directly! Auto-derivation (`Factory`) is available but not required.

---

### Pitfall 2: Writing Shrinkers

**Don't do this:**
```scala
// NO! Americium doesn't need this!
implicit val shrinkMyType: Shrink[MyType] = /* ... */
```

**Do this instead:**
```scala
// Just build your trials - shrinkage comes for free!
val myTrials: Trials[MyType] = 
  api.integers()
    .flatMap(x => api.strings().map(s => MyType(x, s)))
```

Shrinkage derives from construction. That's the whole point! 🎉

---

### Pitfall 3: Expecting `Prop.forAll`

**Scalacheck:**
```scala
property("my test") = forAll { (x: Int) =>
  x + 0 == x
}
```

**Americium:**
```scala
test("my test") {  // Or @Test, or whatever your framework uses
  api.integers()
    .withLimit(100)
    .supplyTo { x =>
      assert(x + 0 == x)
    }
}
```

Americium is **framework-agnostic**. Use `.supplyTo` inside whatever test framework you prefer!

---

## Migration Checklist

When migrating a Scalacheck test suite to Americium:

- [ ] Replace `Gen[T]` with `Trials[T]`
- [ ] Replace `Gen.choose` etc. with `api.integers` etc.
- [ ] Replace `Prop.forAll` with `.withLimit().supplyTo`
- [ ] Replace `Gen.size` with `api.complexities`
- [ ] **Delete all `Shrink` instances** and dance! 🎉
- [ ] Gang multiple generators with `.and`
- [ ] Replace `Test.Parameters` with `.withStrategy`, `.withSeed`, etc.
- [ ] Replace `Arbitrary` derivation with explicit construction or `Factory`
- [ ] Update imports from `org.scalacheck` to `com.sageserpent.americium`

---

## Getting Help

If you get stuck during migration:

1. **Check the API docs** - Most `Gen` methods have clear Americium equivalents
2. **Look at examples** - The [examples package](https://github.com/sageserpent-open/americium/tree/master/src/test/scala/com/sageserpent/americium/examples) has many patterns
3. **Ask questions** - Open an issue on GitHub
4. **Read this guide** - Seriously, it covers most common cases!

---

## Still Using Scalacheck?

That's fine! You can even use **both** in the same project during migration:
```scala
// Scalacheck test
property("old test") = forAll { (x: Int) => x >= 0 }

// Americium test  
test("new test") {
  api.integers().withLimit(100).supplyTo { x =>
    assert(x + 1 > x)
  }
}
```

Migrate at your own pace. No need for a big-bang rewrite!

---

{: .note-title }
> Key Takeaways
>
> - **`Gen[T]` → `Trials[T]`** - Core concept translation
> - **No `Arbitrary`** - Build trials explicitly (or use `Factory`)
> - **Delete shrinkers!** 🎉 - Shrinkage is integrated
> - **`Prop.forAll` → `.withLimit().supplyTo`** - Running tests
> - **Gang with `.and`** - Multiple generators
> - **API methods are plural** - `doubles`, `sequences`, `lists`
> - **Same monadic power** - `map`, `flatMap`, for-comprehensions
> - **Framework agnostic** - Use any test framework or assertion library
> - **Migrate incrementally** - Both libraries can coexist during transition