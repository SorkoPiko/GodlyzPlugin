package com.sorkopiko.godlyzbox.listeners;

import com.sorkopiko.godlyzbox.GodlyzPlugin;
import com.sorkopiko.godlyzbox.database.WarningDB;
import me.frep.vulcan.api.event.VulcanFlagEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VulcanListener implements Listener {
    private final GodlyzPlugin plugin;

    public VulcanListener(GodlyzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVulcanFlag(VulcanFlagEvent event) {
        if (event.getCheck().getMaxVl() == event.getCheck().getVl()+1 && event.getCheck().isPunishable()) {
            this.plugin.warn.warn(event.getPlayer().getUniqueId(), event.getCheck().getDisplayName() + " [Type " + Character.toUpperCase(event.getCheck().getType()) + "] [VL: " + event.getCheck().getMaxVl() + "]", "VULCAN", "+");
        }
    }
}
