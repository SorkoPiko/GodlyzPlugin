package com.sorkopiko.godlyzbox.listeners;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.DiscordDB;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Tickets extends ListenerAdapter {
    private final JDA api;
    private final GodlyzPlugin plugin;
    private final DiscordDB discordDB;

    public Tickets(GodlyzPlugin plugin) {
        this.plugin = plugin;
        this.discordDB = this.plugin.getDiscordDB();
        this.api = plugin.getJDA();
        if (this.api == null) {
            return;
        }
        this.api.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket-setup")) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Staff Applications");
        }
    }
}
