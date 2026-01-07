package me.velyn.smputil.notrample;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

import com.github.roleplaycauldron.spellbook.core.*;

import me.velyn.smputil.*;

public class TrampleBlockListener implements Listener {

    private final WrappedLogger log;
    private final PluginConfig config;

    public TrampleBlockListener(WrappedLogger log, PluginConfig config) {
        this.log = log;
        this.config = config;
    }

    @EventHandler
    public void onFarmlandChange(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND) {
            return;
        }

        if (config.allowFarmlandTrampleWhenSneaking
                && event.getEntity() instanceof Player player
                && player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        log.debugF("Farmland trample prevented by entity %s at %s",
                event.getEntity().getName(), event.getBlock().getLocation());
    }
}
