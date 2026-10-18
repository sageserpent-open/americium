---
layout: default
title: "Awkward tests"
parent: Wiki Content
nav_order: 8
---

# Awkward tests
{: .no_toc }

Sometimes the test doesn't even want to run itself
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Let's earn our bonuses and do some high-end financial software implementation:

```java
interface CashBoxAccounts {
    class AccountAlreadyExists extends RuntimeException {
        private UUID accountId;

        public AccountAlreadyExists(UUID accountId) {

            this.accountId = accountId;
        }

        @Override
        public String toString() {
            return String.format("Account already exists for %s.",
                                 accountId);
        }
    }

    class AccountDoesNotExist extends RuntimeException {
        private UUID accountId;

        public AccountDoesNotExist(UUID accountId) {

            this.accountId = accountId;
        }

        @Override
        public String toString() {
            return String.format("Account does not exist for %s.",
                                 accountId);
        }
    }

    class InsufficientFunds extends RuntimeException {
        private UUID accountId;
        private int requestedAmount;
        private int currentBalance;

        public InsufficientFunds(UUID accountId, int requestedAmount,
                                 int currentBalance) {
            this.accountId = accountId;
            this.requestedAmount = requestedAmount;
            this.currentBalance = currentBalance;
        }

        @Override
        public String toString() {
            return String.format(
                    "Insufficient funds in account %s for requested" +
                    " amount: %s. Current balance is: %s.",
                    accountId,
                    requestedAmount,
                    currentBalance);
        }
    }

    /**
     * Opens an account.
     * @throws {@link CashBoxAccounts.AccountAlreadyExists} if there is
     * an existing account using {@code accountId}.
     * @apiNote {@code cash} must be positive.
     */
    void open(UUID accountId, int cash) throws AccountAlreadyExists;

    /**
     * Deposit cash in an account.
     * @throws {@link CashBoxAccounts.AccountDoesNotExist} if there is
     * no existing account using {@code accountId}.
     * @apiNote {@code cash} must be positive.
     */
    void deposit(UUID accountId, int cash) throws AccountDoesNotExist;

    /**
     * Attempt to withdraw cash from an account.
     * @throws InsufficientFunds if the current balance does not cover
     * {@code requestedAmount}.
     * @throws {@link CashBoxAccounts.AccountDoesNotExist} if there is
     * no existing account using {@code accountId}.
     * @apiNote {@code requestedAmount} must be positive.
     */
    void withdraw(UUID accountId, int requestedAmount)
            throws AccountDoesNotExist, InsufficientFunds;

    /**
     * Closes the account, forgetting {@code accountId}
     * @param accountId
     * @throws {@link CashBoxAccounts.AccountDoesNotExist} if there is
     * no existing account using {@code accountId}.
     * @return The final balance.
     */
    int close(UUID accountId) throws AccountDoesNotExist;
}

public class Bank {
    private final Map<UUID, Integer> accountBalances = new HashMap<>();

    /**
     * Execute a transaction, supplying a {@link CashBoxAccounts} that
     * is valid for the duration of the transaction.
     * @apiNote Commits the effects made on the {@link CashBoxAccounts}
     * on successful completion of {@code action}. Any exception thrown
     * by {@code action} rolls the effects back.
     * @implNote Accounts may earn interest and log a transaction
     * history, so the implementation should have some notion of time to
     * mark the effects of a transaction. This is presumably UTC.
     * @param action
     */
    public void transaction(Consumer<CashBoxAccounts> action) {
        final CashBoxAccounts cashBoxAccounts =
                new CashBoxAccounts() {
                    @Override
                    public void open(UUID accountId, int cash)
                            throws AccountAlreadyExists {
                        Preconditions.checkArgument(0 < cash);
                        if (null !=
                            accountBalances.putIfAbsent(accountId, cash)) {
                            throw new AccountAlreadyExists(accountId);
                        }
                    }

                    @Override
                    public void deposit(UUID accountId, int cash)
                            throws AccountDoesNotExist {
                        Preconditions.checkArgument(0 < cash);
                        if (null == accountBalances.computeIfPresent(
                                accountId,
                                (unused, balance) -> cash + balance)) {
                            throw new AccountDoesNotExist(accountId);
                        }
                    }

                    @Override
                    public void withdraw(UUID accountId,
                                         int requestedAmount)
                            throws InsufficientFunds, AccountDoesNotExist {
                        Preconditions.checkArgument(0 < requestedAmount);
                        if (null == accountBalances.computeIfPresent(
                                accountId,
                                (unused, balance) -> {
                                    if (requestedAmount <= balance) {
                                        return balance - requestedAmount;
                                    } else throw new InsufficientFunds(
                                            accountId,
                                            requestedAmount,
                                            balance);
                                })) {
                            throw new AccountDoesNotExist(accountId);
                        }
                    }

                    @Override
                    public int close(UUID accountId)
                            throws AccountDoesNotExist {
                        return Optional
                                .of(accountBalances.get(accountId))
                                .orElseThrow(() -> new AccountDoesNotExist(
                                        accountId));
                    }
                };

        action.accept(cashBoxAccounts);
    }
}
```

