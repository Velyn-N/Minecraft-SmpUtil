package me.velyn.smputil.horsedotzip;

import static me.velyn.smputil.horsedotzip.HorseDotZipHelper.*;

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
    public static final String CMD_NAME = "horsedotzip";
    private static final String ZIP_SUB_CMD = "zip";
    private static final String UNZIP_SUB_CMD = "unzip";
    private static final String CHECK_SUB_CMD = "check";
    private static final String DEBUG_SUB_CMD = "debug";
    private static final String DEBUG_OUTPUT_SUB_CMD = "output";
    private static final String ZIP_CMD = "horse." + ZIP_SUB_CMD;
    private static final String UNZIP_CMD = "horse." + UNZIP_SUB_CMD;
    private static final String CHECK_CMD = "horse." + CHECK_SUB_CMD;

    public HorseDotZipCommand() {
        super(CMD_NAME, "Zip your rideable mob into a saddle", "/horsedotzip <zip|unzip|check|debug>", List.of());
        setAliases(List.of(ZIP_CMD, UNZIP_CMD, CHECK_CMD, ZIP_SUB_CMD, UNZIP_SUB_CMD));
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
            Entity vehicle = player.getVehicle();
            switch (args[0].toLowerCase()) {
                case ZIP_SUB_CMD -> {
                    if (!(vehicle instanceof AbstractHorse horse)) {
                        player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
                        return false;
                    }
                    saveRideable(player, horse);
                }
                case UNZIP_SUB_CMD -> releaseRideable(player);
                case CHECK_SUB_CMD -> {
                    if (!(vehicle instanceof AbstractHorse horse)) {
                        player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
                        return false;
                    }
                    checkRideable(player, horse);
                }
                case DEBUG_SUB_CMD -> handleDebug(player, args);
                default -> {
                    player.sendMessage(Component.text("Unknown sub-command.", NamedTextColor.RED));
                    return false;
                }
            }
        } else {
            Entity vehicle = player.getVehicle();
            switch (alias.toLowerCase()) {
                case ZIP_CMD, ZIP_SUB_CMD -> {
                    if (!(vehicle instanceof AbstractHorse horse)) {
                        player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
                        return false;
                    }
                    saveRideable(player, horse);
                }
                case UNZIP_CMD, UNZIP_SUB_CMD -> releaseRideable(player);
                case CHECK_CMD -> {
                    if (!(vehicle instanceof AbstractHorse horse)) {
                        player.sendMessage(Component.text("You must be riding a horse, donkey, mule, camel, or llama!", NamedTextColor.RED));
                        return false;
                    }
                    checkRideable(player, horse);
                }
                default -> {
                    player.sendMessage(Component.text("Unknown sub-command.", NamedTextColor.RED));
                    return false;
                }
            }
        }
        return true;
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
}
