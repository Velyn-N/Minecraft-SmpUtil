package me.velyn.smputil.itemchat;

import java.util.*;

import org.bukkit.event.*;
import org.bukkit.inventory.*;

import io.papermc.paper.event.player.*;
import me.velyn.smputil.*;
import net.kyori.adventure.text.*;
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
            Component output = MiniMessage.miniMessage().deserialize(placeholder)
                    .hoverEvent(itemInMainHand.asHoverEvent());

            finalMsg = finalMsg.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(placeholder)
                    .replacement(output).build());
        }
        event.message(finalMsg);
    }
}
