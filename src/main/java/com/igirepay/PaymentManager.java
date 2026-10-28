
package com.igirepay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaymentManager {
    private Map<Integer, List<Account>> customerAccounts = new HashMap<>();
    private List<Transaction> transactionHistory = new ArrayList<>();
    private Set<String> processedReferenceIDs = new HashSet<>();
    private List<String> failedTransactionLogs = new ArrayList<>();

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }

        customerAccounts
                .computeIfAbsent(account.getCustomerID(), id -> new ArrayList<>())
                .add(account);
    }

    public void processTransaction(Account account, Transaction transaction) {
        if (account == null || transaction == null) {
            throw new IllegalArgumentException(
                    "Account and transaction are required");
        }

        String referenceID = transaction.getReferenceID();

        if (processedReferenceIDs.contains(referenceID)) {
            failedTransactionLogs.add(
                    "Duplicate reference ID: " + referenceID);

            throw new IllegalArgumentException(
                    "This transaction has already been processed");
        }

        try {
            account.processTransaction(transaction);

            processedReferenceIDs.add(referenceID);
            transactionHistory.add(transaction);

        } catch (RuntimeException e) {
            failedTransactionLogs.add(
                    referenceID + ": " + e.getMessage());
            throw e;
        }
    }

    public Map<Integer, List<Account>> getCustomerAccounts() {
        return customerAccounts;
    }

    public List<Transaction> getTransactionHistory() {
        return transactionHistory;
    }

    public Set<String> getProcessedReferenceIDs() {
        return processedReferenceIDs;
    }

    public List<String> getFailedTransactionLogs() {
        return failedTransactionLogs;
    }
}
