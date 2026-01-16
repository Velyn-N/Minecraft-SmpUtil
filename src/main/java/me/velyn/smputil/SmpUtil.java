package me.velyn.smputil;

import java.util.*;

import org.bukkit.command.*;
import org.bukkit.event.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.roleplaycauldron.spellbook.core.*;

import me.velyn.smputil.horsedotzip.*;
import me.velyn.smputil.itemchat.*;
import me.velyn.smputil.notrample.*;

public final class SmpUtil extends JavaPlugin {

    private WrappedLogger log;

    private PluginConfig config;

    private final List<Command> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        log = new WrappedLogger(getLogger());

        saveDefaultConfig();
        reloadConfig();

        getServer().getCommandMap().register(this.getName(), new SmpUtilCommand(this));

        log.infoF("Successfully enabled SmpUtil!");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        HandlerList.unregisterAll(this);

        if (config.blockFarmlandTrample) {
            pm.registerEvents(new TrampleBlockListener(log, config), this);
        }
        if (config.horseDotZip) {
            pm.registerEvents(new HorseDotZipInteractionListener(this), this);
        }
        if (config.itemChatEnabled) {
            pm.registerEvents(new ChatListener(config), this);
        }
    }

    private void registerCommands() {
        CommandMap cm = getServer().getCommandMap();

        registeredCommands.forEach(c -> c.unregister(cm));
        registeredCommands.clear();

        if (config.horseDotZip) {
            HorseDotZipCommand horseDotZipCommand = new HorseDotZipCommand();
            registeredCommands.add(horseDotZipCommand);
            cm.register(this.getName(), horseDotZipCommand);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (config == null) {
            config = new PluginConfig();
        }
        config.apply(getConfig());
        log.setDebug(config.debug);

        registerListeners();
        registerCommands();
        log.infoF("Successfully reloaded SmpUtil!");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        log.infoF("Successfully disabled SmpUtil!");
    }
}
