package com.sorkopiko.godlyzbox;


import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GodlyzBot extends ListenerAdapter {

    private final GodlyzPlugin plugin;

    public GodlyzBot(GodlyzPlugin plugin) {
        this.plugin = plugin;
    }

    public static void main(String[] args) {
        //Initialize Discord bot using JDA

    }
}
