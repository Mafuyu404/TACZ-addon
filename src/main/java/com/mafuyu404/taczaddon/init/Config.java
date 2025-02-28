package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.TACZaddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TACZaddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MELEE_WEAPON_LIST;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static Set<String> ITEM_BLACKLIST = new HashSet<>();

    static {
        BUILDER.push("Melee Setting");
        MELEE_WEAPON_LIST = BUILDER
                .comment("列表里的枪械会作为近战武器使用，开火将被替换为近战攻击。你可以按F3+H打开高级提示框，查看物品的GunId标签，就像示例的那样。")
                .defineList("MeleeWeaponList",
                        List.of("tacz:type_82", "tacz:type_83"), // 默认值
                        entry -> entry instanceof String
                );
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // 初始化或重载配置时更新缓存
    private static void updateItemBlacklist() {
        ITEM_BLACKLIST.clear();
        for (String itemId : MELEE_WEAPON_LIST.get()) {
            ITEM_BLACKLIST.add(new ResourceLocation(itemId).toString());
        }
    }

    // 监听配置加载事件
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    // 监听配置热重载事件
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    // 检查当前物品是否在黑名单中
    public static boolean isItemInBlacklist(ItemStack itemStack) {
        return ITEM_BLACKLIST.contains(itemStack.getTag().getString("GunId"));
    }
}
