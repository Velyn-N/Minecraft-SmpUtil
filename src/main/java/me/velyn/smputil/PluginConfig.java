package me.velyn.smputil;

import org.bukkit.configuration.*;

public class PluginConfig {

    public boolean debug;
    public boolean blockFarmlandTrample;

    public void apply(ConfigurationSection config) {
        debug = config.getBoolean("debug", false);
        blockFarmlandTrample = config.getBoolean("block-farmland-trample", true);
    }
}
