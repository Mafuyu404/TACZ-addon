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
        com.mafuyu404.taczaddon.init.ConfigMigration.migrate(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        NetworkHandler.register(bus);
        ModRecipeSerializers.SERIALIZERS.register(bus);
        bus.addListener(Config::onConfigLoad);
        bus.addListener(Config::onConfigReload);
        bus.addListener(SetupEvent::onClientSetup);
        bus.addListener(com.mafuyu404.taczaddon.init.CommonConfig::onConfigReload);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC, "taczaddon-client.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, com.mafuyu404.taczaddon.init.CommonConfig.SPEC, "taczaddon-common.toml");
        RuleRegistry.init();
    }
}
