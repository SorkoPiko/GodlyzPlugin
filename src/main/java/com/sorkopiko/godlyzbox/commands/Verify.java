package com.sorkopiko.godlyzbox.commands;


import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.DiscordDB;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class Verify extends ListenerAdapter implements CommandExecutor {

    private HashMap<UUID, String> verificationCodes = new HashMap<>();
    private HashMap<UUID, Long> pendingVerifications = new HashMap<>();
    private final JDA api;
    private final GodlyzPlugin plugin;
    private final DiscordDB discordDB;

    public Verify(GodlyzPlugin plugin) {
        this.plugin = plugin;
        this.discordDB = this.plugin.getDiscordDB();
        this.api = plugin.getJDA();
        if (this.api == null) {
            return;
        }
        this.api.addEventListener(this);
        plugin.getCommand("verify").setExecutor(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("verify")) {
            try {
                if (discordDB.getLinkedMC(event.getUser().getIdLong()) != null) {
                    event.reply("You are already verified!").setEphemeral(true).queue();
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("An error occurred while verifying!");
                e.printStackTrace();
                event.reply("An error occurred while verifying your account!").setEphemeral(true).queue();
                return;
            }

            Player player = plugin.getServer().getPlayer(event.getOption("username").getAsString());
            if (player == null) {
                event.reply("Please join the server to verify!").setEphemeral(true).queue();
                return;
            }

            String randomCode = generateCode();
            while (verificationCodes.containsValue(randomCode)) {
                randomCode = generateCode();
            }

            verificationCodes.put(player.getUniqueId(), randomCode);
            pendingVerifications.put(player.getUniqueId(), event.getUser().getIdLong());
            event.reply("Run `/verify "+randomCode+"` on Minecraft to verify!").setEphemeral(true).queue();
        }
    }

    private String generateCode() {
        return "V-"+new Random().nextInt(100000, 1000000);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!(sender instanceof Player)){
            return false;
        }

        Player player = (Player) sender;

        if(!verificationCodes.containsKey(player.getUniqueId())){
            player.sendMessage(ChatColor.RED + "You don't have a pending verification session! Please use " + ChatColor.GREEN + "/verify " + player.getName() + ChatColor.RED + " in Discord to start!");
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase(verificationCodes.get(player.getUniqueId()))){
            try {
                User discordUser = api.retrieveUserById(pendingVerifications.get(player.getUniqueId())).complete();
                discordDB.addUser(player.getUniqueId(), pendingVerifications.get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "You have successfully verified your account " + discordUser.getName() + "!");
                verificationCodes.remove(player.getUniqueId());
                pendingVerifications.remove(player.getUniqueId());
            } catch (Exception e) {
                plugin.getLogger().severe("An error occurred while verifying!");
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "An error occurred while verifying your account!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Invalid verification code!");
        }
        return true;
    }
}
