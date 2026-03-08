---
layout: default
title: Awkward Tests
parent: Core Concepts
nav_order: 5
---

# Awkward Tests
{: .no_toc }

Handling preconditions with `Trials.reject()` and `Trials.whenever()`
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

{% include disclaimer.html %}

---

## The Problem: Stateful Systems

Sometimes you're testing a **stateful system** where test cases represent sequences of operations. Not all sequences make sense - some violate preconditions or invariants.

Consider testing a banking system where operations can fail for perfectly valid reasons (insufficient funds). How do we handle this?

---

## Example: Bank Account Testing

Let's test a simple cash box accounting system:
```java
import com.sageserpent.americium.java.CashBoxAccounts;

// Generate random account operations
final Trials<ImmutableList<CashBoxAccounts.OperationId>> testPlans = 
    api().uniqueIds()
        .map(CashBoxAccounts.OperationId::new)
        .immutableLists();

testPlans.withLimit(100).supplyTo(plan -> {
    final CashBoxAccounts cashBoxAccounts = new CashBoxAccounts();
    
    for (final CashBoxAccounts.OperationId operationId : plan) {
        // Randomly choose an operation type
        final int operationType = random.nextInt(4);
        
        switch (operationType) {
            case 0: // Open account
                cashBoxAccounts.open(operationId);
                break;
            case 1: // Deposit
                cashBoxAccounts.deposit(operationId, randomAmount());
                break;
            case 2: // Withdrawal  
                cashBoxAccounts.withdrawal(operationId, randomAmount());
                break;
            case 3: // Close account
                cashBoxAccounts.close(operationId);
                break;
        }
    }
    
    // Verify invariants
    assertThat(cashBoxAccounts.balance(), greaterThanOrEqualTo(0));
});
```

---

## The Issue: Invalid Operations

This test will **fail frequently** - but not because the system is buggy!

Invalid sequences like:
- Withdrawing from a closed account
- Depositing to a non-existent account
- Withdrawing more than the balance

These throw exceptions - but they're **correct behavior**, not bugs. Our **test plan is faulty**, not the system.

We could handle this with try-catch:
```java
try {
    cashBoxAccounts.withdrawal(operationId, amount);
} catch (CashBoxAccounts.InsufficientFunds e) {
    // Ignore - this is valid behavior
}
```

But this feels wrong - we're **swallowing exceptions** and the test continues with a partially invalid state.

---

## Solution 1: `Trials.reject()`

When a test case doesn't meet preconditions, **reject it entirely** and move to the next:
```java
testPlans.withLimit(100).supplyTo(plan -> {
    final CashBoxAccounts cashBoxAccounts = new CashBoxAccounts();
    
    for (final CashBoxAccounts.OperationId operationId : plan) {
        final int operationType = random.nextInt(4);
        
        try {
            switch (operationType) {
                case 0:
                    cashBoxAccounts.open(operationId);
                    break;
                case 1:
                    cashBoxAccounts.deposit(operationId, randomAmount());
                    break;
                case 2:
                    cashBoxAccounts.withdrawal(operationId, randomAmount());
                    break;
                case 3:
                    cashBoxAccounts.close(operationId);
                    break;
            }
        } catch (CashBoxAccounts.InsufficientFunds e) {
            Trials.reject();  // ← Abort this trial, try another
        }
    }
    
    // Only valid sequences reach here
    assertThat(cashBoxAccounts.balance(), greaterThanOrEqualTo(0));
});
```

### How `reject()` Works

When `Trials.reject()` is called:
1. Current trial is **immediately aborted**
2. No failure is recorded
3. Americium **generates a new test case** and tries again
4. Counts against your case limit

{: .note }
> Rejected trials still consume your limit! If you have `.withLimit(100)` and reject 30 trials, you'll only run 70 successful trials.

---

## Solution 2: `Trials.whenever()`

A more elegant approach - **guard code blocks** with preconditions:
```java
testPlans.withLimit(100).supplyTo(plan -> {
    final CashBoxAccounts cashBoxAccounts = new CashBoxAccounts();
    
    for (final CashBoxAccounts.OperationId operationId : plan) {
        final int operationType = random.nextInt(4);
        
        switch (operationType) {
            case 0:
                cashBoxAccounts.open(operationId);
                break;
            case 1:
                Trials.whenever(
                    cashBoxAccounts.isOpen(operationId),
                    () -> cashBoxAccounts.deposit(operationId, randomAmount())
                );
                break;
            case 2:
                Trials.whenever(
                    cashBoxAccounts.canWithdraw(operationId, randomAmount()),
                    () -> cashBoxAccounts.withdrawal(operationId, randomAmount())
                );
                break;
            case 3:
                cashBoxAccounts.close(operationId);
                break;
        }
    }
    
    assertThat(cashBoxAccounts.balance(), greaterThanOrEqualTo(0));
});
```

### How `whenever()` Works
```java
Trials.whenever(guardCondition, () -> {
    // Code that requires guardCondition
});
```

- If `guardCondition` is **true** → Execute the code block
- If `guardCondition` is **false** → Call `Trials.reject()` automatically

It's just **syntactic sugar**, but much cleaner!

---

## When to Use Rejection

### ✅ Good Use Cases

**Filtering complex state:**
```java
Trials.whenever(systemInValidState(), () -> {
    performOperation();
});
```

**Precondition checking:**
```java
Trials.whenever(account.isOpen() && balance > 0, () -> {
    account.withdraw(amount);
});
```

