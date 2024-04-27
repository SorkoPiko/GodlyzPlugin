package com.sorkopiko.godlyzbox;

import com.sorkopiko.godlyzbox.commands.Verify;
import com.sorkopiko.godlyzbox.commands.Warn;
import com.sorkopiko.godlyzbox.commands.Warns;
import com.sorkopiko.godlyzbox.commands.CheckWarns;
import com.sorkopiko.godlyzbox.database.DiscordDB;
import com.sorkopiko.godlyzbox.database.WarningDB;
import com.sorkopiko.godlyzbox.listeners.Tickets;
import com.sorkopiko.godlyzbox.listeners.VulcanListener;
import kotlin.text.Charsets;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.shanerx.mojang.Mojang;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class GodlyzPlugin extends JavaPlugin {

    private DiscordDB discordDB;
    private WarningDB warningDB;
    private Tickets tickets;
    private JDA jda;
    private Mojang mojang;
    public Warn warn;
    public Warns warns;

    @Override
    public void onEnable() {
        // Plugin startup logic
        FileConfiguration config = getConfig();
        config.addDefault("bot.enabled", true);
        config.addDefault("bot.staffRole", "YOUR_STAFF_ROLE_ID");
        config.addDefault("bot.token", "YOUR_BOT_TOKEN");
        config.addDefault("messages.warn", ChatColor.RED.toString() + ChatColor.BOLD + "You have been warned: " + ChatColor.YELLOW + "{reason}");
        config.addDefault("messages.unwarn", ChatColor.RED.toString() + ChatColor.BOLD + "You have been unwarned: " + ChatColor.YELLOW + "{reason}");
        config.addDefault("punishments.default.commands", List.of());
        config.addDefault("punishments.1.commands", List.of());
        config.addDefault("punishments.3.commands", List.of("tempban {player} 1d"));
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

        if(!new File("plugins/Vulcan/config.yml").exists()) {
            getLogger().severe("Vulcan not found! Please download it and make sure the Vulcan folder exists!");
            getServer().getPluginManager().disablePlugin(this);
        }

        YamlConfiguration vulcanConfig = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream("plugins/Vulcan/config.yml");
            vulcanConfig.load(new InputStreamReader(stream, Charsets.UTF_8));
            boolean enabled = vulcanConfig.getBoolean("settings.enable-api");
            if (!enabled) {
                getLogger().info("Vulcan API was disabled, enabling it now");
                vulcanConfig.set("settings.enable-api", true);
                vulcanConfig.save(new File("plugins/Vulcan/config.yml"));
                Plugin plugin = Bukkit.getPluginManager().getPlugin("Vulcan");
                Bukkit.getPluginManager().disablePlugin(plugin);
                Bukkit.getPluginManager().enablePlugin(plugin);
            }
        } catch (Exception e) {
            getLogger().severe("There was an error loading Vulcan's config file to check if the API is enabled. Assuming it is enabled...");
        }

        mojang = new Mojang().connect();
        warn = new Warn(this);
        warns = new Warns(this);
        new CheckWarns(this);

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
                    new Verify(this);
                    new Tickets(this);
                    getLogger().info("Bot started successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    getLogger().severe("Failed to start the bot!");
                }
            }
        }
        getServer().getPluginManager().registerEvents(new VulcanListener(this), this);
        getLogger().info("Started successfully!");
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

    public Mojang getMojang() {
        return mojang;
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
