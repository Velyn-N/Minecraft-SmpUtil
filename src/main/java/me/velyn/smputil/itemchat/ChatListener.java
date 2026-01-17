package me.velyn.smputil.itemchat;

import java.util.*;

import org.bukkit.event.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import io.papermc.paper.event.player.*;
import me.velyn.smputil.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.minimessage.*;

public class ChatListener implements Listener {

    private final PluginConfig config;

    public ChatListener(PluginConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInMainHand.isEmpty()) {
            return;
        }
        var finalMsg = event.message();

        for (String placeholder : config.itemChatPlaceholders) {
            Component replText;
            if (config.itemChatReplaceWithItemName) {
                ItemMeta itemMeta = itemInMainHand.getItemMeta();
                if (itemMeta.hasDisplayName()) {
                    replText = Objects.requireNonNull(itemMeta.displayName());
                } else {
                    replText = Component.text(itemInMainHand.getType().name());
                }
            } else {
                replText = MiniMessage.miniMessage().deserialize(placeholder);
            }
            Component output = replText.hoverEvent(itemInMainHand.asHoverEvent());

            finalMsg = finalMsg.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(placeholder)
                    .replacement(output).build());
        }
        event.message(finalMsg);
    }
}