What have we got here? A model of simple cash accounts - so no inter-account transfers, external payments, nostros and vostros. Lots of cash boxes, with UUIDs to distinguish them. How cash is deposited or withdrawn can vary - might be at a bank counter, might be an ATM.

There is a transactional API that executes actions on the accounts; the interface of `CashBoxAccounts` is a passive one - it records the effect of an operation, but doesn't engage with other systems to realise the effect in the real world.

So when an ATM dispenses money, that is because some code in an action first called `.withdraw` and then instructed the ATM to dispense the cash as a following step in that action. If `.withdraw` fails, there is no effect and no cash is dispensed; if the cash can't be dispensed in the real world, the effect of the transaction on the account is rolled back. Note that our implementation here doesn't actually implement rollback yet, but we have to start somewhere.

What's a good property to test at this point? If a cashbox account doesn't allow an overdraft and there are no transaction charges, then we would expect the customer to be able to close the account at any time regardless of what happened before.

We are trying to come up with a property that is not simply a replication of the internal logic of the system under test, so we don't have the test plan keep track of deposits and withdrawals, as that would be simply replicating logic we're testing in the test itself. In fact, we won't in general actually know exactly how much money should be in the account because of interest, although that isn't baked into the implementation yet.

A test case for this is a plan consisting of steps - the plan will execute the steps that model an account's lifecycle from being opened to being closed and then apply expectations checking. This is a common pattern for testing stateful systems, and is accommodated nicely by Americium. Let's start with a test for just one account - we have a lump sum of cash that is distributed in separate deposits; we also attempt to withdraw sums of cash up to the lump sum in value. So all in all, we should have either no money at all when we close the account, or perhaps a positive amount due to interest.

This is a big one:

```java
class TestPlan {
    TestPlan(UUID accountId, int availableToDeposit,
             int targetToWithdraw, Step step) {
        this.accountId = accountId;
        this.availableToDeposit = availableToDeposit;
        this.targetToWithdraw = targetToWithdraw;
        this.step = step;
    }

    static class Step {
        final Consumer<CashBoxAccounts> action;
        final Optional<Step> followingStep;

        Step(Consumer<CashBoxAccounts> action,
             Optional<TestPlan.Step> followingStep) {
            this.action = action;
            this.followingStep = followingStep;
        }

        void executeUsing(Bank bank) {
            bank.transaction(action);

            // This is costly in terms of stack space, but is only test
            // code, so....
            followingStep.ifPresent(value -> value.executeUsing(bank));
        }
    }

    private Step followingStep(Consumer<CashBoxAccounts> action) {
        return new Step(action, Optional.of(step));
    }

    private final UUID accountId;

    private final int availableToDeposit;

    private final int targetToWithdraw;

    private final Step step;

    public static TestPlan closure(UUID accountId,
                                   int lumpSum,
                                   int expectedFinalBalance) {
        Preconditions.checkArgument(expectedFinalBalance <= lumpSum);

        return new TestPlan(accountId,
                            lumpSum,
                            lumpSum - expectedFinalBalance,
                            new Step(cashBoxAccounts -> {
                                final int closingBalance =
                                        cashBoxAccounts.close(accountId);
                                assertThat(closingBalance,
                                           greaterThanOrEqualTo(
                                                   expectedFinalBalance));
                                System.out.format(
                                        "Closed and validated," +
                                        " expected final balance is %d," +
                                        " closing balance is: %d.\n",
                                        expectedFinalBalance,
                                        closingBalance);
                            }, Optional.empty()));
    }

    public TestPlan deposition(int cash) {
        Preconditions.checkArgument(1 <= availableToDeposit);

        return new TestPlan(accountId,
                            availableToDeposit - cash,
                            targetToWithdraw,
                            followingStep(cashBoxAccounts -> {
                                cashBoxAccounts.deposit(accountId, cash);
                                System.out.println("Deposited.");
                            }));
    }

    public TestPlan withdrawal(int requestedAmount) {
        Preconditions.checkArgument(1 <= targetToWithdraw);

        return new TestPlan(accountId,
                            availableToDeposit,
                            targetToWithdraw - requestedAmount,
                            followingStep(cashBoxAccounts -> {
                                cashBoxAccounts.withdraw(accountId,
                                                         requestedAmount);
                                System.out.println("Withdrawn.");
                            }));
    }

    public TestPlan opening() {
        Preconditions.checkArgument(0 == targetToWithdraw);

        return new TestPlan(accountId,
                            0,
                            0,
                            followingStep(cashBoxAccounts -> {
                                cashBoxAccounts.open(accountId,
                                                     availableToDeposit);
                                System.out.println("Opened.");
                            }));
    }

    public Trials<TestPlan> extend() {
        if (0 == targetToWithdraw) {
            return api().only(opening());
        }

        final List<Trials<TestPlan>> choices =
                new LinkedList();

        if (2 <= availableToDeposit) {
            choices.add(api().integers(1, availableToDeposit - 1)
                             .map(this::deposition));
        }

        if (1 <= targetToWithdraw) {
            choices.add(api().integers(1, targetToWithdraw)
                             .map(this::withdrawal));
        }

        return api()
                .alternate(choices)
                .flatMap(plan -> plan.extend());
    }

    public void executeUsing(Bank bank) {
        step.executeUsing(bank);
    }
}


final Trials<TestPlan> testPlans = api()
        .integers(1, 1000)
        .flatMap(lumpSum -> api()
                .integers(1, lumpSum)
                .flatMap(expectedFinalBalance -> TestPlan
                        .closure(UUID.randomUUID(),
                                 lumpSum,
                                 expectedFinalBalance)
                        .extend()));

try {
    testPlans.withLimit(10).supplyTo(testPlan -> {
        try {
            testPlan.executeUsing(new Bank());
        } catch (Throwable t) {
            System.out.println(t);
            throw t;
        }
    });
} catch (TrialsFactoring.TrialException exception) {
    System.out.println(exception);
}
```

