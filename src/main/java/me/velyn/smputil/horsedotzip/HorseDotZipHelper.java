package me.velyn.smputil.horsedotzip;

import java.io.*;
import java.util.*;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import org.bukkit.util.io.*;
import org.yaml.snakeyaml.external.biz.base64Coder.*;

import com.google.gson.*;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.plain.*;

public class HorseDotZipHelper {
    public static final NamespacedKey DATA_KEY = new NamespacedKey("horsedotzip", "data");

    private static final Gson GSON = new Gson();

    public static void checkRideable(Player player, AbstractHorse horse) {
        Component message = Component.text("Running diagnostics on ", NamedTextColor.GRAY)
                .append(horse.customName() != null ? horse.customName() : Component.text(horse.getType().name()))
                .append(Component.text("...", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(getHorseStats(horse));
        player.sendMessage(message);
    }

    public static void saveRideable(Player player, AbstractHorse horse) {
        JsonObject json = serializeMob(horse);
        ItemStack saddle = new ItemStack(Material.SADDLE);
        ItemMeta meta = saddle.getItemMeta();

        meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, GSON.toJson(json));

        Component displayName = horse.customName() != null ? horse.customName() : Component.text(horse.getType().name());
        if (displayName != null) {
            meta.displayName(Component.text("Horse.zip: ", NamedTextColor.GOLD).append(displayName.color(NamedTextColor.YELLOW)));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Contains a compressed " + horse.getType().name().toLowerCase(), NamedTextColor.GRAY));
        lore.add(getHorseStats(horse));
        meta.lore(lore);

        saddle.setItemMeta(meta);
        horse.remove();
        HashMap<Integer, ItemStack> unstoredItems = player.getInventory().addItem(saddle);
        unstoredItems.forEach((slot, item) ->
                player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(Component.text("Mob compressed into saddle!", NamedTextColor.GREEN));
        if (!unstoredItems.isEmpty()) {
            player.sendMessage(Component.text("The saddle has been dropped as your inventory is full!", NamedTextColor.YELLOW));
        }
    }

    private static Component getHorseStats(AbstractHorse horse) {
        Component component = Component.empty();
        AttributeInstance healthAttr = horse.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            component = component.append(
                    Component.text("%.1f♥ ".formatted(healthAttr.getValue() / 2), TextColor.color(165, 42, 42))
            );
        }
        AttributeInstance speedAttr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            component = component.append(
                    Component.text("%.4f→ ".formatted(speedAttr.getValue()), TextColor.color(205, 127, 50))
            );
        }
        AttributeInstance jumpAttr = horse.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpAttr != null) {
            component = component.append(
                    Component.text("%.2f↑".formatted(jumpAttr.getValue()), TextColor.color(123, 63, 0))
            );
        }
        return component;
    }

    private static JsonObject serializeMob(AbstractHorse horse) {
        JsonObject root = new JsonObject();
        root.addProperty("type", horse.getType().name());
        root.addProperty("health", horse.getHealth());
        root.addProperty("tamed", horse.isTamed());

        if (horse.customName() != null) {
            root.addProperty("name", PlainTextComponentSerializer.plainText().serialize(horse.customName()));
        }

        JsonObject attrs = new JsonObject();
        Registry.ATTRIBUTE.forEach(attr -> {
            AttributeInstance instance = horse.getAttribute(attr);
            if (instance != null) {
                attrs.addProperty(attr.key().asString(), instance.getBaseValue());
            }
        });
        root.add("attributes", attrs);

        if (horse instanceof Horse h) {
            root.addProperty("color", h.getColor().name());
            root.addProperty("style", h.getStyle().name());
        } else if (horse instanceof Llama l) {
            root.addProperty("color", l.getColor().name());
        }

        root.addProperty("inventory", itemStackArrayToBase64(horse.getInventory().getContents()));

        if (horse instanceof ChestedHorse chested && chested.isCarryingChest()) {
            root.addProperty("hasChest", true);
        }

        return root;
    }

    public static void releaseRideable(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SADDLE || !item.hasItemMeta()) {
            return;
        }

        String rawData = item.getItemMeta().getPersistentDataContainer().get(DATA_KEY, PersistentDataType.STRING);
        if (rawData == null) {
            return;
        }

        try {
            JsonObject json = GSON.fromJson(rawData, JsonObject.class);
            AbstractHorse horse = (AbstractHorse) player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(json.get("type").getAsString()));

            applyData(horse, json);

            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("Mob released!", NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to release mob: corrupted data.", NamedTextColor.RED));
        }
    }

    private static void applyData(AbstractHorse horse, JsonObject json) {
        if (json.has("name")) {
            horse.customName(Component.text(json.get("name").getAsString()));
        }
        horse.setTamed(json.get("tamed").getAsBoolean());

        JsonObject attrs = json.getAsJsonObject("attributes");
        for (String key : attrs.keySet()) {
            try {
                NamespacedKey attrKey = NamespacedKey.fromString(key);
                if (attrKey != null) {
                    Attribute attr = Registry.ATTRIBUTE.get(attrKey);
                    if (attr != null) {
                        AttributeInstance inst = horse.getAttribute(attr);
                        if (inst != null) {
                            inst.setBaseValue(attrs.get(key).getAsDouble());
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        horse.setHealth(json.get("health").getAsDouble());

        if (horse instanceof Horse h) {
            h.setColor(Horse.Color.valueOf(json.get("color").getAsString()));
            h.setStyle(Horse.Style.valueOf(json.get("style").getAsString()));
        } else if (horse instanceof Llama l) {
            l.setColor(Llama.Color.valueOf(json.get("color").getAsString()));
        }

        if (json.has("hasChest") && horse instanceof ChestedHorse chested) {
            chested.setCarryingChest(true);
        }

        ItemStack[] items = itemStackArrayFromBase64(json.get("inventory").getAsString());
        horse.getInventory().setContents(items);
    }

    /**
     * Standard Bukkit Base64 Serialization for ItemStacks
     */
    private static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            return Base64Coder.encodeLines(ItemStack.serializeItemsAsBytes(items));
        } catch (Exception e) {
            return "";
        }
    }

    private static ItemStack[] itemStackArrayFromBase64(String data) {
        try {
            return ItemStack.deserializeItemsFromBytes(Base64Coder.decodeLines(data));
        } catch (Exception e) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                 BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                ItemStack[] items = new ItemStack[dataInput.readInt()];
                for (int i = 0; i < items.length; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }
                return items;
            } catch (Exception ignored) {
                return new ItemStack[0];
            }
        }
    }
}
