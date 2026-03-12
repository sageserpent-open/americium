package com.sageserpent.americium.junit5.java.examples;

import com.google.common.base.Preconditions;
import com.sageserpent.americium.java.Trials;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.sageserpent.americium.java.Trials.api;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class AwkwardTest {
    static class TestPlan {
        TestPlan(UUID accountId, int availableToDeposit,
                 int targetToWithdraw, Step step) {
            this.accountId = accountId;
            this.availableToDeposit = availableToDeposit;
            this.targetToWithdraw = targetToWithdraw;
            this.step = step;
        }

        record Step(Consumer<CashBoxAccounts> action,
                    Optional<Step> followingStep) {

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

    @Disabled
    @Test
    void breakTheBank() {
        final Trials<TestPlan> testPlans = api()
                .integers(1, 1000)
                .flatMap(lumpSum -> api()
                        .integers(1, lumpSum)
                        .flatMap(expectedFinalBalance -> TestPlan
                                .closure(UUID.randomUUID(),
                                         lumpSum,
                                         expectedFinalBalance)
                                .extend()));


        testPlans.withLimit(10).supplyTo(testPlan -> {
            try {
                System.out.println("*** Executing test plan ***");
                testPlan.executeUsing(new Bank());
            } catch (Throwable t) {
                System.out.println(t);
                throw t;
            }
        });
    }
}
