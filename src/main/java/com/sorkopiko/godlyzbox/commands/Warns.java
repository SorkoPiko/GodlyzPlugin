package com.sorkopiko.godlyzbox.commands;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.WarningDB;
import com.sorkopiko.godlyzbox.types.Warning;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class Warns implements CommandExecutor {
    private final GodlyzPlugin plugin;
    private final WarningDB warningDB;

    public Warns(GodlyzPlugin plugin) {
        this.plugin = plugin;
        this.warningDB = plugin.getWarningDB();
        plugin.getCommand("warns").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        sender.sendMessage(warns(uuid));
        return true;
    }

    public String warns(UUID uuid) {
        List<Warning> warns = null;
        StringBuilder returnMessage = new StringBuilder(ChatColor.AQUA.toString()).append("Your warns:\n");
        try {
            warns = this.warningDB.warnList(uuid);
        } catch (SQLException e) {
            e.printStackTrace();
            this.plugin.getLogger().severe("An error occurred while fetching warns!");
            return "";
        }
        if (warns.isEmpty()) {
            return ChatColor.GREEN + "No warns found!";
        }

        for (Warning warn : warns) {
            String warner;
            try {
                warner = this.plugin.getServer().getOfflinePlayer(UUID.fromString(warn.warner)).getName();
            } catch (IllegalArgumentException   e) {
                if (warn.warner.equals("CONSOLE") || warn.warner.equals("VULCAN")) {
                    warner = warn.warner;
                } else {
                    this.plugin.getLogger().severe("Could not parse warner '" + warn.warner + "' as UUID!");
                    warner = "Unknown";
                }
            }
            ChatColor color;
            if (warn.type.equals("+")) {
                color = ChatColor.RED;
            } else {
                color = ChatColor.GREEN;
            }
            returnMessage.append(color).append(warn.type).append("1 ").append(ChatColor.GRAY).append("- ").append(ChatColor.YELLOW).append(warn.reason).append(ChatColor.GRAY).append(" from ").append(ChatColor.AQUA).append(warner).append("\n");
        }
        try {
            returnMessage.append(ChatColor.GREEN).append("Total warns: ").append(ChatColor.RED).append(warningDB.getWarningCount(uuid));
        } catch (SQLException e) {
            e.printStackTrace();
            this.plugin.getLogger().severe("An error occurred while fetching warns!");
        }
        return returnMessage.toString();
    }
}
