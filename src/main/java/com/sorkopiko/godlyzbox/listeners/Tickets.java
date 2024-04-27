package com.sorkopiko.godlyzbox.listeners;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.DiscordDB;
import com.sorkopiko.godlyzbox.database.WarningDB;
import com.sorkopiko.godlyzbox.types.Warning;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.*;

public class Tickets extends ListenerAdapter {
    private final JDA jda;
    private final GodlyzPlugin plugin;
    private final DiscordDB discordDB;
    private final WarningDB warningDB;
    private final Chat chat;
    private final LuckPerms luckPerms;

    public Tickets(GodlyzPlugin plugin) {
        this.plugin = plugin;
        this.discordDB = this.plugin.getDiscordDB();
        this.warningDB = this.plugin.getWarningDB();
        this.jda = plugin.getJDA();
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            chat = rsp.getProvider();
        } else {
            this.plugin.getLogger().severe("Vault not found! Please make sure Vault is installed!");
            chat = null;
        }
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            this.plugin.getLogger().severe("LuckPerms not found! Please make sure LuckPerms is installed!");
            luckPerms = null;
        }
        if (this.jda == null) {
            return;
        }
        this.jda.addEventListener(this);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String categoryName;
        String channelTopic;
        InteractionHook hook;
        if (event.getComponentId().equals("general-ticket")) {
            hook = event.deferReply().setEphemeral(true).complete();
            categoryName = "General Tickets";
            channelTopic = "General Ticket";
        }
        else if (event.getComponentId().equals("staff-application")) {
            hook = event.deferReply().setEphemeral(true).complete();
            categoryName = "Staff Applications";
            channelTopic = "Staff Application";
        }
        else if (event.getComponentId().equals("bug-report")) {
            hook = event.deferReply().setEphemeral(true).complete();
            categoryName = "Bug Reports";
            channelTopic = "Bug Report";
        }
        else if (event.getComponentId().equals("close-ticket")) {
            event.reply("Are you sure you want to close the ticket?")
                    .addActionRow(
                            Button.success("confirm-close", "Yes"),
                            Button.danger("cancel-close", "No")
                    ).queue();
            return;
        }
        else if (event.getComponentId().equals("confirm-close")) {
            event.getChannel().delete().queue();
            return;
        }
        else if (event.getComponentId().equals("cancel-close")) {
            event.getMessage().delete().queue();
            return;
        }
        else if (event.getComponentId().equals("refresh-info")) {
            hook = event.deferReply().setEphemeral(true).complete();
            try {
                OfflinePlayer player = this.plugin.getServer().getOfflinePlayer(discordDB.getLinkedMC(Long.parseLong(event.getChannel().getName().substring(2))));
                event.getMessage().editMessage("").queue();
                event.getMessage().editMessageEmbeds(playerInfoEmbed(player).build(), playerWarnEmbed(player).build(), serverInfoEmbed(player).build()).queue();
            } catch (Exception e) {
                e.printStackTrace();
                hook.editOriginal("An error occurred while refreshing!").queue();
                return;
            }
            hook.editOriginal("Refreshed!").queue();
            return;
        }
        else {
            return;
        }

        String staffRoleString = plugin.getConfig().getString("bot.staffRole");
        Role staffRole;
        String staffRoleMention;

        try {
            staffRole = event.getGuild().getRoleById(staffRoleString);
        } catch (NumberFormatException e) {
            staffRole = null;
        }

        if (staffRole != null) {
            staffRoleMention = staffRole.getAsMention();
        } else {
            plugin.getLogger().severe("Staff role not found! Please set the staff role ID in the config.yml file!");
            hook.editOriginal("An error occurred while creating the ticket!").queue();
            return;
        }

        EnumSet<Permission> permissions = EnumSet.of(Permission.VIEW_CHANNEL);

        OfflinePlayer player;
        try {
            player = this.plugin.getServer().getOfflinePlayer(discordDB.getLinkedMC(event.getUser().getIdLong()));
        } catch (Exception e) {
            hook.editOriginal("You must verify your account to create a ticket!").queue();
            return;
        }

        List<Category> categories = event.getGuild().getCategoriesByName(categoryName, true);
        Category category;
        if (!categories.isEmpty()) {
            category = categories.get(0);
        } else {
            category = event.getGuild().createCategory(categoryName).addPermissionOverride(event.getGuild().getPublicRole(), null, permissions).addPermissionOverride(staffRole, permissions, null).complete();
        }
        List<GuildChannel> channels = category.getChannels();
        // check if the channel already exists
        for (GuildChannel channel : channels) {
            if (channel.getName().equals("t-" + event.getUser().getId())) {
                hook.editOriginal("You already have a ticket!").queue();
                return;
            }
        }
        TextChannel channel = event.getGuild().createTextChannel("t-" + event.getUser().getId())
                .setParent(category)
                .setTopic(channelTopic)
                .syncPermissionOverrides()
                .addPermissionOverride(event.getMember(), permissions, null).complete();

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(channelTopic);
        embed.setDescription("Support will be with you shortly!\nTo close the ticket, click the button below.");
        embed.setColor(0x00ff00);
        embed.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

        EmbedBuilder playerInfo = playerInfoEmbed(player);
        EmbedBuilder playerWarn = playerWarnEmbed(player);
        EmbedBuilder serverInfo = serverInfoEmbed(player);

        channel.sendMessage("")
                .setEmbeds(playerInfo.build(), playerWarn.build(), serverInfo.build())
                .setActionRow(
                        Button.primary("refresh-info", "\uD83D\uDD04 Refresh")
                ).queue();

        channel.sendMessage(staffRoleMention + ", " + event.getUser().getAsMention())
                .setEmbeds(embed.build())
                .setActionRow(
                        Button.danger("close-ticket", "\uD83D\uDD12 Close ticket")
                ).queue();

        hook.editOriginal("✅ Ticket created: " + channel.getAsMention()).queue();
    }

    private @NotNull EmbedBuilder playerInfoEmbed(OfflinePlayer player) {
        EmbedBuilder playerInfo = new EmbedBuilder();

        playerInfo.setTitle("Player Information");
        playerInfo.setThumbnail("https://mc-heads.net/avatar/" + player.getUniqueId());
        playerInfo.addField("Username", player.getName(), false);
        playerInfo.addField("UUID", "`" + player.getUniqueId() + "`", false);
        playerInfo.addField("First Join", "<t:" + player.getFirstPlayed() /1000 + ":f> (<t:" + player.getFirstPlayed()/1000 + ":R>)", false);
        playerInfo.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");
        playerInfo.setColor(0xffff00);
        return playerInfo;
    }

    private @NotNull EmbedBuilder playerWarnEmbed(OfflinePlayer player) {
        EmbedBuilder playerWarn = new EmbedBuilder();
        List<Warning> warnings;
        StringBuilder description = new StringBuilder();
        Integer total = 0;
        Integer warns = 0;
        Integer unwarns = 0;

        playerWarn.setTitle("Player Warnings");
        playerWarn.setColor(0xff0000);
        playerWarn.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

        try {
            warnings = this.warningDB.warnList(player.getUniqueId());
        } catch (SQLException e) {
            this.plugin.getLogger().severe("An error occurred while fetching the player's warnings!");
            e.printStackTrace();
            return playerWarn;
        }
        if (warnings.isEmpty()) {
            playerWarn.setDescription("No warnings found!");
            return playerWarn;
        }

        for (Warning warning : warnings) {
            total += warning.type.equals("+") ? 1 : -1;
            if (warning.type.equals("+")) {
                warns++;
            } else {
                unwarns++;
            }
            String warner;
            try {
                warner = this.plugin.getServer().getOfflinePlayer(UUID.fromString(warning.warner)).getName();
            } catch (IllegalArgumentException   e) {
                if (warning.warner.equals("CONSOLE") || warning.warner.equals("VULCAN")) {
                    warner = warning.warner;
                } else {
                    this.plugin.getLogger().severe("Could not parse warner '" + warning.warner + "' as UUID!");
                    warner = "Unknown";
                }
            }
            description.append(" **").append(warning.type).append("**1 (Total: ").append(total).append(") - ").append(warning.reason).append(" **FROM** ").append(warner).append(" (<t:").append(warning.timestamp.toEpochSecond(ZoneOffset.UTC)).append(":R>) `").append(warning.id).append("`\n");
        }

        playerWarn.setDescription("**Warn History**\n" + description + "---------------------------------------\nCurrent Warns: **" + total + "** (Total Warns: **" + warns + "** | Total Unwarns: **" + unwarns + "**)");
        return playerWarn;
    }

    private @NotNull EmbedBuilder serverInfoEmbed(OfflinePlayer player) {
        EmbedBuilder serverInfo = new EmbedBuilder();

        User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());

        serverInfo.setTitle("Server Information");
        serverInfo.addField("Playtime", PlaceholderAPI.setPlaceholders(player, "%godlyzbox_playtime% (#%ajlb_position_godlyzbox_playtime_alltime%)"), false);
        serverInfo.addField("Blocks Mined", PlaceholderAPI.setPlaceholders(player, "%godlyzbox_blocks% (#%ajlb_position_godlyzbox_blocks_alltime%)"), false);
        serverInfo.addField("Kills", PlaceholderAPI.setPlaceholders(player, "%godlyzbox_kills% (#%ajlb_position_godlyzbox_kills_alltime%)"), false);
        serverInfo.addField("Deaths", PlaceholderAPI.setPlaceholders(player, "%godlyzbox_deaths%"), false);
        serverInfo.addField("Rank", user.getPrimaryGroup(), false);
        serverInfo.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");
        serverInfo.setColor(0x0000ff);

        return serverInfo;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket-setup")) {
            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle("General Tickets");
            embed.setDescription("\uD83D\uDCE9 To create a ticket, click the button below!");
            embed.setColor(0x00ff00);
            embed.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

            event.getChannel().sendMessageEmbeds(embed.build()).addActionRow(
                    Button.primary("general-ticket", "Create a ticket")
            ).queue();

            embed.setTitle("Staff Applications");
            embed.setDescription("\uD83D\uDCE9 To apply for staff, click the button below!");
            embed.setColor(0x00ff00);
            embed.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

            event.getChannel().sendMessageEmbeds(embed.build()).addActionRow(
                    Button.success("staff-application", "Apply for staff")
            ).queue();

            embed.setTitle("Bug Reports");
            embed.setDescription("\uD83D\uDCE9 To report a bug, click the button below!");
            embed.setColor(0x00ff00);
            embed.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

            event.getChannel().sendMessageEmbeds(embed.build()).addActionRow(
                    Button.danger("bug-report", "Report a bug")
            ).queue();

            event.getChannel().sendMessage("⚠️ **THE SERVER MUST BE RUNNING TO CREATE A TICKET** ⚠️").queue();
        }
    }
}
