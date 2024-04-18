package com.sorkopiko.godlyzbox;

import com.sorkopiko.godlyzbox.commands.Verify;
import com.sorkopiko.godlyzbox.database.DiscordDB;
import com.sorkopiko.godlyzbox.database.WarningDB;
import com.sorkopiko.godlyzbox.listeners.Tickets;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class GodlyzPlugin extends JavaPlugin {

    private DiscordDB discordDB;
    private WarningDB warningDB;
    private Verify verify;
    private Tickets tickets;
    private JDA jda;

    @Override
    public void onEnable() {
        // Plugin startup logic
        FileConfiguration config = getConfig();
        config.addDefault("bot.enabled", true);
        config.addDefault("bot.staffRole", "YOUR_STAFF_ROLE_ID");
        config.addDefault("bot.token", "YOUR_BOT_TOKEN");
        config.options().copyDefaults(true);
        saveConfig();
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            warningDB = new WarningDB(getDataFolder().getAbsolutePath() + "/warnings.db");
            discordDB = new DiscordDB(getDataFolder().getAbsolutePath() + "/discord.db");
            getLogger().info("Connected to the database!");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to the database!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (config.getBoolean("bot.enabled")) {
            if (Objects.equals(getConfig().getString("bot.token"), "YOUR_BOT_TOKEN")) {
                getLogger().severe("Bot token not found in the config.yml file! Disabling the bot!");
            }
            else {
                try {
                    jda = JDABuilder.createDefault(getConfig().getString("bot.token")).build();
                    jda.awaitReady();
                    jda.updateCommands().addCommands(
                            Commands.slash("verify", "Verify your Discord account with your Minecraft account.")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "username", "Your Minecraft username", true)
                                                    .setMaxLength(16)
                                                    .setMinLength(3)
                                    ),
                            Commands.slash("ticket-setup", "Setup the ticket system for your server.")
                                    .setDefaultPermissions(
                                            DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)
                                    )
                    ).queue();
                    verify = new Verify(this);
                    tickets = new Tickets(this);
                    getLogger().info("Bot started successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    getLogger().severe("Failed to start the bot!");
                }
            }
        }
        getLogger().info("Started successfully!");
        //getServer().getPluginManager().registerEvents(new GodlyzListener(), this);
    }

    public WarningDB getWarningDB() {
        return warningDB;
    }

    public DiscordDB getDiscordDB() {
        return discordDB;
    }

    public JDA getJDA() {
        return jda;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            warningDB.closeConnection();
            getLogger().info("Closed the database connection!");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to close the database connection!");
        }
    }
}
