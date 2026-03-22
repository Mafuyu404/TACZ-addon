package com.mafuyu404.taczaddon.event;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.compat.JeiCompat;
import com.mafuyu404.taczaddon.compat.ShoulderSurfingCompat;
import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = TACZaddon.MODID)
public class SetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ShoulderSurfingCompat::init);
        event.enqueueWork(JeiCompat::init);
        event.enqueueWork(SophisticatedBackpacksCompat::init);
    }
    @SubscribeEvent
    public static void onServerSetup(FMLDedicatedServerSetupEvent event) {
        event.enqueueWork(SophisticatedBackpacksCompat::init);
    }
//    @SubscribeEvent
//    public static void registerRecipes(RegisterEvent event) {
//        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, helper -> {
//            helper.register(new ResourceLocation(TACZaddon.MODID, "gun_upgrade"),
//                    new SmithingRecipeSerializer<>(ToolUpgradeRecipe::new));
//        });
//    }
}
