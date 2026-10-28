
package com.igirepay.dao;

import com.igirepay.DatabaseConnection;
import com.igirepay.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
            statement.setTimestamp(5, Timestamp.valueOf(transaction.getTimestamp()));

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("Creating transaction failed; no ID was returned.");
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

        String sql = "UPDATE transactions SET transaction_type = ? WHERE id = ?";

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

        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT reference_id, amount, transaction_type, created_at " +
                "FROM transactions WHERE transaction_type = ? ORDER BY created_at DESC";

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


    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("created_at");

        return new Transaction(
                resultSet.getString("reference_id"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("transaction_type"),
                timestamp.toLocalDateTime()
        );
    }
}
