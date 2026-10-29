package com.igirepay;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class PaymentService {

    public boolean processPayment(int accountID, Transaction transaction)
            throws SQLException {

        if (transaction == null || accountID <= 0) {
            throw new IllegalArgumentException("Invalid payment request");
        }

        String type = transaction.getTransactionType().toUpperCase(Locale.ROOT);
        BigDecimal amount = transaction.getAmount();
        String referenceID = transaction.getReferenceID();

        if (!type.equals("DEPOSIT") && !type.equals("WITHDRAW")) {
            throw new IllegalArgumentException("Unsupported transaction type");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Boolean previous = previousResult(connection, referenceID, type,
                        accountID, 0, amount);
                if (previous != null) {
                    connection.commit();
                    return previous;
                }

                String balanceSQL = type.equals("DEPOSIT")
                        ? "UPDATE accounts SET balance = balance + ? WHERE id = ?"
                        : "UPDATE accounts SET balance = balance - ? "
                        + "WHERE id = ? AND balance >= ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(balanceSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, accountID);
                    if (type.equals("WITHDRAW")) {
                        statement.setBigDecimal(3, amount);
                    }

                    if (statement.executeUpdate() == 0) {
                        markRequest(connection, referenceID, "FAILED",
                                "Account not found or insufficient balance");
                        connection.commit();
                        System.out.println("Account not found or insufficient balance");
                        return false;
                    }
                }

                insertTransaction(connection, accountID, referenceID, type, amount);
                markRequest(connection, referenceID, "SUCCEEDED",
                        type + " completed successfully");
                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean transfer(int sourceAccountID, int destinationAccountID,
                            BigDecimal amount, String referenceID)
            throws SQLException {

        if (sourceAccountID <= 0 || destinationAccountID <= 0) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        if (sourceAccountID == destinationAccountID) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (referenceID == null || referenceID.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Boolean previous = previousResult(connection, referenceID, "TRANSFER",
                        sourceAccountID, destinationAccountID, amount);
                if (previous != null) {
                    connection.commit();
                    return previous;
                }

                String lockSQL = "SELECT id, balance FROM accounts "
                        + "WHERE id IN (?, ?) ORDER BY id FOR UPDATE";

                boolean sourceFound = false;
                boolean destinationFound = false;
                BigDecimal sourceBalance = null;

                try (PreparedStatement statement = connection.prepareStatement(lockSQL)) {
                    statement.setInt(1, sourceAccountID);
                    statement.setInt(2, destinationAccountID);

                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            int id = result.getInt("id");
                            if (id == sourceAccountID) {
                                sourceFound = true;
                                sourceBalance = result.getBigDecimal("balance");
                            }
                            if (id == destinationAccountID) {
                                destinationFound = true;
                            }
                        }
                    }
                }

                if (!sourceFound || !destinationFound) {
                    markRequest(connection, referenceID, "FAILED",
                            "Source or destination account does not exist");
                    connection.commit();
                    System.out.println("Source or destination account does not exist");
                    return false;
                }

                if (sourceBalance.compareTo(amount) < 0) {
                    markRequest(connection, referenceID, "FAILED", "Insufficient balance");
                    connection.commit();
                    System.out.println("Insufficient balance");
                    return false;
                }

                String debitSQL = "UPDATE accounts SET balance = balance - ? "
                        + "WHERE id = ? AND balance >= ?";
                try (PreparedStatement statement = connection.prepareStatement(debitSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, sourceAccountID);
                    statement.setBigDecimal(3, amount);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Transfer debit failed");
                    }
                }

                String creditSQL = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
                try (PreparedStatement statement = connection.prepareStatement(creditSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, destinationAccountID);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Transfer credit failed");
                    }
                }

                insertTransaction(connection, sourceAccountID, referenceID, "TRANSFER", amount);
                insertTransaction(connection, destinationAccountID, referenceID, "TRANSFER", amount);
                markRequest(connection, referenceID, "SUCCEEDED",
                        "Transfer completed successfully");
                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private Boolean previousResult(Connection connection, String referenceID,
                                   String type, int accountID,
                                   int destinationAccountID, BigDecimal amount)
            throws SQLException {

        String insertSQL = destinationAccountID > 0
                ? "INSERT INTO processed_requests "
                + "(reference_id, status, transaction_type, account_id, "
                + "destination_account_id, amount) "
                + "VALUES (?, 'PROCESSING', ?, ?, ?, ?) "
                + "ON CONFLICT (reference_id) DO NOTHING"
                : "INSERT INTO processed_requests "
                + "(reference_id, status, transaction_type, account_id, amount) "
                + "VALUES (?, 'PROCESSING', ?, ?, ?) "
                + "ON CONFLICT (reference_id) DO NOTHING";

        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setString(1, referenceID);
            statement.setString(2, type);
            statement.setInt(3, accountID);
            if (destinationAccountID > 0) {
                statement.setInt(4, destinationAccountID);
                statement.setBigDecimal(5, amount);
            } else {
                statement.setBigDecimal(4, amount);
            }

            if (statement.executeUpdate() > 0) {
                return null;
            }
        }

        String checkSQL = "SELECT status, transaction_type, account_id, "
                + "destination_account_id, amount, result_message "
                + "FROM processed_requests WHERE reference_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(checkSQL)) {
            statement.setString(1, referenceID);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Could not find existing request");
                }

                String oldType = result.getString("transaction_type");
                int oldAccountID = result.getInt("account_id");
                int oldDestinationID = result.getInt("destination_account_id");
                BigDecimal oldAmount = result.getBigDecimal("amount");
                String oldStatus = result.getString("status");
                String oldMessage = result.getString("result_message");

                boolean sameRequest = type.equalsIgnoreCase(oldType)
                        && oldAccountID == accountID
                        && oldAmount != null
                        && oldAmount.compareTo(amount) == 0
                        && (destinationAccountID <= 0
                        || oldDestinationID == destinationAccountID);

                if (!sameRequest) {
                    throw new SQLException(
                            "Reference ID already belongs to a different request.");
                }

                System.out.println("Previous outcome: " + oldMessage);
                return "SUCCEEDED".equalsIgnoreCase(oldStatus);
            }
        }
    }

    private void insertTransaction(Connection connection, int accountID,
                                   String referenceID, String type,
                                   BigDecimal amount) throws SQLException {

        String sql = "INSERT INTO transactions "
                + "(account_id, reference_id, transaction_type, amount) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountID);
            statement.setString(2, referenceID);
            statement.setString(3, type);
            statement.setBigDecimal(4, amount);
            statement.executeUpdate();
        }
    }

    private void markRequest(Connection connection, String referenceID,
                             String status, String message) throws SQLException {

        String sql = "UPDATE processed_requests SET status = ?, "
                + "result_message = ?, completed_at = CURRENT_TIMESTAMP "
                + "WHERE reference_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, message);
            statement.setString(3, referenceID);
            statement.executeUpdate();
        }
    }
}
