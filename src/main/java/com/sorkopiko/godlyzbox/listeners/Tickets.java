package com.sorkopiko.godlyzbox.listeners;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.DiscordDB;
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
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.bukkit.OfflinePlayer;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String categoryName;
        String channelTopic;
        InteractionHook hook;
        if (event.getComponentId().equals("general-ticket")) {
            hook = event.reply("Creating a ticket...").setEphemeral(true).complete();
            categoryName = "General Tickets";
            channelTopic = "General Ticket";
        }
        else if (event.getComponentId().equals("staff-application")) {
            hook = event.reply("Creating a staff application...").setEphemeral(true).complete();
            categoryName = "Staff Applications";
            channelTopic = "Staff Application";
        }
        else if (event.getComponentId().equals("bug-report")) {
            hook = event.reply("Creating a bug report...").setEphemeral(true).complete();
            categoryName = "Bug Reports";
            channelTopic = "Bug Report";
        }
        else if (event.getComponentId().equals("close-ticket")) {
            event.getChannel().sendMessage("Are you sure you want to close the ticket?")
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
        else {
            return;
        }

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
            EnumSet<Permission> permissions = EnumSet.of(Permission.VIEW_CHANNEL);
            ChannelAction<Category> categoryAction = event.getGuild().createCategory(categoryName).addPermissionOverride(event.getGuild().getPublicRole(), null, permissions);
            category = categoryAction.complete();
        }
        List<GuildChannel> channels = category.getChannels();
        // check if the channel already exists
        for (GuildChannel channel : channels) {
            if (channel.getName().equals("t-" + event.getUser().getId())) {
                hook.editOriginal("You already have a ticket!").queue();
                return;
            }
        }
        EnumSet<Permission> permissions = EnumSet.of(Permission.VIEW_CHANNEL);
        TextChannel channel = event.getGuild().createTextChannel("t-" + event.getUser().getId())
                .setParent(category)
                .setTopic(channelTopic)
                .addPermissionOverride(event.getMember(), null, permissions).complete();

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
            staffRoleMention = "Staff";
            plugin.getLogger().severe("Staff role not found! Please set the staff role ID in the config.yml file!");
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(channelTopic);
        embed.setDescription("Support will be with you shortly!\nTo close the ticket, click the button below.");
        embed.setColor(0x00ff00);
        embed.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");

        EmbedBuilder playerInfo = new EmbedBuilder();

        playerInfo.setTitle("Player Information");
        playerInfo.addField("Username", player.getName(), true);
        playerInfo.addField("UUID", player.getUniqueId().toString(), true);
        playerInfo.addField("First Join", "<t:" + Math.round(player.getFirstPlayed()/1000) + ":f> (<t:" + Math.round(player.getFirstPlayed()/1000) + ":R>", true);
        playerInfo.setFooter("SorkoPiko", "https://cdn.discordapp.com/avatars/609544328737456149/be44b3b9d13b875a42c9ddc1aa503fcf.png?size=4096");
        playerInfo.setColor(0xffff00);

        channel.sendMessage(staffRoleMention + ", " + event.getUser().getAsMention())
                .setEmbeds(embed.build(), playerInfo.build())
                .setActionRow(
                        Button.danger("close-ticket", "\uD83D\uDD12 Close ticket")
                ).queue();

        hook.editOriginal("✅ Ticket created: " + channel.getAsMention()).queue();
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
