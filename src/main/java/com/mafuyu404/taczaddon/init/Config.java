package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.TACZaddon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-owned presentation and input preferences.
 *
 * Server-authoritative gunsmith policies such as nearby-container access,
 * scan radius, and batch limits belong in CommonConfig.
 */
@Mod.EventBusSubscriber(
        modid = TACZaddon.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue BETTER_AIM_CAMERA;
    public static final ForgeConfigSpec.BooleanValue GUNSMITHTABLE_CRAFT_TOAST;
    public static final ForgeConfigSpec.BooleanValue SHOW_ATTACHMENT_ATTRIBUTE;
    public static final ForgeConfigSpec.IntValue LESS_ALLOW_GUN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            MELEE_WEAPON_LIST;
    public static final ForgeConfigSpec.BooleanValue FAST_SWAP_GUN;
    public static final ForgeConfigSpec.BooleanValue SHOW_ITEM_RELATION;
    public static final ForgeConfigSpec.BooleanValue SHOOT_WHILE_RELOADING;

    public static final ForgeConfigSpec SPEC;

    private static final Set<String> ITEM_BLACKLIST =
            new HashSet<>();

    static {
        BUILDER.push("Melee Setting");

        MELEE_WEAPON_LIST = BUILDER
                .comment(
                        "列表里的枪械会作为近战武器使用，开火将被替换为近战攻击。",
                        "可以通过 F3+H 查看物品的 GunId 标签。"
                )
                .defineList(
                        "MeleeWeaponList",
                        List.of("tacz:type_82", "tacz:type_83"),
                        entry -> entry instanceof String value
                                && ResourceLocation.tryParse(value) != null
                );

        BUILDER.pop();

        BUILDER.push("Gun Setting");

        SHOOT_WHILE_RELOADING = BUILDER
                .comment(
                        "允许在换弹过程中按下开火键。",
                        "仅在枪械当前确实存在可发射弹药且其它射击冷却允许时取消换弹。",
                        "实际射击仍由 TaCZ 客户端和服务端正常校验。"
                )
                .define("enableShootWhileReloading", true);

        BUILDER.pop();

        BUILDER.push("GunSmithTable Presentation");

        GUNSMITHTABLE_CRAFT_TOAST = BUILDER
                .comment("枪械工作台制作成功后显示物品提示。")
                .define("enableCraftToast", true);

        BUILDER.pop();

        BUILDER.push("Attachment Setting");

        LESS_ALLOW_GUN = BUILDER
                .comment("配件高级提示中最多显示的适用枪械数量。")
                .defineInRange(
                        "allowGunDisplayCount",
                        16,
                        1,
                        256
                );

        SHOW_ATTACHMENT_ATTRIBUTE = BUILDER
                .comment("配件提示中显示详细属性变化。")
                .define("enableAttachmentDetail", true);

        BUILDER.pop();

        BUILDER.push("Other Setting");

        BETTER_AIM_CAMERA = BUILDER
                .comment(
                        "非第一人称视角瞄准时自动切换到第一人称，",
                        "取消瞄准后恢复原视角。"
                )
                .define("enableBetterAimCamera", true);

        FAST_SWAP_GUN = BUILDER
                .comment("切枪时跳过收枪后摇。")
                .define("enableFastSwapGun", true);

        SHOW_ITEM_RELATION = BUILDER
                .comment("悬停物品时高亮显示相关物品。")
                .define("enableShowItemRelation", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {
    }

    @SubscribeEvent
    public static void onConfigLoad(
            ModConfigEvent.Loading event
    ) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(
            ModConfigEvent.Reloading event
    ) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    private static void updateItemBlacklist() {
        ITEM_BLACKLIST.clear();

        List<? extends String> configuredItems =
                MELEE_WEAPON_LIST.get();
        if (configuredItems == null) {
            return;
        }

        for (String configuredId : configuredItems) {
            ResourceLocation id =
                    ResourceLocation.tryParse(configuredId);
            if (id != null) {
                ITEM_BLACKLIST.add(id.toString());
            }
        }
    }

    public static boolean isItemInBlacklist(
            ItemStack itemStack
    ) {
        if (itemStack.isEmpty()) {
            return false;
        }

        CompoundTag tag = itemStack.getTag();
        if (tag == null) {
            return false;
        }

        String gunId = tag.getString("GunId");
        return !gunId.isEmpty()
                && ITEM_BLACKLIST.contains(gunId);
    }

    public static boolean enableShootWhileReloading() {
        return SHOOT_WHILE_RELOADING.get();
    }

    public static boolean enableBetterAimCamera() {
        return BETTER_AIM_CAMERA.get();
    }

    public static boolean enableGunSmithTableCraftToast() {
        return GUNSMITHTABLE_CRAFT_TOAST.get();
    }

    public static int getAllowGunAmount() {
        return LESS_ALLOW_GUN.get();
    }
}
