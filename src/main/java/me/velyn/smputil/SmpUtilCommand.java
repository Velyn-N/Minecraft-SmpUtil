package me.velyn.smputil;

import java.util.*;

import org.bukkit.command.*;
import org.jetbrains.annotations.*;

public class SmpUtilCommand extends Command {
    public static final String CMD_NAME = "smputil";
    public static final String PERMISSION = "smputil.admin";

    private final SmpUtil plugin;

    protected SmpUtilCommand(SmpUtil plugin) {
        super(CMD_NAME, "Administrative Command for SmpUtil", "/smputil", List.of());
        this.plugin = plugin;
        setPermission(PERMISSION);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args) throws IllegalArgumentException {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length > 1) {
            return Collections.emptyList();
        }
        return List.of("reload");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String cmdName, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return false;
        }
        String subCmd = args.length > 0 ? args[0] : "";
        switch (subCmd) {
            case "reload" -> plugin.reloadConfig();
            default -> {
                sender.sendMessage("Unknown subcommand. Usage: /smputil <subcmd>");
                return false;
            }
        }
        return true;
    }
}
