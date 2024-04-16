package com.sorkopiko.godlyzbox;

import com.sorkopiko.godlyzbox.database.WarningDB;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class GodlyzPlugin extends JavaPlugin {

    private WarningDB warningDB;
    private GodlyzBot godlyzBot;

    @Override
    public void onEnable() {
        // Plugin startup logic
        FileConfiguration config = getConfig();
        config.addDefault("bot.token", "YOUR_BOT_TOKEN");
        config.options().copyDefaults(true);
        saveConfig();
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            warningDB = new WarningDB(getDataFolder().getAbsolutePath() + "/warnings.db");
            getLogger().info("Connected to the database!");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to the database!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        godlyzBot = new GodlyzBot(this);
        getLogger().info("Started successfully!");
        //getServer().getPluginManager().registerEvents(new GodlyzListener(), this);
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
