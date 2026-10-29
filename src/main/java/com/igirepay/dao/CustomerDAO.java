package com.igirepay.dao;

import com.igirepay.Customer;
import com.igirepay.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static final String CUSTOMER_SELECT =
            "SELECT c.id, c.full_name, c.email, c.phone_number, "
                    + "(SELECT COUNT(*) FROM accounts a WHERE a.customer_id = c.id) "
                    + "AS account_count ";

    public int addCustomer(Customer customer, String pin) throws SQLException {
        String sql = "INSERT INTO customers (full_name, email, phone_number, pin) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhoneNumber());
            statement.setString(4, pin);
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("Creating customer failed; no ID was returned.");
    }

    public Customer getCustomerById(int customerID) throws SQLException {
        String sql = CUSTOMER_SELECT + "FROM customers c WHERE c.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        }

        return null;
    }

    public boolean updateCustomer(Customer customer, int customerID)
            throws SQLException {

        String sql = "UPDATE customers SET full_name = ?, email = ?, "
                + "phone_number = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhoneNumber());
            statement.setInt(4, customerID);

            return statement.executeUpdate() > 0;
        }
    }

    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String sql = CUSTOMER_SELECT + "FROM customers c ORDER BY c.id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                customers.add(mapCustomer(resultSet));
            }
        }

        return customers;
    }

    public boolean createPin(int customerID, String pin) throws SQLException {
        String sql = "UPDATE customers SET pin = ? WHERE id = ? AND pin IS NULL";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pin);
            statement.setInt(2, customerID);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean resetPin(int customerID, String pin) throws SQLException {
        ensurePinAudit();
        String sql = "UPDATE customers SET pin = ?, failed_pin_attempts = 0 WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pin);
            statement.setInt(2, customerID);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean validatePin(int customerID, String pin) throws SQLException {
        String sql = "SELECT pin FROM customers WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String savedPin = resultSet.getString("pin");
                    return savedPin != null && savedPin.equals(pin);
                }
            }
        }

        return false;
    }

    public boolean changePin(int customerID, String currentPin, String newPin)
            throws SQLException {

        if (!validatePin(customerID, currentPin)) {
            recordFailedCustomerAttempt(customerID);
            return false;
        }

        ensurePinAudit();
        String sql = "UPDATE customers SET pin = ?, failed_pin_attempts = 0 WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newPin);
            statement.setInt(2, customerID);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean isPinAvailable(String pin) throws SQLException {
        String sql = "SELECT id FROM customers WHERE pin = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pin);

            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    public int getCustomerIDByPin(String pin) throws SQLException {
        String sql = "SELECT id FROM customers WHERE pin = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pin);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        return -1;
    }

    public String getAllPinRecords() throws SQLException {
        ensurePinAudit();

        String sql = "SELECT id, full_name, "
                + "CASE WHEN pin IS NULL OR pin = '' THEN 'NOT SET' ELSE pin END AS pin_value, "
                + "COALESCE(failed_pin_attempts, 0) AS failed_pin_attempts "
                + "FROM customers ORDER BY id";

        StringBuilder report = new StringBuilder("PIN RECORDS\n");
        report.append("--------------------------------------------------\n");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            boolean found = false;
            while (resultSet.next()) {
                found = true;
                report.append("Customer ID: ").append(resultSet.getInt("id"))
                        .append(", Name: ").append(resultSet.getString("full_name"))
                        .append(", PIN: ").append(resultSet.getString("pin_value"))
                        .append(", Failed PIN tries: ")
                        .append(resultSet.getInt("failed_pin_attempts"))
                        .append("\n");
            }

            if (!found) {
                return "No customers found.";
            }
        }

        return report.toString();
    }

    public String getCustomersWithoutPin() throws SQLException {
        String sql = "SELECT id, full_name, email FROM customers "
                + "WHERE pin IS NULL OR pin = '' ORDER BY id";

        StringBuilder report = new StringBuilder("CUSTOMERS WITHOUT A PIN\n");
        report.append("--------------------------------------------------\n");
        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                found = true;
                report.append("Customer ID: ").append(resultSet.getInt("id"))
                        .append(", Name: ").append(resultSet.getString("full_name"))
                        .append(", Email: ").append(resultSet.getString("email"))
                        .append("\n");
            }
        }

        return found ? report.toString() : "Every customer has a PIN set.";
    }

    public String getRepeatedPinFailures() throws SQLException {
        ensurePinAudit();

        String sql = "SELECT id, full_name, COALESCE(failed_pin_attempts, 0) AS failed_pin_attempts "
                + "FROM customers WHERE COALESCE(failed_pin_attempts, 0) >= 3 ORDER BY failed_pin_attempts DESC";

        StringBuilder report = new StringBuilder(
                "CUSTOMERS WITH 3 OR MORE FAILED PIN TRIES\n");
        report.append("--------------------------------------------------\n");
        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                found = true;
                report.append("Customer ID: ").append(resultSet.getInt("id"))
                        .append(", Name: ").append(resultSet.getString("full_name"))
                        .append(", Failed PIN tries: ")
                        .append(resultSet.getInt("failed_pin_attempts"))
                        .append("\n");
            }
        }

        report.append("\n").append(getLoginAttemptSummary());
        return found ? report.toString() : "No customers have 3 or more failed PIN tries.\n\n"
                + getLoginAttemptSummary();
    }

    public void recordLoginAttempt(boolean success) throws SQLException {
        ensurePinAudit();

        String sql = "INSERT INTO pin_attempts (success) VALUES (?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, success);
            statement.executeUpdate();
        }
    }

    public void recordFailedCustomerAttempt(int customerID) throws SQLException {
        ensurePinAudit();

        String sql = "UPDATE customers SET failed_pin_attempts = "
                + "COALESCE(failed_pin_attempts, 0) + 1 WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);
            statement.executeUpdate();
        }
    }

    public void clearFailedCustomerAttempts(int customerID) throws SQLException {
        ensurePinAudit();

        String sql = "UPDATE customers SET failed_pin_attempts = 0 WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);
            statement.executeUpdate();
        }
    }

    private String getLoginAttemptSummary() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_attempts, "
                + "COUNT(*) FILTER (WHERE success = TRUE) AS successful, "
                + "COUNT(*) FILTER (WHERE success = FALSE) AS failed "
                + "FROM pin_attempts";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return "LOGIN ATTEMPT SUMMARY\n"
                        + "Total PIN logins tried: " + resultSet.getLong("total_attempts") + "\n"
                        + "Successful: " + resultSet.getLong("successful") + "\n"
                        + "Failed: " + resultSet.getLong("failed");
            }
        }

        return "No login attempts recorded yet.";
    }

    private void ensurePinAudit() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("ALTER TABLE customers "
                    + "ADD COLUMN IF NOT EXISTS failed_pin_attempts INTEGER NOT NULL DEFAULT 0");
            statement.execute("CREATE TABLE IF NOT EXISTS pin_attempts ("
                    + "id SERIAL PRIMARY KEY, "
                    + "success BOOLEAN NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone_number")
        );
        customer.setAccountCount(resultSet.getInt("account_count"));
        return customer;
    }
}
