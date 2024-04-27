package com.sorkopiko.godlyzbox.database;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.types.Warning;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
                            reason TEXT,
                            type TEXT,
                            warner TEXT,
                            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                            )
                    """);
        }
    }

    public void addWarning(UUID playerUuid, String reason, String warner, String type) throws SQLException, IllegalArgumentException {
        if (!Objects.equals(type, "+") && !Objects.equals(type, "-")) {
            throw new IllegalArgumentException("Type must be either '+' or '-'");
        }

        String sql = "INSERT INTO warnings(player_uuid, reason, type, warner) VALUES(?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, reason);
            pstmt.setString(3, type);
            pstmt.setString(4, warner);
            pstmt.executeUpdate();
        }
    }

    public List<Warning> warnList(UUID playerUUID) throws SQLException {
        List<Warning> warnings = new ArrayList<>();

        String sql = "SELECT * FROM warnings WHERE player_uuid = '" + playerUUID.toString() + "' ORDER BY timestamp ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String warningText = rs.getString("reason");
                String type = rs.getString("type");
                String warner = rs.getString("warner");
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                Warning warning = new Warning(id, playerUUID, warningText, type, warner, timestamp);
                warnings.add(warning);
            }
        }

        return warnings;
    }

    public Integer getWarningCount(UUID playerUUID) throws SQLException {
        List<Warning> warnings = warnList(playerUUID);
        int total = 0;

        for (Warning warning : warnings) {
            total += warning.type.equals("+") ? 1 : -1;
        }
        return total;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Nullable
    public static List<String> checkPunishments(GodlyzPlugin plugin, WarningDB warningDB, UUID playerUUID) throws SQLException {
        List<String> punishment = plugin.getConfig().getStringList("punishments.default.commands");
        String path = "punishments." + warningDB.getWarningCount(playerUUID) + ".commands";

        if (plugin.getConfig().contains(path)) {
            punishment.addAll(plugin.getConfig().getStringList(path));
        }

        return punishment;
    }
}