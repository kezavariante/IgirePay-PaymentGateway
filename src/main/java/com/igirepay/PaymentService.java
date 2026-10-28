
package com.igirepay;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

public class PaymentService {


    public boolean processPayment(int accountID, Transaction transaction)
            throws SQLException {

        if (transaction == null || accountID <= 0) {
            throw new IllegalArgumentException("Invalid payment request");
        }

        String referenceID = transaction.getReferenceID();
        String type = transaction.getTransactionType().toUpperCase(Locale.ROOT);
        BigDecimal amount = transaction.getAmount();

        if (referenceID == null || referenceID.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (!type.equals("DEPOSIT") && !type.equals("WITHDRAW")) {
            throw new IllegalArgumentException("Unsupported transaction type");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {

                String requestSQL =
                        "INSERT INTO processed_requests " +
                                "(reference_id, status, transaction_type, account_id, amount) " +
                                "VALUES (?, 'PROCESSING', ?, ?, ?) " +
                                "ON CONFLICT (reference_id) DO NOTHING";

                int inserted;

                try (PreparedStatement statement =
                             connection.prepareStatement(requestSQL)) {
                    statement.setString(1, referenceID);
                    statement.setString(2, type);
                    statement.setInt(3, accountID);
                    statement.setBigDecimal(4, amount);
                    inserted = statement.executeUpdate();
                }


                if (inserted == 0) {
                    String checkSQL =
                            "SELECT status, transaction_type, account_id, amount, " +
                                    "result_message FROM processed_requests " +
                                    "WHERE reference_id = ?";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(checkSQL)) {
                        statement.setString(1, referenceID);

                        try (java.sql.ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("Could not find existing request");
                            }

                            String oldType = result.getString("transaction_type");
                            int oldAccountID = result.getInt("account_id");
                            boolean accountWasNull = result.wasNull();
                            BigDecimal oldAmount = result.getBigDecimal("amount");
                            String oldStatus = result.getString("status");
                            String oldMessage = result.getString("result_message");

                            if (oldType == null || oldAmount == null || accountWasNull
                                    || !oldType.equalsIgnoreCase(type)
                                    || oldAccountID != accountID
                                    || oldAmount.compareTo(amount) != 0) {
                                throw new SQLException(
                                        "Reference ID already belongs to a different " +
                                                "or older request. Use a new reference ID.");
                            }

                            connection.commit();

                            System.out.println("Previous outcome: " + oldMessage);
                            return "SUCCEEDED".equalsIgnoreCase(oldStatus);
                        }
                    }
                }


                String balanceSQL;

                if (type.equals("DEPOSIT")) {
                    balanceSQL =
                            "UPDATE accounts SET balance = balance + ? WHERE id = ?";
                } else {
                    balanceSQL =
                            "UPDATE accounts SET balance = balance - ? " +
                                    "WHERE id = ? AND balance >= ?";
                }

                int updated;

                try (PreparedStatement statement =
                             connection.prepareStatement(balanceSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, accountID);

                    if (type.equals("WITHDRAW")) {
                        statement.setBigDecimal(3, amount);
                    }

                    updated = statement.executeUpdate();
                }


                if (updated == 0) {
                    String message =
                            "Account not found or insufficient balance";

                    String failedSQL =
                            "UPDATE processed_requests SET status = 'FAILED', " +
                                    "result_message = ?, completed_at = CURRENT_TIMESTAMP " +
                                    "WHERE reference_id = ?";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(failedSQL)) {
                        statement.setString(1, message);
                        statement.setString(2, referenceID);
                        statement.executeUpdate();
                    }

                    connection.commit();
                    System.out.println(message);
                    return false;
                }

                String transactionSQL =
                        "INSERT INTO transactions " +
                                "(account_id, reference_id, transaction_type, amount) " +
                                "VALUES (?, ?, ?, ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(transactionSQL)) {
                    statement.setInt(1, accountID);
                    statement.setString(2, referenceID);
                    statement.setString(3, type);
                    statement.setBigDecimal(4, amount);
                    statement.executeUpdate();
                }


