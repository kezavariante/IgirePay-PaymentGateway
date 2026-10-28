
package com.igirepay;

import java.math.BigDecimal;

public class SavingsAccount extends Account {

    private static final BigDecimal MINIMUM_BALANCE =
            new BigDecimal("5000.00");

    private static final BigDecimal MAXIMUM_WITHDRAWAL =
            new BigDecimal("10000.00");

    public SavingsAccount(int accountID, String accountNumber,
                          BigDecimal balance, int customerID) {
        super(accountID, accountNumber, balance, customerID, "SAVINGS");
    }

    @Override
    public BigDecimal withdraw(BigDecimal amount) {


        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be greater than zero");
        }


        if (amount.compareTo(MAXIMUM_WITHDRAWAL) > 0) {
            throw new IllegalArgumentException(
                    "Maximum savings withdrawal is 10000 RWF");
        }

        BigDecimal remainingBalance =
                getBalance().subtract(amount);

        if (remainingBalance.compareTo(MINIMUM_BALANCE) < 0) {
            throw new IllegalArgumentException(
                    "Savings account must retain at least 5000 RWF");
        }


        return super.withdraw(amount);
    }

    @Override
    public String toString() {
        return "Savings Account " + super.toString();
    }
}