What do we see?

```
Opened.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 32, closing balance is: 32.
Opened.
Insufficient funds in account 5120f880-6d9b-4e58-bffa-2599e00e14fe for requested amount: 9. Current balance is: 1.
Opened.
Withdrawn.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 211, closing balance is: 211.
Opened.
Withdrawn.
Insufficient funds in account c1b7b523-3a39-45d9-83bd-190ab715e748 for requested amount: 10. Current balance is: 3.

etc...
```

This is awkward - it seems that most of the time, the trials are passing, but every now and then we have a badly constructed test plan that tries to overdraw from the account, so the _test itself is causing a fault_.

In our haste to avoid duplicating the logic of the system under test, we have allowed test plans to split the deposits and withdrawals in arbitrary ways - although they both total to the lump sum, there is no coordination between the two, so a test plan's step can try to overdraw partway through the account's lifecycle.

Let's allow the test to reject bad test cases; this aborts the currently executing trial, but allows supply of further test cases to proceed as usual:

```java
try {
    testPlans.withLimit(10).supplyTo(testPlan -> {
        try {
            testPlan.executeUsing(new Bank());
        } catch (CashBoxAccounts.InsufficientFunds e) {
            Trials.reject();
        } catch (Throwable t) {
            System.out.println(t);
            throw t;
        }
    });
} catch (TrialsFactoring.TrialException exception) {
    System.out.println(exception);
}
```
Now we see 10 trials with validated account lifecycles:

```
Opened.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 32, closing balance is: 32.
Opened.
Opened.
Withdrawn.
Opened.
Withdrawn.
Closed and validated, expected final balance is 293, closing balance is: 293.
Opened.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 589, closing balance is: 589.
Opened.
Withdrawn.
Opened.
Withdrawn.
Deposited.
Opened.
Withdrawn.
Opened.
Withdrawn.
Opened.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Closed and validated, expected final balance is 109, closing balance is: 109.
Opened.
Withdrawn.
Opened.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Opened.
Withdrawn.
Opened.
Withdrawn.
Opened.
Withdrawn.
Opened.
Withdrawn.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 440, closing balance is: 440.
Opened.
Withdrawn.
Opened.
Withdrawn.
Withdrawn.
Opened.
Opened.
Withdrawn.
Withdrawn.
Withdrawn.
Closed and validated, expected final balance is 681, closing balance is: 681.
Opened.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Deposited.
Closed and validated, expected final balance is 69, closing balance is: 69.
Opened.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Deposited.
Closed and validated, expected final balance is 70, closing balance is: 70.
Opened.
Withdrawn.
Deposited.
Withdrawn.
Opened.
Withdrawn.
Deposited.
Deposited.
Deposited.
Deposited.
Closed and validated, expected final balance is 128, closing balance is: 128.
Opened.
Withdrawn.
Opened.
Withdrawn.
Opened.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Withdrawn.
Withdrawn.
Withdrawn.
Deposited.
Withdrawn.
Withdrawn.
Deposited.
Deposited.
Withdrawn.
Closed and validated, expected final balance is 130, closing balance is: 130.
```

We see how rejection of a test case acts as a kind of last chance filtration in the test itself; Americium treats the rejected test case in the same way as it would a test case that didn't pass `.filter`.

Hopefully it goes without saying that we shouldn't be too casual with rejection - it is possible that the `CashBoxAccounts.InsufficientFunds` exception might be thrown because of a genuine bug in the implementation. If a test yields nothing but rejections, this may be a sign that there is a genuine problem going on, either in the test itself or what it's testing!

There is a variation on this technique supported by `Trials.whenever` - the idea here is to evaluate a guard precondition and depending on whether the precondition holds, execute some guarded test code, or reject the test case:

```java
Test.whenever(<some Boolean guard precondition>, () -> {<block of guarded test code>});
```

This wraps `Trials.reject` under the hood, but reads nicely in situations where it's easy to evaluate the precondition before actually hitting the 'real' test code.

Before we move on, did you notice that there is a potential bug lurking in the `Bank` implementation? It certainly passes the test, but maybe we should write an extra test. (HINT: what would happen if we changed the test plans to allow two back-to-back lifecycles sharing the same account ID?)

***
Next topic: [JUnit5 Integration...]({% link docs/wiki-content/junit5-integration.md %})