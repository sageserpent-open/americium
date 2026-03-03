---
layout: default
title: Complexity Budgeting
parent: Advanced Techniques
nav_order: 4
---

# Complexity Budgeting
{: .no_toc }

Controlling recursive structure generation with complexity awareness
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
   {:toc}

---

## The Problem

When generating **recursive data structures** (trees, expressions, nested lists, etc.), you need to control their depth and size:

**Too shallow:**
```
1           ← Boring, doesn't test recursion
```

**Too deep:**
```
(((((((((((((((((((((1 + 2) - 3) * 4) / 5) + 6) - 7) ...))))))))))))))))
↑ 100 levels deep - unwieldy, hard to debug
```

**What you want:** A **balanced distribution** of shallow and deep structures.

We've already seen one approach - using `api().delay()` to prevent infinite recursion:
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

This works, but the distribution is **random**. Sometimes you get deep trees, sometimes shallow ones. There's no **control** over complexity.

---

## The Solution: Complexity-Aware Generation

Use **`api().complexities`** to access the current complexity budget and make **depth-aware** decisions:
```scala
def calculation(): Trials[String] = {
  val constants = api.integers(1, 100).map(_.toString)
  
  val unaryExpression = api.delay(() =>
    calculation().map(expr => s"-($expr)")
  )
  
  val binaryExpression = api.delay(() =>
    calculation().flatMap(lhs =>
      api.choose("+", "-", "*", "/").flatMap(op =>
        calculation().map(rhs => s"($lhs) $op ($rhs)")
      )
    )
  )
  
  // 🌟 Complexity-aware alternation
  api.complexities.flatMap { complexity =>
    api.alternateWithWeights(
      complexity -> constants,         // Weight increases with depth
      2 -> unaryExpression,           // Fixed weight
      2 -> binaryExpression           // Fixed weight
    )
  }
}
```

### How It Works

**`api.complexities`** provides the **current complexity level**:
- Starts at **0** (no complexity consumed yet)
- **Increases** as you build nested structures
- Used to **weight alternatives**
```scala
api.alternateWithWeights(
  complexity -> constants,      // Weight = complexity
  2 -> unaryExpression,        // Weight = 2 (fixed)
  2 -> binaryExpression        // Weight = 2 (fixed)
)
```

At **low complexity** (shallow):
- `constants` weight = 1
- `unaryExpression` weight = 2
- `binaryExpression` weight = 2
- **Recursion favored** (weights 2 + 2 = 4 vs. 1)

At **high complexity** (deep):
- `constants` weight = 50
- `unaryExpression` weight = 2
- `binaryExpression` weight = 2
- **Termination favored** (weight 50 vs. 4)

---

## Understanding Complexity

**Complexity** in Americium measures the **degrees of freedom** used to build a test case:
```scala
// No complexity
api.only(42)

// Complexity = 1 (one choice)
api.choose(1, 2, 3)

// Complexity = n (n independent choices)
api.integers().immutableListsOfSize(n)

// Complexity = depth of recursion
calculation()  // Could be 0 (constant) to 50+ (deep expression)
```

Each decision point **consumes complexity**:
- `.choose()` consumes 1
- `.integers()` consumes 1
- Recursive call consumes whatever the recursive structure consumes

---

## The Magic of Weighted Alternation
```scala
api.alternateWithWeights(
  complexity -> alternative1,   // ← Lambda: weight varies with complexity
  fixedWeight -> alternative2   // ← Constant: weight is always fixedWeight
)
```

**Early in generation** (complexity = 0):
```
Weights: [0, fixedWeight]
→ alternative2 heavily favored
```

**Deep in generation** (complexity = 50):
```
Weights: [50, fixedWeight]
→ alternative1 heavily favored
```

This creates a **natural termination pressure** - the deeper you go, the more likely you are to pick the base case!

---

## Complete Example: Expression Trees

