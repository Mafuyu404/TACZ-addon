package com.mafuyu404.taczaddon;

import com.mafuyu404.taczaddon.init.Config;
import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TACZaddon.MODID)
public class TACZaddon
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "taczaddon";

    public TACZaddon()
    {

        NetworkHandler.register();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册配置和事件
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                Config.SPEC,
                "taczaddon-client.toml"
        );

        MinecraftForge.EVENT_BUS.register(new RuleRegistry());

    }
}