**Avoiding edge cases temporarily:**
```java
// While debugging, skip problematic states
Trials.whenever(!isProblematicState(state), () -> {
    testSystemUnderTest(state);
});
```

---

### ❌ When NOT to Use Rejection

{: .warning-title }
> **Don't be casual with rejection!**

**High rejection rates** can indicate problems:
```java
// BAD: This will reject most cases!
api().integers(1, 1000000).withLimit(100).supplyTo(n -> {
    Trials.whenever(isPrime(n), () -> {  // Only ~7% of numbers are prime!
        testWithPrime(n);
    });
});
```

If you're rejecting >50% of cases, you're doing it wrong. Generate valid cases **directly** instead:
```java
// GOOD: Generate primes directly
final Trials<Integer> primes = 
    api().choose(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, ...);

primes.withLimit(100).supplyTo(prime -> {
    testWithPrime(prime);  // No rejection needed!
});
```

---

### Starvation Detection

Remember the **starvation ratio** from Configuration?
```java
trials.withStrategy(cycle -> 
    CasesLimitStrategy.counted(100, 0.2)  // ← 20% rejection tolerance
);
```

If more than 20% of trials are rejected, Americium will warn you about **starvation** - you're not getting enough valid test cases.

---

## Rejection vs Filtering

You might be wondering: how is `Trials.reject()` different from `.filter()`?
```java
// Using .filter()
trials
    .filter(isValid)
    .withLimit(100)
    .supplyTo(testCase -> { ... });

// Using Trials.reject()  
trials.withLimit(100).supplyTo(testCase -> {
    Trials.whenever(isValid(testCase), () -> {
        // Test code
    });
});
```

**Key differences:**

| `.filter()` | `Trials.reject()` |
|------------|-------------------|
| Filters **before** generating complex structures | Filters **during** test execution |
| Can't access runtime state | Can check runtime state |
| More efficient for simple predicates | Better for stateful/sequential tests |
| Applied at generation time | Applied at test time |

**Use `.filter()` when:**
- Predicate is based on the test case alone
- No runtime state needed
- Simple conditions

**Use `Trials.reject()` when:**
- Need to check runtime state
- Testing sequences of operations
- Preconditions depend on accumulated state

---

## A Lurking Bug

Our `CashBoxAccounts` example actually has a **subtle bug** that we haven't caught yet!

Look at the `close()` implementation:
```java
public void close(OperationId operationId) {
    if (!accounts.containsKey(operationId)) {
        throw new IllegalArgumentException("Account does not exist");
    }
    
    Account account = accounts.get(operationId);
    
    if (account.isClosed()) {
        throw new IllegalStateException("Account already closed");
    }
    
    account.setClosed(true);
    // BUG: Doesn't remove from map!
}
```

The account is marked closed but **not removed from the map**.

If we try to open an account with the **same ID twice**, the second open will fail because the ID already exists in the map (even though it's "closed").

This is why our test uses `api().uniqueIds()` - it ensures unique operation IDs. But in real systems, IDs might be reused after accounts close!

{: .tip }
> **Exercise:** Modify the test to reuse operation IDs and catch this bug. You'll need to handle the two distinct "lifecycles" of an account with the same ID.

---

## Best Practices

### 1. Prefer Direct Generation
```java
// ❌ Bad: Generate then filter heavily
api().integers(1, 1000).withLimit(100).supplyTo(n -> {
    Trials.whenever(isSpecialCase(n), () -> { ... });
});

// ✅ Good: Generate valid cases directly
api().choose(getSpecialCases()).withLimit(100).supplyTo(n -> {
    // All cases are valid!
});
```

### 2. Use Rejection for State, Filtering for Data
```java
// ✅ Good: Filter data properties
trials
    .filter(data -> data.isValid())
    .withLimit(100)
    .supplyTo(data -> { ... });

// ✅ Good: Reject based on accumulated state
trials.withLimit(100).supplyTo(data -> {
    Trials.whenever(system.canHandle(data), () -> {
        system.process(data);
    });
});
```

### 3. Monitor Rejection Rates
```java
// Set a starvation ratio to detect problems early
trials.withStrategy(cycle -> 
    CasesLimitStrategy.counted(100, 0.1)  // Max 10% rejection
);
```

### 4. Document Why You're Rejecting
```java
Trials.whenever(account.isOpen(), () -> {
    // Rejection here is expected: closed accounts can't accept deposits
    account.deposit(amount);
});
```

---

## Summary Pattern

The typical pattern for testing stateful systems:
```java
final Trials<ImmutableList<Operation>> operationSequences = 
    generateOperations();

operationSequences.withLimit(100).supplyTo(operations -> {
    final StatefulSystem system = new StatefulSystem();
    
    for (Operation op : operations) {
        Trials.whenever(op.preconditionsMet(system), () -> {
            op.execute(system);
        });
    }
    
    // Verify invariants on valid sequences
    assertThat(system.isConsistent(), is(true));
});
```

---

{: .note-title }
> Key Takeaways
>
> - **`Trials.reject()`** - Abort current trial when preconditions aren't met
> - **`Trials.whenever(condition, code)`** - Syntactic sugar for guarded rejection
> - **Use for stateful testing** - When preconditions depend on runtime state
> - **Monitor rejection rates** - High rejection = inefficient test case generation
> - **Prefer direct generation** - Generate valid cases rather than filtering invalid ones
> - **Starvation ratio** - Configure tolerance for rejection
> - **`.filter()` vs `.reject()`** - Filter at generation time, reject at test time
> - Rejection counts against your case limit