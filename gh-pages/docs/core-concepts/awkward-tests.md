---
layout: default
title: Awkward Tests
parent: Core Concepts
nav_order: 5
reviewed: true
---

# Awkward Tests
{: .no_toc }

Handling preconditions with `Trials.reject()` and `Trials.whenever()`
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## The Problem: Stateful Systems

Sometimes you're testing a **stateful system** where test cases represent sequences of operations. Not all sequences make sense - some violate preconditions or invariants.

Consider testing a banking system where operations can fail for perfectly valid reasons (e.g., insufficient funds). How do we handle this in a property-based test?

---

## Example: Bank Account Testing

### System Under Test

Let's look at a model of simple cash accounts. We have a `Bank` that manages `CashBoxAccounts` identified by UUIDs.

```java
interface CashBoxAccounts {
    void open(UUID accountId, int cash) throws AccountAlreadyExists;
    void deposit(UUID accountId, int cash) throws AccountDoesNotExist;
    void withdraw(UUID accountId, int requestedAmount)
            throws AccountDoesNotExist, InsufficientFunds;
    int close(UUID accountId) throws AccountDoesNotExist;
}
```

The `Bank` provides a transactional API:

```java
public class Bank {
    private final Map<UUID, Integer> accountBalances = new HashMap<>();

    public void transaction(Consumer<CashBoxAccounts> action) {
        final CashBoxAccounts cashBoxAccounts = new CashBoxAccounts() {
            // ... implementation that updates accountBalances ...
            // BUG: close(UUID) returns the balance but fails to remove
            // the account from accountBalances!
        };
        action.accept(cashBoxAccounts);
    }
}
```

### Test Approach: The Test Plan

We want to test if a customer can always close their account and receive their expected balance, regardless of the sequence of deposits and withdrawals, provided they don't overdraw.

A good way to model this is a recursive `TestPlan` that builds a chain of operations:

```java
class TestPlan {
    // ... fields for accountId, availableToDeposit, targetToWithdraw, and the current step ...

    public Trials<TestPlan> extend() {
        if (0 == targetToWithdraw) {
            return api().only(opening());
        }

        List<Trials<TestPlan>> choices = new ArrayList<>();

        if (2 <= availableToDeposit) {
            choices.add(api().integers(1, availableToDeposit - 1)
                             .map(this::deposition));
        }

        if (1 <= targetToWithdraw) {
            choices.add(api().integers(1, targetToWithdraw)
                             .map(this::withdrawal));
        }

        return api().alternate(choices)
                    .flatMap(TestPlan::extend);
    }

    void executeUsing(Bank bank) {
        // ... executes the chain of transactions ...
    }
}
```

---

## The Issue: Invalid Operations

When we run this test, we might see failures like this:
```
Insufficient funds in account 5120f880... for requested amount: 9. Current balance is: 1.
```

This isn't necessarily a bug in the `Bank`—it's a **faulty test plan**. Our `TestPlan` generator allows splitting deposits and withdrawals in arbitrary ways. Even if they total up correctly by the end, a withdrawal might be attempted before enough money has been deposited.

We could try to make the generator smarter to avoid overdrawing, but for complex systems, that often means duplicating the system's logic in the test.

Instead, Americium allows us to **reject** test cases that violate preconditions during execution.

---

## Solution 1: `Trials.reject()`

When a test case hits a valid "business rule" failure that isn't the bug you're looking for, you can reject it:

```java
testPlans.withLimit(100).supplyTo(testPlan -> {
    try {
        testPlan.executeUsing(new Bank());
    } catch (CashBoxAccounts.InsufficientFunds e) {
        Trials.reject();  // ← Abort this trial, try another
    }
});
```

### How `reject()` Works

When `Trials.reject()` is called:
1. The current trial is **immediately aborted** via a control-flow exception.
2. No failure is recorded for this case.
3. Americium **generates a new test case** and continues.
4. The rejected trial still counts against your `casesLimit`.

---

## Solution 2: `Trials.whenever()`

A more elegant approach is to **guard code blocks** with preconditions. This is especially useful if you can check the state before performing the operation:

```java
// Logic inside a test plan step
Trials.whenever(
    cashBoxAccounts.getBalance(accountId) >= requestedAmount,
    () -> cashBoxAccounts.withdraw(accountId, requestedAmount)
);
```

### How `whenever()` Works
```java
Trials.whenever(guardCondition, () -> {
    // Code that requires guardCondition
});
```

- If `guardCondition` is **true** → The code block is executed.
- If `guardCondition` is **false** → `Trials.reject()` is called automatically.

It's syntactic sugar that makes the intent of preconditions clear.

---

## When to Use Rejection

### ✅ Good Use Cases

- **Stateful testing**: When a sequence of operations depends on the results of previous ones.
- **Precondition checking**: When it's easier to check a condition at runtime than to build it into the generator.
- **Handling rare edge cases**: Skipping specific combinations of state and input that are known to be invalid.

---

### ❌ When NOT to Use Rejection

{: .warning-title }
> **Avoid high rejection rates!**

If your generator produces invalid cases most of the time, your test will be very slow and may "starve."

```java
// BAD: Only ~7% of numbers are prime!
api().integers(1, 1000).withLimit(100).supplyTo(n -> {
    Trials.whenever(isPrime(n), () -> { ... });
});
```

Instead, **prefer direct generation**:
```java
// GOOD: Generate from a known set of primes
api().choose(2, 3, 5, 7, 11, ...).withLimit(100).supplyTo(n -> { ... });
```

---

## Rejection vs Filtering

| `.filter()` | `Trials.reject()` / `.whenever()` |
|:---|:---|
| Applied **during generation**. | Applied **during test execution**. |
| Best for static data properties. | Best for dynamic runtime state. |
| More efficient (avoids building the case). | Flexible (can access the system under test). |

---

## A Lurking Bug

Remember the bug mentioned in the `Bank` implementation?

```java
public int close(UUID accountId) throws AccountDoesNotExist {
    return Optional.of(accountBalances.get(accountId))
                   .orElseThrow(() -> new AccountDoesNotExist(accountId));
    // BUG: Missing accountBalances.remove(accountId);
}
```

Our current `TestPlan` uses a `UUID.randomUUID()` for each plan, so we never see the same ID twice. This is why we haven't caught the bug yet!

To find it, we would need to modify our generator to occasionally **reuse account IDs** across different lifecycles. Once we do that, we'll see that opening an account fails if it was previously "closed" but not removed from the bank's internal map.

---

## Summary

- **`Trials.reject()`** allows a test to abandon a trial that violates preconditions.
- **`Trials.whenever()`** provides a clean way to guard code blocks with these preconditions.
- Use these tools to handle the "awkward" parts of stateful systems where the test data and system state must be synchronized.
- Always monitor your rejection rates to ensure your tests remain efficient.
