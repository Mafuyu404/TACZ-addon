package com.mafuyu404.taczaddon;

import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TACZaddon.MODID)
public class TACZaddon
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "taczaddon";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public TACZaddon()
    {

        LOGGER.info("HELLO FROM COMMON SETUP");
        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(new RuleRegistry());
        MinecraftForge.EVENT_BUS.register(this);

    }
}
