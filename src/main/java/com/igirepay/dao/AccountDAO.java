package com.igirepay.dao;

import com.igirepay.Account;
import com.igirepay.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountDAO {



    public int addAccount(Account account) throws SQLException {
       String sql = "SELECT id, customer_id, account_type, balance " +
                "FROM accounts " +
                "WHERE customer_id = ? ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, account.getCustomerID());
            statement.setString(2, account.getAccountType());
            statement.setBigDecimal(3, account.getBalance());

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException(
                "Creating account failed; no ID was returned.");
    }



    public Account getAccountById(int accountID) throws SQLException {
        String sql = "SELECT id, customer_id, account_type, balance\n" +
                "FROM accounts\n" +
                "WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
            }
        }

        return null;
    }


    public boolean updateBalance(int accountID, BigDecimal newBalance)
            throws SQLException {

        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        String sql = "UPDATE accounts " +
                "SET balance = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, newBalance);
            statement.setInt(2, accountID);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(int accountID) throws SQLException {
        String sql = "DELETE FROM accounts" +
                " WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            return statement.executeUpdate() > 0;
        }
    }



    public List<Account> getAccountsByCustomerId(int customerID)
            throws SQLException {

        List<Account> accounts = new ArrayList<>();

        String sql = "SELECT id, customer_id, account_type, balance " +
                "FROM accounts " +
                "WHERE customer_id = ? ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
            }
        }

        return accounts;
    }



    public List<Account> getAccountsWithBalanceAbove(BigDecimal minimumBalance)
            throws SQLException {

        List<Account> accounts = new ArrayList<>();

        String sql = "SELECT id, customer_id, balance " +
                "FROM accounts " +
                "WHERE balance > ? ORDER BY balance DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, minimumBalance);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
            }
        }

        return accounts;
    }


    public List<Account> getAllAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();

        String sql = "SELECT id, customer_id, balance " +
                "FROM accounts " +
                "ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                accounts.add(mapAccount(resultSet));
            }
        }

        return accounts;
    }



    private Account mapAccount(ResultSet resultSet) throws SQLException {

        int accountID = resultSet.getInt("id");
        int customerID = resultSet.getInt("customer_id");
        BigDecimal balance = resultSet.getBigDecimal("balance");
        String accountType = resultSet.getString("account_type");

        String accountNumber = "ACC-" + accountID;

        return new Account(
                accountID,
                accountNumber,
                balance,
                customerID,
                accountType
        );
    }


}
