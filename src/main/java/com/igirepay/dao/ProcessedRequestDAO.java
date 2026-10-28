
package com.igirepay.dao;

import com.igirepay.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProcessedRequestDAO {


    public boolean addProcessedRequest(String referenceID)
            throws SQLException {

        String sql = "INSERT INTO processed_requests (reference_id) " +
                "VALUES (?) ON CONFLICT (reference_id) DO NOTHING";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);


            return statement.executeUpdate() > 0;
        }
    }


    public boolean isProcessed(String referenceID) throws SQLException {
        String sql = "SELECT 1 FROM processed_requests WHERE reference_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }


    public boolean deleteProcessedRequest(String referenceID)
            throws SQLException {

        String sql = "DELETE FROM processed_requests WHERE reference_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            return statement.executeUpdate() > 0;
        }
    }


    public List<String> getAllProcessedReferences() throws SQLException {
        List<String> references = new ArrayList<>();

        String sql = "SELECT reference_id FROM processed_requests ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                references.add(resultSet.getString("reference_id"));
            }
        }

        return references;
    }
}
