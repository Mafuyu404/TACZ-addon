package com.mafuyu404.taczaddon;

import com.mafuyu404.taczaddon.event.SetupEvent;
import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.ModRecipeSerializers;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.RuleRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(TACZaddon.MODID)
public class TACZaddon {
    public static final String MODID = "taczaddon";

    public TACZaddon(IEventBus bus, ModContainer modContainer) {
        NetworkHandler.register(bus);
        ModRecipeSerializers.SERIALIZERS.register(bus);
        bus.addListener(Config::onConfigLoad);
        bus.addListener(Config::onConfigReload);
        bus.addListener(SetupEvent::onClientSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "taczaddon-common.toml");
        RuleRegistry.init();
    }
}
