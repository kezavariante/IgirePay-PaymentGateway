package com.igirepay.dao;

import com.igirepay.DatabaseConnection;
import com.igirepay.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public List<Transaction> getTransactionsByAccountId(int accountID)
            throws SQLException {

        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT reference_id, amount, transaction_type, created_at "
                + "FROM transactions WHERE account_id = ? ORDER BY created_at DESC";

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

    public void exportTransactionsToCSV(Path filePath)
            throws SQLException, IOException {

        String sql = "SELECT id, account_id, reference_id, "
                + "transaction_type, amount, created_at "
                + "FROM transactions ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery();
             BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            writer.write("Transaction ID,Account ID,Reference ID,Type,Amount,Timestamp");
            writer.newLine();

            while (resultSet.next()) {
                writer.write(
                        resultSet.getInt("id") + ","
                                + resultSet.getInt("account_id") + ","
                                + csvValue(resultSet.getString("reference_id")) + ","
                                + csvValue(resultSet.getString("transaction_type")) + ","
                                + resultSet.getBigDecimal("amount") + ","
                                + csvValue(resultSet.getTimestamp("created_at").toString())
                );
                writer.newLine();
            }
        }
    }

    public void exportCustomerStatement(int customerID, Path filePath)
            throws SQLException, IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(getCustomerTransactionStatement(customerID));
        }
    }

    public String getAllTransactionsReport() throws SQLException {
        String sql = "SELECT t.id, t.account_id, a.customer_id, c.full_name, "
                + "t.transaction_type, t.amount, t.created_at "
                + "FROM transactions t "
                + "JOIN accounts a ON t.account_id = a.id "
                + "JOIN customers c ON a.customer_id = c.id "
                + "ORDER BY t.created_at DESC";

        StringBuilder report = new StringBuilder("ALL CUSTOMER TRANSACTIONS\n");
        report.append("--------------------------------------------------\n");
        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                found = true;
                report.append("Transaction ID: ").append(resultSet.getInt("id")).append("\n");
                report.append("Customer ID: ").append(resultSet.getInt("customer_id"))
                        .append(" (").append(resultSet.getString("full_name")).append(")\n");
                report.append("Account ID: ").append(resultSet.getInt("account_id")).append("\n");
                report.append("Type: ").append(resultSet.getString("transaction_type")).append("\n");
                report.append("Amount: ").append(resultSet.getBigDecimal("amount")).append(" RWF\n");
                report.append("Timestamp: ").append(resultSet.getTimestamp("created_at")).append("\n");
                report.append("--------------------------------------------------\n");
            }
        }

        return found ? report.toString() : "No transactions found.";
    }

    public String getDailyTransactionSummary(LocalDate date) throws SQLException {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        String sql = "SELECT COUNT(*) AS total_transactions, "
                + "COALESCE(SUM(amount), 0) AS total_amount, "
                + "COALESCE(SUM(CASE WHEN transaction_type = 'DEPOSIT' "
                + "THEN amount ELSE 0 END), 0) AS total_deposits, "
                + "COALESCE(SUM(CASE WHEN transaction_type = 'WITHDRAW' "
                + "THEN amount ELSE 0 END), 0) AS total_withdrawals "
                + "FROM transactions "
                + "WHERE created_at >= ? AND created_at < ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(date.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "DAILY TRANSACTION SUMMARY\n"
                            + "Date: " + date + "\n"
                            + "Total transactions: "
                            + resultSet.getLong("total_transactions") + "\n"
                            + "Total amount of all transactions: "
                            + resultSet.getBigDecimal("total_amount") + "\n"
                            + "Total deposits: "
                            + resultSet.getBigDecimal("total_deposits") + "\n"
                            + "Total withdrawals: "
                            + resultSet.getBigDecimal("total_withdrawals");
                }
            }
        }

        return "No summary available for " + date;
    }

    public String getCustomerTransactionStatement(int customerID)
            throws SQLException {
        return getCustomerTransactionStatement(customerID, true);
    }

    public String getCustomerTransactionStatement(int customerID, boolean includeCustomerId)
            throws SQLException {

        String sql = "SELECT t.id, t.account_id, t.reference_id, "
                + "t.transaction_type, t.amount, t.created_at "
                + "FROM transactions t "
                + "JOIN accounts a ON t.account_id = a.id "
                + "WHERE a.customer_id = ? "
                + "ORDER BY t.created_at ASC";

        StringBuilder statementText = new StringBuilder();
        statementText.append("CUSTOMER TRANSACTION STATEMENT\n");
        if (includeCustomerId) {
            statementText.append("Customer ID: ").append(customerID).append("\n");
        }
        statementText.append("--------------------------------------------------\n");

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
                            .append(resultSet.getString("reference_id")).append("\n");
                    statementText.append("Type: ")
                            .append(resultSet.getString("transaction_type")).append("\n");
                    statementText.append("Amount: ")
                            .append(resultSet.getBigDecimal("amount")).append("\n");
                    statementText.append("Timestamp: ")
                            .append(resultSet.getTimestamp("created_at")).append("\n");
                    statementText.append("--------------------------------------------------\n");
                }
            }
        }

        if (!found) {
            return "No transactions found for customer ID " + customerID;
        }

        return statementText.toString();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        return new Transaction(
                resultSet.getString("reference_id"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("transaction_type"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
