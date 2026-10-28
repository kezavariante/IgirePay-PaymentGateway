
package com.igirepay.dao;

import com.igirepay.DatabaseConnection;
import com.igirepay.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {


    public int addTransaction(int accountID, Transaction transaction)
            throws SQLException {

        String sql = "INSERT INTO transactions " +
                "(account_id, reference_id, transaction_type, amount, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, accountID);
            statement.setString(2, transaction.getReferenceID());
            statement.setString(3, transaction.getTransactionType());
            statement.setBigDecimal(4, transaction.getAmount());
            statement.setTimestamp(
                    5, Timestamp.valueOf(transaction.getTimestamp()));

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException(
                "Creating transaction failed; no ID was returned.");
    }


    public Transaction getTransactionById(int transactionID)
            throws SQLException {

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transactionID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTransaction(resultSet);
                }
            }
        }

        return null;
    }


    public boolean updateTransactionType(int transactionID, String newType)
            throws SQLException {

        if (newType == null ||
                !List.of("DEPOSIT", "WITHDRAW", "TRANSFER")
                        .contains(newType.toUpperCase())) {
            throw new IllegalArgumentException("Invalid transaction type");
        }

        String sql =
                "UPDATE transactions SET transaction_type = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newType.toUpperCase());
            statement.setInt(2, transactionID);

            return statement.executeUpdate() > 0;
        }
    }


    public boolean deleteTransaction(int transactionID) throws SQLException {

        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transactionID);

            return statement.executeUpdate() > 0;
        }
    }


    public List<Transaction> getTransactionsByReferenceId(String referenceID)
            throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions WHERE reference_id = ? ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }
        }

        return transactions;
    }


    public List<Transaction> getTransactionsByAccountId(int accountID)
            throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions WHERE account_id = ? ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }
        }

        return transactions;
    }


    public List<Transaction> getTransactionsByType(String transactionType)
            throws SQLException {

        if (transactionType == null || transactionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction type is required.");
        }

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions WHERE transaction_type = ? " +
                "ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transactionType.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
            }
        }

        return transactions;
    }


    public List<Transaction> getAllTransactions() throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                transactions.add(mapTransaction(resultSet));
            }
        }

        return transactions;
    }


    public void exportTransactionsToCSV(Path filePath)
            throws SQLException, IOException {

        String sql = "SELECT id, account_id, reference_id, " +
                "transaction_type, amount, created_at " +
                "FROM transactions ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery();
             BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            writer.write(
                    "Transaction ID,Account ID,Reference ID,Type,Amount,Timestamp");
            writer.newLine();

            while (resultSet.next()) {
                writer.write(
                        resultSet.getInt("id") + "," +
                                resultSet.getInt("account_id") + "," +
                                csvValue(resultSet.getString("reference_id")) + "," +
                                csvValue(resultSet.getString("transaction_type")) + "," +
                                resultSet.getBigDecimal("amount") + "," +
                                csvValue(resultSet.getTimestamp("created_at").toString())
                );

                writer.newLine();
            }
        }
    }


    private String csvValue(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }


    public String getDailyTransactionSummary(LocalDate date)
            throws SQLException {

        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        String sql = "SELECT " +
                "COUNT(*) AS total_transactions, " +
                "COALESCE(SUM(amount), 0) AS total_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'DEPOSIT' " +
                "THEN amount ELSE 0 END), 0) AS total_deposits, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'WITHDRAW' " +
                "THEN amount ELSE 0 END), 0) AS total_withdrawals " +
                "FROM transactions " +
                "WHERE created_at >= ? AND created_at < ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(
                    1, Timestamp.valueOf(date.atStartOfDay()));

            statement.setTimestamp(
                    2, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "DAILY TRANSACTION SUMMARY\n" +
                            "Date: " + date + "\n" +
                            "Total transactions: " +
                            resultSet.getLong("total_transactions") + "\n" +
                            "Total amount of all transactions: " +
                            resultSet.getBigDecimal("total_amount") + "\n" +
                            "Total deposits: " +
                            resultSet.getBigDecimal("total_deposits") + "\n" +
                            "Total withdrawals: " +
                            resultSet.getBigDecimal("total_withdrawals");
                }
            }
        }

        return "No summary available for " + date;
    }


    public String getCustomerTransactionStatement(int customerID)
            throws SQLException {

        String sql = "SELECT t.id, t.account_id, t.reference_id, " +
                "t.transaction_type, t.amount, t.created_at " +
                "FROM transactions t " +
                "JOIN accounts a ON t.account_id = a.id " +
                "WHERE a.customer_id = ? " +
                "ORDER BY t.created_at ASC";

        StringBuilder statementText = new StringBuilder();

        statementText.append("CUSTOMER TRANSACTION STATEMENT\n");
        statementText.append("Customer ID: ").append(customerID).append("\n");
        statementText.append(
                "--------------------------------------------------\n");

        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    found = true;

                    statementText.append("Transaction ID: ")
                            .append(resultSet.getInt("id")).append("\n");

                    statementText.append("Account ID: ")
                            .append(resultSet.getInt("account_id")).append("\n");

                    statementText.append("Reference ID: ")
                            .append(resultSet.getString("reference_id"))
                            .append("\n");

                    statementText.append("Type: ")
                            .append(resultSet.getString("transaction_type"))
                            .append("\n");

                    statementText.append("Amount: ")
                            .append(resultSet.getBigDecimal("amount"))
                            .append("\n");

                    statementText.append("Timestamp: ")
                            .append(resultSet.getTimestamp("created_at"))
                            .append("\n");

                    statementText.append(
                            "--------------------------------------------------\n");
                }
            }
        }

        if (!found) {
            return "No transactions found for customer ID " + customerID;
        }

        return statementText.toString();
    }


    private Transaction mapTransaction(ResultSet resultSet)
            throws SQLException {

        Timestamp timestamp = resultSet.getTimestamp("created_at");

        return new Transaction(
                resultSet.getString("reference_id"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("transaction_type"),
                timestamp.toLocalDateTime()
        );
    }
}
