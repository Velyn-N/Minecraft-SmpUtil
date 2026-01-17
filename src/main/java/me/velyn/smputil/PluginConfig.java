package me.velyn.smputil;

import java.util.*;

import org.bukkit.configuration.*;

public class PluginConfig {

    public boolean debug;

    public boolean blockFarmlandTrample;
    public boolean allowFarmlandTrampleWhenSneaking;

    public boolean horseDotZip;

    public boolean itemChatEnabled;
    public List<String> itemChatPlaceholders;
    public boolean itemChatReplaceWithItemName;

    public void apply(ConfigurationSection config) {
        debug = config.getBoolean("debug", false);
        blockFarmlandTrample = config.getBoolean("block-farmland-trample", true);
        allowFarmlandTrampleWhenSneaking = config.getBoolean("allow-farmland-trample-when-sneaking", false);

        horseDotZip = config.getBoolean("horse-dot-zip", true);

        itemChatEnabled = config.getBoolean("item-chat.enabled", true);
        itemChatPlaceholders = config.getStringList("item-chat.placeholders");
        if (itemChatPlaceholders.isEmpty()) {
            itemChatPlaceholders = List.of("!item", "[item]");
        }
        itemChatReplaceWithItemName = config.getBoolean("item-chat.replace-with-item-name", true);
    }
}
