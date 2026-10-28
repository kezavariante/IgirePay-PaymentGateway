
package com.igirepay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PinService {

    public boolean createPin(int customerID, String pin)
            throws SQLException {

        if (!isValidPin(pin)) {
            throw new IllegalArgumentException(
                    "PIN must contain 4 to 6 digits.");
        }

        String sql = "UPDATE customers SET pin = ? "
                + "WHERE id = ? AND pin IS NULL";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, pin);
            statement.setInt(2, customerID);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean validatePin(int customerID, String pin)
            throws SQLException {

        String sql = "SELECT pin FROM customers WHERE id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    String savedPin = resultSet.getString("pin");

                    return savedPin != null
                            && savedPin.equals(pin);
                }
            }
        }

        return false;
    }

    public boolean changePin(int customerID,
                             String currentPin,
                             String newPin)
            throws SQLException {

        if (!isValidPin(newPin)) {
            throw new IllegalArgumentException(
                    "New PIN must contain 4 to 6 digits.");
        }

        if (!validatePin(customerID, currentPin)) {
            return false;
        }

        String sql = "UPDATE customers SET pin = ? WHERE id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, newPin);
            statement.setInt(2, customerID);

            return statement.executeUpdate() > 0;
        }
    }

    private boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4,6}");
    }
}
