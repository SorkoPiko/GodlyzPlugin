package com.sorkopiko.godlyzbox.commands;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.WarningDB;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Warn implements CommandExecutor {
    private final WarningDB warningDB;
    private final GodlyzPlugin plugin;

    public Warn(GodlyzPlugin plugin) {
        this.plugin = plugin;
        this.warningDB = plugin.getWarningDB();
        plugin.getCommand("warn").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }
        UUID uuid;
        if (plugin.getServer().getPlayer(args[0]) == null) {
            if (this.plugin.getMojang().getUUIDOfUsername(args[0]) == null) {
                sender.sendMessage("Player not found!");
                return true;
            }
            String uuidStr = this.plugin.getMojang().getUUIDOfUsername(args[0]);
            uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" + uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" + uuidStr.substring(20);
            uuid = UUID.fromString(uuidStr);
        } else {
            uuid = plugin.getServer().getPlayer(args[0]).getUniqueId();
        }

        String warner;
        if (!(sender instanceof Player)) {
            warner = "CONSOLE";
        }
        else {
            warner = ((Player) sender).getUniqueId().toString();
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        warn(uuid, reason, warner, "+");
        return true;
    }

    public void warn(UUID uuid, String reason, String warner, String type) throws IllegalArgumentException {
        Player onlinePlayer = plugin.getServer().getPlayer(uuid);
        if (!Objects.equals(type, "+") && !Objects.equals(type, "-")) {
            throw new IllegalArgumentException("Type must be either '+' or '-'");
        }
        try {
            warningDB.addWarning(uuid, reason, warner, type);
            if (Objects.equals(type, "-")) {
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(this.plugin.getConfig().getString("messages.unwarn")
                            .replace("{reason}", reason)
                            .replace("{count}", String.valueOf(warningDB.getWarningCount(uuid)))
                            .replace("{warner}", warner));
                }
                return;
            }
            List<String> execute = WarningDB.checkPunishments(plugin, warningDB, uuid);
            if (execute != null) {
                for (String executeCommand : execute) {
                    try {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), executeCommand);
                    } catch (Exception e) {
                        e.printStackTrace();
                        plugin.getLogger().severe("An error occurred while executing the punishment command: '" + executeCommand + "'!");
                    }
                }
            }
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(this.plugin.getConfig().getString("messages.warn")
                        .replace("{reason}", reason)
                        .replace("{count}", String.valueOf(warningDB.getWarningCount(uuid)))
                        .replace("{warner}", warner));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("An error occurred while warning the player!");
        }
    }
}
