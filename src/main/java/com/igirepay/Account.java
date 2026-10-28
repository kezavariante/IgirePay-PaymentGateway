
package com.igirepay;

import java.math.BigDecimal;

public class Account {
    private int accountID;
    private String accountNumber;
    private BigDecimal balance;
    private int customerID;
    private String accountType;


    public Account(int accountID, String accountNumber,
                   BigDecimal balance, int customerID) {
        this(accountID, accountNumber, balance, customerID, "WALLET");
    }


    public Account(int accountID, String accountNumber,
                   BigDecimal balance, int customerID,
                   String accountType) {
        setAccountID(accountID);
        setAccountNumber(accountNumber);
        setBalance(balance);
        setCustomerID(customerID);
        setAccountType(accountType);
    }

    public int getAccountID() {
        return accountID;
    }

    public void setAccountID(int accountID) {
        if (accountID <= 0) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        this.accountID = accountID;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Account number is required");
        }
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        if (balance == null
                || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid balance");
        }
        this.balance = balance;
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        if (customerID <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }
        this.customerID = customerID;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        if (accountType == null
                || (!accountType.equalsIgnoreCase("WALLET")
                && !accountType.equalsIgnoreCase("SAVINGS"))) {
            throw new IllegalArgumentException(
                    "Account type must be WALLET or SAVINGS");
        }

        this.accountType = accountType.toUpperCase();
    }

    public BigDecimal deposit(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than zero");
        }

        balance = balance.add(amount);
        return balance;
    }

    public BigDecimal withdraw(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be greater than zero");
        }

        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        balance = balance.subtract(amount);
        return balance;
    }

    public void processTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Transaction is required");
        }

        String type = transaction.getTransactionType();

        if ("DEPOSIT".equalsIgnoreCase(type)) {
            deposit(transaction.getAmount());
        } else if ("WITHDRAW".equalsIgnoreCase(type)) {
            withdraw(transaction.getAmount());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction type: " + type);
        }
    }

    @Override
    public String toString() {
        return "Account ID: " + accountID
                + ", Account Number: " + accountNumber
                + ", Account Type: " + accountType
                + ", Balance: " + balance
                + ", Customer ID: " + customerID;
    }
}
