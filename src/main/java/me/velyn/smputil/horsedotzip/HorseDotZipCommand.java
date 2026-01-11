package me.velyn.smputil.horsedotzip;

import java.io.*;
import java.util.*;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import org.bukkit.util.io.*;
import org.jetbrains.annotations.*;
import org.yaml.snakeyaml.external.biz.base64Coder.*;

import com.google.gson.*;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.plain.*;

public class HorseDotZipCommand extends Command {
    private static final NamespacedKey DATA_KEY = new NamespacedKey("horsedotzip", "data");

    public static final String CMD_NAME = "horsedotzip";
    private static final String ZIP_SUB_CMD = "zip";
    private static final String UNZIP_SUB_CMD = "unzip";
    private static final String CHECK_SUB_CMD = "check";
    private static final String DEBUG_SUB_CMD = "debug";
    private static final String DEBUG_OUTPUT_SUB_CMD = "output";
    private static final String ZIP_CMD = "horse." + ZIP_SUB_CMD;
    private static final String UNZIP_CMD = "horse." + UNZIP_SUB_CMD;
    private static final String CHECK_CMD = "horse." + CHECK_SUB_CMD;

    private static final Gson GSON = new Gson();

    public HorseDotZipCommand() {
        super(CMD_NAME, "Zip your rideable mob into a saddle", "/horsedotzip <zip|unzip|check|debug>", List.of());
        setAliases(List.of(ZIP_CMD, UNZIP_CMD, CHECK_CMD));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                             @NotNull String alias,
                                             @NotNull String @NotNull [] args) throws IllegalArgumentException {
        if (CMD_NAME.equalsIgnoreCase(alias)) {
            if (args.length == 1) {
                return List.of(ZIP_SUB_CMD, UNZIP_SUB_CMD, CHECK_SUB_CMD, DEBUG_SUB_CMD);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase(DEBUG_SUB_CMD)) {
                return List.of(DEBUG_OUTPUT_SUB_CMD);
            }
        }
        return List.of();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (CMD_NAME.equalsIgnoreCase(alias)) {
            if (args.length == 0) {
                sender.sendMessage(Component.text("Usage: /horsedotzip <zip|unzip|check|debug>", NamedTextColor.RED));
                return false;
            }
            switch (args[0].toLowerCase()) {
                case ZIP_SUB_CMD -> saveRideable(player);
                case UNZIP_SUB_CMD -> releaseRideable(player);
                case CHECK_SUB_CMD -> checkRideable(player);
                case DEBUG_SUB_CMD -> handleDebug(player, args);
                default -> {
                    player.sendMessage(Component.text("Unknown sub-command.", NamedTextColor.RED));
                    return false;
                }
            }
        } else {
            switch (alias.toLowerCase()) {
                case ZIP_CMD -> saveRideable(player);
                case UNZIP_CMD -> releaseRideable(player);
                case CHECK_CMD -> checkRideable(player);
                default -> {
                    player.sendMessage(Component.text("Unknown sub-command.", NamedTextColor.RED));
                    return false;
                }
            }
        }
        return true;
    }

    private void checkRideable(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) {
            player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
            return;
        }
        Component message = Component.text("Running diagnostics on ", NamedTextColor.GRAY)
                .append(horse.customName() != null ? horse.customName() : Component.text(horse.getType().name()))
                .append(Component.text("...", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(getHorseStats(horse));
        player.sendMessage(message);
    }

    private void saveRideable(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) {
            player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
            return;
        }

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
        player.getInventory().addItem(saddle);
        player.sendMessage(Component.text("Mob compressed into saddle!", NamedTextColor.GREEN));
    }

    private Component getHorseStats(AbstractHorse horse) {
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

    private JsonObject serializeMob(AbstractHorse horse) {
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

    private void releaseRideable(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SADDLE || !item.hasItemMeta()) return;

        String rawData = item.getItemMeta().getPersistentDataContainer().get(DATA_KEY, PersistentDataType.STRING);
        if (rawData == null) return;

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

    private void applyData(AbstractHorse horse, JsonObject json) throws Exception {
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

    private void handleDebug(Player player, String[] args) {
        if (args.length < 2) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();

        if (DEBUG_OUTPUT_SUB_CMD.equalsIgnoreCase(args[1])) {
            String data = item.getItemMeta().getPersistentDataContainer().get(DATA_KEY, PersistentDataType.STRING);
            if (data != null) {
                player.sendMessage(Component.text(data)
                        .hoverEvent(HoverEvent.showText(Component.text("Copy to clipboard")))
                        .clickEvent(ClickEvent.copyToClipboard(data)));
            }
        }
    }

    /**
     * Standard Bukkit Base64 Serialization for ItemStacks
     */
    private String itemStackArrayToBase64(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack[] itemStackArrayFromBase64(String data) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        }
    }
}