package com.sorkopiko.godlyzbox.commands;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class CheckWarns implements CommandExecutor {
    private final GodlyzPlugin plugin;

    public CheckWarns(GodlyzPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("checkwarns").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
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

        sender.sendMessage(this.plugin.warns.warns(uuid));
        return true;
    }
}
