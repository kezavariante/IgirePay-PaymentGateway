
package com.igirepay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private static int nextTransactionID = 1;

    private final int transactionID;
    private final String referenceID;
    private BigDecimal amount;
    private String transactionType;
    private LocalDateTime timestamp;


    public Transaction(BigDecimal amount, String transactionType) {
        this(nextTransactionID++,
                UUID.randomUUID().toString(),
                amount,
                transactionType,
                LocalDateTime.now());
    }

    public Transaction(String referenceID, BigDecimal amount,
                       String transactionType, LocalDateTime timestamp) {
        this(nextTransactionID++,
                referenceID,
                amount,
                transactionType,
                timestamp);
    }

    private Transaction(int transactionID, String referenceID,
                        BigDecimal amount, String transactionType,
                        LocalDateTime timestamp) {

        if (referenceID == null || referenceID.isBlank()) {
            throw new IllegalArgumentException(
                    "Reference ID is required");
        }

        this.transactionID = transactionID;
        this.referenceID = referenceID;

        setAmount(amount);
        setTransactionType(transactionType);
        setTimestamp(timestamp);
    }


    public int getTransactionID() {
        return transactionID;
    }

    public String getReferenceID() {
        return referenceID;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }


    public void setAmount(BigDecimal amount) {
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero");
        }

        this.amount = amount;
    }

    public void setTransactionType(String transactionType) {
        if (transactionType == null ||
                transactionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction type is required");
        }

        this.transactionType = transactionType;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp is required");
        }

        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return "Transaction ID: " + transactionID
                + ", Reference ID: " + referenceID
                + ", Amount: " + amount
                + ", Type: " + transactionType
                + ", Timestamp: " + timestamp;
    }
}
