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

public class AccountDAO {

    public int addAccount(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (customer_id, account_type, balance) "
                + "VALUES (?, ?, ?)";

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

        throw new SQLException("Creating account failed; no ID returned.");
    }

    public Account getAccountById(int accountID) throws SQLException {
        String sql = "SELECT id, customer_id, account_type, balance "
                + "FROM accounts WHERE id = ?";

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

    public boolean deleteAccount(int accountID) throws SQLException {
        String sql = "DELETE FROM accounts WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);
            return statement.executeUpdate() > 0;
        }
    }

    public List<Account> getAccountsByCustomerId(int customerID)
            throws SQLException {

        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, customer_id, account_type, balance "
                + "FROM accounts WHERE customer_id = ? ORDER BY id";

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

    public String getAccountSummary() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_accounts, "
                + "COALESCE(SUM(balance), 0) AS total_balance, "
                + "COUNT(*) FILTER (WHERE account_type = 'WALLET') AS wallets, "
                + "COUNT(*) FILTER (WHERE account_type = 'SAVINGS') AS savings "
                + "FROM accounts";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return "ACCOUNT SUMMARY\n"
                        + "Total accounts: " + resultSet.getLong("total_accounts") + "\n"
                        + "Wallet accounts: " + resultSet.getLong("wallets") + "\n"
                        + "Savings accounts: " + resultSet.getLong("savings") + "\n"
                        + "Total balance: " + resultSet.getBigDecimal("total_balance") + " RWF";
            }
        }

        return "No accounts found.";
    }

    public List<Account> getAllAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, customer_id, account_type, balance "
                + "FROM accounts ORDER BY id";

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
        return new Account(
                accountID,
                "ACC-" + accountID,
                resultSet.getBigDecimal("balance"),
                resultSet.getInt("customer_id"),
                resultSet.getString("account_type")
        );
    }
}
