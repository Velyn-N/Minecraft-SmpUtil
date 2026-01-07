package me.velyn.smputil;

import org.bukkit.event.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.roleplaycauldron.spellbook.core.*;

import me.velyn.smputil.notrample.*;

public final class SmpUtil extends JavaPlugin {

    private WrappedLogger log;

    private PluginConfig config;

    @Override
    public void onEnable() {
        log = new WrappedLogger(getLogger());

        saveDefaultConfig();
        reloadConfig();

        getServer().getCommandMap().register(SmpUtilCommand.CMD_NAME, new SmpUtilCommand(this));

        log.infoF("Successfully enabled SmpUtil!");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        HandlerList.unregisterAll(this);

        if (config.blockFarmlandTrample) {
            pm.registerEvents(new TrampleBlockListener(log, config), this);
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
        log.infoF("Successfully reloaded SmpUtil!");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        log.infoF("Successfully disabled SmpUtil!");
    }
}
