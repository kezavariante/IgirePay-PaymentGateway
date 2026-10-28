package com.igirepay;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        // 1. Create an account with an initial balance of 1000
        WalletAccount account = new WalletAccount(
                101,
                "440098762",
                new BigDecimal("1000.00"),
                1001
        );

        PaymentManager manager = new PaymentManager();
        manager.addAccount(account);

        System.out.println("========== IGIREPAY ==========");
        System.out.println("Initial balance: " + account.getBalance());


        // 2. Deposit 250
        System.out.println("\n========== DEPOSIT ==========");

        Transaction deposit = new Transaction(
                new BigDecimal("250.00"),
                "DEPOSIT"
        );

        try {
            manager.processTransaction(account, deposit);

            System.out.println("Transaction successful!");
            System.out.println(deposit);
            System.out.println("Balance after deposit: "
                    + account.getBalance());

        } catch (RuntimeException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }


        // 3. Withdraw 100
        System.out.println("\n========== WITHDRAWAL ==========");

        Transaction withdrawal = new Transaction(
                new BigDecimal("100.00"),
                "WITHDRAW"
        );

        try {
            manager.processTransaction(account, withdrawal);

            System.out.println("Transaction successful!");
            System.out.println(withdrawal);
            System.out.println("Balance after withdrawal: "
                    + account.getBalance());

        } catch (RuntimeException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }


        // 4. Try to reuse the successful deposit's reference ID
        System.out.println("\n========== DUPLICATE TEST ==========");

        Transaction duplicate = new Transaction(
                deposit.getReferenceID(),
                deposit.getAmount(),
                deposit.getTransactionType(),
                deposit.getTimestamp()
        );

        System.out.println("Reused reference ID: "
                + duplicate.getReferenceID());

        try {
            manager.processTransaction(account, duplicate);

            System.out.println("Transaction successful!");

        } catch (RuntimeException e) {
            System.out.println("Transaction rejected: "
                    + e.getMessage());
        }

        System.out.println("Balance after duplicate attempt: "
                + account.getBalance());


        // 5. Attempt a withdrawal that exceeds the balance
        System.out.println("\n========== FAILED WITHDRAWAL ==========");

        Transaction failedWithdrawal = new Transaction(
                new BigDecimal("5000.00"),
                "WITHDRAW"
        );

        String failedReference = failedWithdrawal.getReferenceID();

        try {
            manager.processTransaction(account, failedWithdrawal);

            System.out.println("Transaction successful!");

        } catch (RuntimeException e) {
            System.out.println("Transaction failed: "
                    + e.getMessage());

            System.out.println("Failed transaction: "
                    + failedWithdrawal);
        }

        System.out.println("Balance after failed withdrawal: "
                + account.getBalance());


        // 6. Deposit enough money to make the retry possible
        System.out.println("\n========== ADD FUNDS ==========");

        Transaction addFunds = new Transaction(
                new BigDecimal("5000.00"),
                "DEPOSIT"
        );

        try {
            manager.processTransaction(account, addFunds);

            System.out.println(addFunds);
            System.out.println("Balance after adding funds: "
                    + account.getBalance());

        } catch (RuntimeException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }


        // 7. Retry the failed withdrawal using its original reference
        System.out.println("\n========== RETRY FAILED TRANSACTION ==========");

        Transaction retry = new Transaction(
                failedReference,
                failedWithdrawal.getAmount(),
                failedWithdrawal.getTransactionType(),
                failedWithdrawal.getTimestamp()
        );

        System.out.println("Original reference: " + failedReference);
        System.out.println("Retry reference:    "
                + retry.getReferenceID());

        try {
            manager.processTransaction(account, retry);

            System.out.println("Retry successful!");
            System.out.println(retry);
            System.out.println("Balance after retry: "
                    + account.getBalance());

        } catch (RuntimeException e) {
            System.out.println("Retry failed: " + e.getMessage());
        }


        // 8. Display the final results
        System.out.println("\n========== FINAL RESULTS ==========");
        System.out.println("Final balance: " + account.getBalance());
        System.out.println("Successful transactions: "
                + manager.getTransactionHistory().size());
        System.out.println("Processed references: "
                + manager.getProcessedReferenceIDs().size());
        System.out.println("Failed transaction logs: "
                + manager.getFailedTransactionLogs().size());
    }
}
