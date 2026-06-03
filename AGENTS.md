# Americium Agent Guide

This file provides instructions and tips for agents working on the Americium project or using it in other projects (like Plutonium).

## Contributing to Americium

### Build & Test
- **Tool**: Use `sbt` for all build tasks.
- **Testing**: Run the full test suite with `sbt test`.
- **Cross-building**: Americium targets Scala 2.13 and Scala 3. Sources are split between:
  - `src/main/scala`: Shared Scala code.
  - `src/main/scala-2.13` / `src/main/scala-3`: Version-specific Scala code.
  - `src/main/java`: Java code (often used for Java client APIs).

### Code Style & Formatting
- **Style**: Pure functional Scala is preferred, but keep it accessible. Pragmatic "Scala-as-a-better-Java" or out-and-out Java is acceptable in the Java sub-packages if it serves the client API better.
- **Formatting**: **Mandatory**.
  - Scala: Use the provided `scalaformat` configuration.
  - Java: Use the IntelliJ Java formatter.
- **Verification**: Ensure all changes pass `sbt test`.

### Release Workflow (`checkTheLibrary`)
- **Important**: There is a special branch `checkTheLibrary` used to validate published artifacts.
- **Action**: It takes merges from `master`, deletes all non-test sources, and updates `build.sbt` to point to the latest release.
- **CRITICAL**: **Never merge `checkTheLibrary` back into `master`**.

---

## Using Americium

### Core Concepts
- **`Trials<Case>`**: The central interface. It's a "fountain" of test data.
- **Limits**: You **must** specify a limit (e.g., `.withLimit(100)`) before supplying cases to a test.
- **Shrinkage**: Integrated and automatic. No separate shrinkers are needed. It uses:
  - **Distance Shrinkage**: Moving values toward a target (usually zero).
  - **Complexity Shrinkage**: Reducing structure size/nesting (degrees of freedom).

### Advanced Techniques
When standard `.map` and `.flatMap` aren't enough, consider these patterns:
- **Forcing Duplicates**: Nest flat-maps to pick from a generated pool of values to guarantee duplicates.
- **Unique IDs**: Generate sequences of IDs that are readable and shrink predictably.
- **Permutations**: Use index permutations to test order-dependent logic.
- **Complexity Budgeting**: Use a budget (passed through recursion) to prevent exponential growth in recursive structures.
- **Alternate Picking**: Merge multiple streams of data while maintaining relative order.

### Quick Reference
- `Trials.api().integers(lower, upper)`: Standard integer range.
- `trials.immutableLists()`: Collection of varying sizes.
- `api().delay(() -> ...)`: Use for recursive trials to avoid `StackOverflowError`.
- `trials.filter(predicate)`: Use sparingly; synthetic construction via `.map`/`.flatMap` is usually better for performance.
