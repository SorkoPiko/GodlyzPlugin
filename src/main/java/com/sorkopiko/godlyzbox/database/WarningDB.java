package com.sorkopiko.godlyzbox.database;

import com.sorkopiko.godlyzbox.types.Warning;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarningDB {

    private final Connection connection;

    public WarningDB(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                            CREATE TABLE IF NOT EXISTS warnings (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT,
                            warning_text TEXT,
                            type TEXT,
                            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                            )
                    """);
        }
    }

    public void addWarning(UUID playerUuid, String warningText, String type) throws SQLException {
        String sql = "INSERT INTO warnings(player_uuid, warning_text, type) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, warningText);
            pstmt.setString(3, type);
            pstmt.executeUpdate();
        }
    }

    public List<Warning> warnList() throws SQLException {
        List<Warning> warnings = new ArrayList<>();

        String sql = "SELECT * FROM warnings";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String playerUuid = rs.getString("player_uuid");
                String warningText = rs.getString("warning_text");
                String type = rs.getString("type");
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                Warning warning = new Warning(id, playerUuid, warningText, type, timestamp);
                warnings.add(warning);
            }
        }

        return warnings;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}