Let's build a complete expression generator with complexity budgeting:
```scala
sealed trait Expr
case class Const(value: Int) extends Expr
case class Neg(expr: Expr) extends Expr
case class BinOp(left: Expr, op: String, right: Expr) extends Expr

def exprTrials: Trials[Expr] = {
  val constants: Trials[Expr] = 
    api.integers(1, 100).map(Const(_))
  
  val unary: Trials[Expr] = 
    api.delay(() => exprTrials.map(Neg(_)))
  
  val binary: Trials[Expr] = 
    api.delay(() =>
      exprTrials.flatMap(left =>
        api.choose("+", "-", "*", "/").flatMap(op =>
          exprTrials.map(right =>
            BinOp(left, op, right)
          )
        )
      )
    )
  
  // Complexity-aware generation
  api.complexities.flatMap { complexity =>
    api.alternateWithWeights(
      complexity -> constants,
      1 -> unary,
      1 -> binary
    )
  }
}

// Test it
exprTrials.withLimit(10).supplyTo { expr =>
  println(s"Expression: $expr")
  println(s"Depth: ${depth(expr)}")
}
```

Output shows **varied depths**:
```
Expression: Const(42)
Depth: 1

Expression: Neg(BinOp(Const(7), +, Const(3)))
Depth: 3

Expression: BinOp(Const(1), *, Neg(Const(5)))
Depth: 3

Expression: BinOp(Neg(Const(2)), -, BinOp(Const(8), /, Const(4)))
Depth: 4

Expression: Const(91)
Depth: 1
```

Without complexity budgeting, you'd get much more extreme variation (all depth 1, or all depth 20+).

---

## Java Example

The same pattern works in Java:
```java
public static Trials<Expr> exprTrials() {
    final Trials<Expr> constants = 
        api().integers(1, 100).map(Const::new);
    
    final Trials<Expr> unary = 
        api().delay(() -> exprTrials().map(Neg::new));
    
    final Trials<Expr> binary = 
        api().delay(() -> 
            exprTrials().flatMap(left ->
                api().choose("+", "-", "*", "/").flatMap(op ->
                    exprTrials().map(right ->
                        new BinOp(left, op, right)
                    )
                )
            )
        );
    
    // Complexity-aware alternation
    return api().complexities().flatMap(complexity ->
        api().alternateWithWeights(
            Map.entry(complexity, constants),
            Map.entry(1, unary),
            Map.entry(1, binary)
        )
    );
}
```

---

## Fine-Tuning the Distribution

Adjust the **fixed weights** to control recursion depth:

### More Recursion
```scala
api.alternateWithWeights(
  complexity -> constants,
  5 -> unary,              // ← Higher fixed weights
  5 -> binary              // ← More recursion favored initially
)
```

Result: **Deeper trees** on average.

---

### Less Recursion
```scala
api.alternateWithWeights(
  complexity -> constants,
  1 -> unary,              // ← Lower fixed weights
  1 -> binary              // ← Terminates sooner
)
```

Result: **Shallower trees** on average.

---

### Favor Specific Constructors
```scala
api.alternateWithWeights(
  complexity -> constants,
  3 -> unary,              // ← Unary expressions more common
  1 -> binary
)
```

Result: More unary operations (negation) than binary operations.

---

## Working with Complexity Limits

Remember `.withComplexityLimit()` from Configuration?
```scala
exprTrials
  .withLimit(100)
  .withComplexityLimit(20)    // ← Maximum complexity budget
  .supplyTo { expr =>
    // Expressions limited to ~20 degrees of freedom
  }
```

The complexity budget **starts at the limit** and **decreases** as you make choices:
```
Initial budget: 20

Choose constant (1 degree): Budget = 19
Choose binary op (1 degree): Budget = 18
Choose left subexpr (3 degrees): Budget = 15
Choose right subexpr (5 degrees): Budget = 10
...
```

When budget is **low**, the `complexity` value in your weighted alternation is **low**, so termination is favored.

---

## Advanced Pattern: Complexity Thresholds

Use explicit thresholds for more control:
```scala
api.complexities.flatMap { complexity =>
  if (complexity < 5) {
    // Low complexity: strongly favor recursion
    api.alternateWithWeights(
      1 -> constants,
      10 -> unary,
      10 -> binary
    )
  } else if (complexity < 15) {
    // Medium complexity: balanced
    api.alternateWithWeights(
      complexity -> constants,
      2 -> unary,
      2 -> binary
    )
  } else {
    // High complexity: strongly favor termination
    api.alternateWithWeights(
      complexity * 2 -> constants,
      1 -> unary,
      1 -> binary
    )
  }
}
```

