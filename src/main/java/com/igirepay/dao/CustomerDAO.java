
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


    public int addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (full_name, email, phone_number) " +
                "VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhoneNumber());

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
        String sql = "SELECT id, full_name, email, phone_number " +
                "FROM customers WHERE id = ?";

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

        String sql = "UPDATE customers SET full_name = ?, email = ?, " +
                "phone_number = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhoneNumber());
            statement.setInt(4, customerID);

            return statement.executeUpdate() > 0;
        }
    }


    public boolean deleteCustomer(int customerID) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            return statement.executeUpdate() > 0;
        }
    }


    public Customer getCustomerByEmail(String email) throws SQLException {
        String sql = "SELECT id, full_name, email, phone_number " +
                "FROM customers WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        }

        return null;
    }


    public List<Customer> searchCustomersByName(String name)
            throws SQLException {

        List<Customer> customers = new ArrayList<>();

        String sql = "SELECT id, full_name, email, phone_number " +
                "FROM customers WHERE full_name ILIKE ? " +
                "ORDER BY full_name";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + name + "%");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(mapCustomer(resultSet));
                }
            }
        }

        return customers;
    }


    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();

        String sql = "SELECT id, full_name, email, phone_number " +
                "FROM customers ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                customers.add(mapCustomer(resultSet));
            }
        }

        return customers;
    }
    public boolean verifyPin(int customerID, String enteredPin)
            throws SQLException {

        String sql = "SELECT pin FROM customers WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String storedPin = resultSet.getString("pin");
                    return storedPin != null && storedPin.equals(enteredPin);
                }
            }
        }

        return false;
    }


    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone_number")
        );
    }



}
