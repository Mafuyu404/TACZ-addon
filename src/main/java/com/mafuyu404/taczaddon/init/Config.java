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

    public static final ForgeConfigSpec.ConfigValue<Boolean> BETTER_AIM_CAMERA;
    public static final ForgeConfigSpec.ConfigValue<Boolean> BETTER_GUNSMITHTABLE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> GUNSMITHTABLE_CRAFT_TOAST;
    public static final ForgeConfigSpec.ConfigValue<Boolean> GUNSMITHTABLE_MEMORY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> GUNSMITHTABLE_CONTAINER_READER;
    public static final ForgeConfigSpec.ConfigValue<Integer> LESS_ALLOW_GUN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MELEE_WEAPON_LIST;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    private static Set<String> ITEM_BLACKLIST = new HashSet<>();

    static {
        BUILDER.push("Melee Setting");
        MELEE_WEAPON_LIST = BUILDER
                .comment("列表里的枪械会作为近战武器使用，开火将被替换为近战攻击。你可以按F3+H打开高级提示框，查看物品的GunId标签，就像示例的那样。")
                .defineList("MeleeWeaponList",
                        List.of("tacz:type_82", "tacz:type_83"),
                        entry -> entry instanceof String
                );
        BUILDER.pop();
        BUILDER.push("Camera Setting");
        BETTER_AIM_CAMERA = BUILDER
                .comment("开启后，如果正处于非第一人称视角，使用枪械瞄准将自动切换为第一人称，取消瞄准后切换为原视角。")
                .define("enableBetterAimCamera", true);
        BUILDER.pop();
        BUILDER.push("GunSmithTable Setting");
        BETTER_GUNSMITHTABLE = BUILDER
                .comment("开启后，持枪与枪械工作台互动将只显示可用配件和弹药。")
                .define("enableRecipeFilter", true);
        GUNSMITHTABLE_CRAFT_TOAST = BUILDER
                .comment("开启后，在枪械工作台制造东西时会弹出相关物品提示。")
                .define("enableCraftToast", true);
        GUNSMITHTABLE_MEMORY = BUILDER
                .comment("开启后，打开枪械工作台时将会跳转到上次浏览的位置。")
                .define("enableMemory", true);
        GUNSMITHTABLE_CONTAINER_READER = BUILDER
                .comment("开启后，使用枪械工作台制作东西时将读取周边容器的物品。")
                .define("enableContainerReader", true);
        BUILDER.pop();
        BUILDER.push("Attachment Setting");
        LESS_ALLOW_GUN = BUILDER
                .comment("在这里填入正整数，即对配件按shift时显示的适用枪械数量。")
                .define("enableLessAllowGun", 16);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static void updateItemBlacklist() {
        ITEM_BLACKLIST.clear();
        for (String itemId : MELEE_WEAPON_LIST.get()) {
            ITEM_BLACKLIST.add(new ResourceLocation(itemId).toString());
        }
    }
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    public static boolean isItemInBlacklist(ItemStack itemStack) {
        return ITEM_BLACKLIST.contains(itemStack.getTag().getString("GunId"));
    }
    public static boolean enableBetterAimCamera() {
        return BETTER_AIM_CAMERA.get();
    }
    public static boolean enableBetterGunSmithTable() { return BETTER_GUNSMITHTABLE.get(); }
    public static boolean enableGunSmithTableCraftToast() {
        return GUNSMITHTABLE_CRAFT_TOAST.get();
    }
    public static boolean enableGunSmithTableMemory() {
        return GUNSMITHTABLE_MEMORY.get();
    }
    public static boolean enableGunSmithTableContainerReader() {
        return GUNSMITHTABLE_CONTAINER_READER.get();
    }
    public static int getAllowGunAmount() { return LESS_ALLOW_GUN.get(); }
}
