package com.sageserpent.americium.junit5.java.examples;

import java.util.UUID;

interface CashBoxAccounts {
    class AccountAlreadyExists extends RuntimeException {
        private final UUID accountId;

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
        private final UUID accountId;

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
        private final UUID accountId;
        private final int requestedAmount;
        private final int currentBalance;

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
     *
     * @throws {@link CashBoxAccounts.AccountAlreadyExists} if there is
     *                an existing account using {@code accountId}.
     * @apiNote {@code cash} must be positive.
     */
    void open(UUID accountId, int cash) throws AccountAlreadyExists;

    /**
     * Deposit cash in an account.
     *
     * @throws {@link CashBoxAccounts.AccountDoesNotExist} if there is
     *                no existing account using {@code accountId}.
     * @apiNote {@code cash} must be positive.
     */
    void deposit(UUID accountId, int cash) throws AccountDoesNotExist;

    /**
     * Attempt to withdraw cash from an account.
     *
     * @throws InsufficientFunds if the current balance does not cover
     *                           {@code requestedAmount}.
     * @throws {@link            CashBoxAccounts.AccountDoesNotExist} if
     * there is
     *                           no existing account using {@code accountId}.
     * @apiNote {@code requestedAmount} must be positive.
     */
    void withdraw(UUID accountId, int requestedAmount)
            throws AccountDoesNotExist, InsufficientFunds;

    /**
     * Closes the account, forgetting {@code accountId}
     *
     * @param accountId
     * @return The final balance.
     * @throws {@link CashBoxAccounts.AccountDoesNotExist} if there is
     *                no existing account using {@code accountId}.
     */
    int close(UUID accountId) throws AccountDoesNotExist;
}