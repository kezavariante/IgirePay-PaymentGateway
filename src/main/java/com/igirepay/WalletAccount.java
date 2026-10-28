
package com.igirepay;

import java.math.BigDecimal;

public class WalletAccount extends Account {

    public WalletAccount(int accountID, String accountNumber,
                         BigDecimal balance, int customerID) {
        super(accountID, accountNumber, balance, customerID, "WALLET");
    }

    @Override
    public void processTransaction(Transaction transaction) {
        super.processTransaction(transaction);
    }

    @Override
    public String toString() {

        return "Wallet Account: " + super.toString();
    }
}
