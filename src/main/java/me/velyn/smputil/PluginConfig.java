package me.velyn.smputil;

import org.bukkit.configuration.*;

public class PluginConfig {

    public boolean debug;
    public boolean blockFarmlandTrample;
    public boolean allowFarmlandTrampleWhenSneaking;
    public boolean horseDotZip;

    public void apply(ConfigurationSection config) {
        debug = config.getBoolean("debug", false);
        blockFarmlandTrample = config.getBoolean("block-farmland-trample", true);
        allowFarmlandTrampleWhenSneaking = config.getBoolean("allow-farmland-trample-when-sneaking", false);
        horseDotZip = config.getBoolean("horse-dot-zip", true);
    }
}