                String successSQL =
                        "UPDATE processed_requests SET status = 'SUCCEEDED', " +
                                "result_message = ?, completed_at = CURRENT_TIMESTAMP " +
                                "WHERE reference_id = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(successSQL)) {
                    statement.setString(1, type + " completed successfully");
                    statement.setString(2, referenceID);
                    statement.executeUpdate();
                }


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
            throw new IllegalArgumentException(
                    "Cannot transfer to the same account");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero");
        }

        if (referenceID == null || referenceID.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {

                String requestSQL =
                        "INSERT INTO processed_requests " +
                                "(reference_id, status, transaction_type, account_id, " +
                                "destination_account_id, amount) " +
                                "VALUES (?, 'PROCESSING', 'TRANSFER', ?, ?, ?) " +
                                "ON CONFLICT (reference_id) DO NOTHING";

                int inserted;

                try (PreparedStatement statement =
                             connection.prepareStatement(requestSQL)) {
                    statement.setString(1, referenceID);
                    statement.setInt(2, sourceAccountID);
                    statement.setInt(3, destinationAccountID);
                    statement.setBigDecimal(4, amount);

                    inserted = statement.executeUpdate();
                }


                if (inserted == 0) {
                    String checkSQL =
                            "SELECT status, transaction_type, account_id, " +
                                    "destination_account_id, amount, result_message " +
                                    "FROM processed_requests WHERE reference_id = ?";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(checkSQL)) {
                        statement.setString(1, referenceID);

                        try (java.sql.ResultSet result =
                                     statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException(
                                        "Could not find existing transfer request");
                            }

                            String oldStatus = result.getString("status");
                            String oldType = result.getString("transaction_type");
                            int oldSourceID = result.getInt("account_id");
                            boolean sourceWasNull = result.wasNull();

                            int oldDestinationID =
                                    result.getInt("destination_account_id");
                            boolean destinationWasNull = result.wasNull();

                            BigDecimal oldAmount = result.getBigDecimal("amount");
                            String oldMessage = result.getString("result_message");


                            if (!"TRANSFER".equalsIgnoreCase(oldType)
                                    || sourceWasNull
                                    || destinationWasNull
                                    || oldSourceID != sourceAccountID
                                    || oldDestinationID != destinationAccountID
                                    || oldAmount == null
                                    || oldAmount.compareTo(amount) != 0) {
                                throw new SQLException(
                                        "Reference ID already belongs to a " +
                                                "different or older request. Use a new reference ID.");
                            }


                            connection.commit();

                            System.out.println("Previous outcome: " + oldMessage);

                            return "SUCCEEDED".equalsIgnoreCase(oldStatus);
                        }
                    }
                }


                String lockSQL =
                        "SELECT id, balance FROM accounts " +
                                "WHERE id IN (?, ?) ORDER BY id FOR UPDATE";

                BigDecimal sourceBalance = null;
                boolean sourceFound = false;
                boolean destinationFound = false;

                try (PreparedStatement statement =
                             connection.prepareStatement(lockSQL)) {
                    statement.setInt(1, sourceAccountID);
                    statement.setInt(2, destinationAccountID);

                    try (java.sql.ResultSet result =
                                 statement.executeQuery()) {
                        while (result.next()) {
                            int id = result.getInt("id");

                            if (id == sourceAccountID) {
                                sourceBalance = result.getBigDecimal("balance");
                                sourceFound = true;
                            }

                            if (id == destinationAccountID) {
                                destinationFound = true;
                            }
                        }
                    }
                }


                if (!sourceFound || !destinationFound) {
                    String message = "Source or destination account does not exist";

                    updateTransferOutcome(
                            connection, referenceID, "FAILED", message);

                    connection.commit();
                    System.out.println(message);
                    return false;
                }


                if (sourceBalance.compareTo(amount) < 0) {
                    String message = "Insufficient balance";

                    updateTransferOutcome(
                            connection, referenceID, "FAILED", message);

                    connection.commit();
                    System.out.println(message);
                    return false;
                }


                String debitSQL =
                        "UPDATE accounts SET balance = balance - ? " +
                                "WHERE id = ? AND balance >= ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(debitSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, sourceAccountID);
                    statement.setBigDecimal(3, amount);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Transfer debit failed");
                    }
                }


                String creditSQL =
                        "UPDATE accounts SET balance = balance + ? WHERE id = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(creditSQL)) {
                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, destinationAccountID);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Transfer credit failed");
                    }
                }


                String transactionSQL =
                        "INSERT INTO transactions " +
                                "(account_id, reference_id, transaction_type, amount) " +
                                "VALUES (?, ?, 'TRANSFER', ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(transactionSQL)) {
                    statement.setInt(1, sourceAccountID);
                    statement.setString(2, referenceID);
                    statement.setBigDecimal(3, amount);
                    statement.executeUpdate();

                    statement.setInt(1, destinationAccountID);
                    statement.setString(2, referenceID);
                    statement.setBigDecimal(3, amount);
                    statement.executeUpdate();
                }


                updateTransferOutcome(
                        connection,
                        referenceID,
                        "SUCCEEDED",
                        "Transfer completed successfully");


                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void updateTransferOutcome(
            Connection connection,
            String referenceID,
            String status,
            String message) throws SQLException {

        String sql =
                "UPDATE processed_requests SET status = ?, " +
                        "result_message = ?, completed_at = CURRENT_TIMESTAMP " +
                        "WHERE reference_id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, message);
            statement.setString(3, referenceID);
            statement.executeUpdate();
        }
    }


}
