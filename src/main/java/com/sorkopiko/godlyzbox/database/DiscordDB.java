package com.sorkopiko.godlyzbox.database;

import java.sql.*;
import java.util.UUID;

public class DiscordDB {

    private final Connection connection;

    public DiscordDB(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                            CREATE TABLE IF NOT EXISTS users (
                            discord_id NUMBER PRIMARY KEY,
                            mc_uuid TEXT NOT NULL,
                            )
                    """);
        }
    }

    public void addUser(UUID minecraftUUID, long discordID) throws SQLException {
        String sql = "INSERT INTO users(mc_uuid, discord_id) VALUES(?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, minecraftUUID.toString());
            pstmt.setLong(2, discordID);
            pstmt.executeUpdate();
        }
    }

    public UUID getLinkedMC(long discordID) throws SQLException {
        String sql = "SELECT mc_uuid FROM users WHERE discord_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, discordID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("mc_uuid"));
            }
        }
        return null;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}