This creates **distinct phases**:
1. **Build up** (complexity < 5)
2. **Balanced** (5 ≤ complexity < 15)
3. **Wind down** (complexity ≥ 15)

---

## Complexity and Shrinkage

When a test fails, Americium shrinks toward **lower complexity**:
```
Initial failure (complexity ~25):
BinOp(
  BinOp(Const(42), +, Neg(BinOp(Const(7), *, Const(3)))),
  -,
  BinOp(Neg(Const(5)), /, Const(2))
)

After shrinkage (complexity ~3):
BinOp(Const(1), +, Const(0))
```

The shrunk case has **much lower complexity** - simpler structure, easier to debug!

---

## Real-World Example: JSON Generation

Generate realistic JSON with controlled nesting:
```scala
sealed trait Json
case class JNull() extends Json
case class JBool(value: Boolean) extends Json
case class JNum(value: Double) extends Json
case class JStr(value: String) extends Json
case class JArr(values: List[Json]) extends Json
case class JObj(fields: Map[String, Json]) extends Json

def jsonTrials: Trials[Json] = {
  val jNull: Trials[Json] = api.only(JNull())
  val jBool: Trials[Json] = api.booleans().map(JBool(_))
  val jNum: Trials[Json] = api.doubles(-1000, 1000).map(JNum(_))
  val jStr: Trials[Json] = api.strings().map(JStr(_))
  
  val jArr: Trials[Json] = api.delay(() =>
    jsonTrials.immutableLists().map(vs => JArr(vs.toList))
  )
  
  val jObj: Trials[Json] = api.delay(() =>
    api.strings()
      .and(jsonTrials)
      .immutableLists()
      .map(pairs => JObj(pairs.map(p => (p._1, p._2)).toMap))
  )
  
  // Complexity-aware distribution
  api.complexities.flatMap { complexity =>
    api.alternateWithWeights(
      complexity -> api.alternate(jNull, jBool, jNum, jStr),  // Primitives
      2 -> jArr,                                               // Arrays
      2 -> jObj                                                // Objects
    )
  }
}
```

This generates:
- **Simple JSON** at low complexity (primitives)
- **Nested JSON** at medium complexity (arrays/objects with primitives)
- **Terminates** at high complexity (back to primitives)

---

## Combining with Complexity Limits
```scala
jsonTrials
  .withLimit(100)
  .withComplexityLimit(30)
  .supplyTo { json =>
    // Test JSON parsing/serialization
    val serialized = Json.stringify(json)
    val parsed = Json.parse(serialized)
    
    assert(parsed == json)
  }
```

The complexity limit ensures you don't generate **absurdly deep** JSON like:
```json
{"a": {"b": {"c": {"d": {"e": {"f": {"g": {"h": ...}}}}}}}
```

---

## When to Use Complexity Budgeting

### ✅ Use when:
- Generating **recursive data structures** (trees, expressions, JSON, XML)
- You want **controlled depth distribution**
- Testing parsers, evaluators, tree algorithms
- You need **balanced** shallow and deep test cases

### ❌ Don't use when:
- Data is **not recursive** (just use regular trials)
- You want **maximum depth** (use large complexity limit without budgeting)
- Overhead of `complexities` flat-map is too much

---

## Performance Note

The `api.complexities.flatMap(...)` pattern adds **one extra flat-map** to your generation. For simple cases, this is negligible. For very large test suites, it's a small trade-off for much better control.

---

{: .note-title }
> Key Takeaways
>
> - **`api.complexities`** - Access current complexity budget
> - **Pattern:** `api.complexities.flatMap(c => alternateWithWeights(c -> base, n -> recursive))`
> - **Low complexity** - Recursion favored (fixed weights higher)
> - **High complexity** - Termination favored (variable weight higher)
> - **Natural termination pressure** - Deeper = more likely to stop
> - **Fine-tune with weights** - Adjust fixed weights to control depth distribution
> - **Shrinks to lower complexity** - Simpler structures after shrinkage
> - **Combine with complexity limits** - `.withComplexityLimit(n)` caps maximum depth
> - Perfect for trees, expressions, JSON, XML, nested structures