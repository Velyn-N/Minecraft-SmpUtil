package me.velyn.smputil.horsedotzip;

import org.bukkit.*;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.*;

import static me.velyn.smputil.horsedotzip.HorseDotZipHelper.*;

import java.util.*;

import me.velyn.smputil.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;

public class HorseDotZipInteractionListener implements Listener {

    private final Map<UUID, AbstractHorse> markedHorses = new HashMap<>();
    private final Map<UUID, BukkitTask> markedTimeoutTasks = new HashMap<>();

    private final SmpUtil plugin;

    public HorseDotZipInteractionListener(SmpUtil plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHorseInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof AbstractHorse horse)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (markedHorses.containsKey(player.getUniqueId()) && markedHorses.get(player.getUniqueId()).equals(horse)) {
            event.setCancelled(true);
            saveRideable(player, horse);
            markedHorses.remove(player.getUniqueId());
        } else if (player.isSneaking()) {
            event.setCancelled(true);
            markedHorses.put(player.getUniqueId(), horse);
            player.sendMessage(Component.text(
                    "Right click the horse to zip it, left click to check it.", NamedTextColor.YELLOW));
            var task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                AbstractHorse removed = markedHorses.remove(player.getUniqueId());
                if (removed != null) {
                    player.sendMessage(Component.text("No longer marking a horse.", NamedTextColor.YELLOW));
                }
            }, 600);
            BukkitTask oldTask = markedTimeoutTasks.put(player.getUniqueId(), task);
            if (oldTask != null) {
                oldTask.cancel();
            }
        }
    }

    @EventHandler
    public void onHorseDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }

        if (markedHorses.containsKey(player.getUniqueId()) && markedHorses.get(player.getUniqueId()).equals(horse)) {
            event.setCancelled(true);
            checkRideable(player, markedHorses.get(player.getUniqueId()));
            markedHorses.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.SADDLE && item.hasItemMeta()) {
            if (item.getItemMeta().getPersistentDataContainer().has(DATA_KEY, PersistentDataType.STRING)) {
                event.setCancelled(true);
                releaseRideable(player);
            }
        }
    }
}
