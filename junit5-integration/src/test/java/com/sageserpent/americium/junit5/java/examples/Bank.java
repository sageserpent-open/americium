package com.sageserpent.americium.junit5.java.examples;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class Bank {
    private final Map<UUID, Integer> accountBalances = new HashMap<>();

    /**
     * Execute a transaction, supplying a {@link CashBoxAccounts} that
     * is valid for the duration of the transaction.
     *
     * @param action
     * @apiNote Commits the effects made on the {@link CashBoxAccounts}
     * on successful completion of {@code action}. Any exception thrown
     * by {@code action} rolls the effects back.
     * @implNote Accounts may earn interest and log a transaction
     * history, so the implementation should have some notion of time to
     * mark the effects of a transaction. This is presumably UTC.
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