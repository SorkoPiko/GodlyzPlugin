package com.sorkopiko.godlyzbox.types;

import java.time.LocalDateTime;
import java.util.UUID;

public class Warning {
    public int id;
    public UUID playerUuid;
    public String reason;
    public String type;
    public String warner;
    public LocalDateTime timestamp;

    public Warning(int id, UUID playerUuid, String reason, String type, String warner, LocalDateTime timestamp) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.reason = reason;
        this.type = type;
        this.warner = warner;
        this.timestamp = timestamp;
    }

    // getters and setters
}