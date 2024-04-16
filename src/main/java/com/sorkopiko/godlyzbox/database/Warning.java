package com.sorkopiko.godlyzbox.database;

import java.time.LocalDateTime;

public class Warning {
    private int id;
    private String playerUuid;
    private String warningText;
    private String type;
    private LocalDateTime timestamp;

    public Warning(int id, String playerUuid, String warningText, String type, LocalDateTime timestamp) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.warningText = warningText;
        this.type = type;
        this.timestamp = timestamp;
    }

    // getters and setters
}