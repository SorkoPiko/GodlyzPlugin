package com.sorkopiko.godlyzbox.commands;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class Unwarn implements CommandExecutor {
    private final GodlyzPlugin plugin;

    public Unwarn(GodlyzPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("unwarn").setExecutor(this);
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
            uuid = UUID.fromString(this.plugin.getMojang().getUUIDOfUsername(args[0]));
        } else {
            String uuidStr = this.plugin.getMojang().getUUIDOfUsername(args[0]);
            uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" + uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" + uuidStr.substring(20);
            uuid = UUID.fromString(uuidStr);
        }

        String warner;
        if (!(sender instanceof Player)) {
            warner = "CONSOLE";
        } else {
            warner = ((Player) sender).getUniqueId().toString();
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        this.plugin.warn.warn(uuid, reason, warner, "-");
        return true;
    }
}
