package com.mafuyu404.taczaddon;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.ModRecipeSerializers;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.init.crafting.WorkbenchAnchorRegistry;
import com.mafuyu404.taczaddon.compat.tacz.TaczWorkbenchAnchorProvider;
import com.mafuyu404.taczaddon.compat.tacz.TaczCompatibility;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TACZaddon.MODID)
public class TACZaddon {
    public static final String MODID = "taczaddon";

    public TACZaddon(FMLJavaModLoadingContext context) {
        TaczCompatibility.logSummary();
        RuleRegistry.bootstrap();
        NetworkHandler.register();
        WorkbenchAnchorRegistry.register(
                new TaczWorkbenchAnchorProvider()
        );

        IEventBus modEventBus = context.getModEventBus();

        ModRecipeSerializers.SERIALIZERS.register(modEventBus);

        context.registerConfig(
                ModConfig.Type.CLIENT,
                Config.SPEC,
                "taczaddon-client.toml"
        );

        context.registerConfig(
                ModConfig.Type.COMMON,
                CommonConfig.SPEC,
                "taczaddon-common.toml"
        );
    }
}
