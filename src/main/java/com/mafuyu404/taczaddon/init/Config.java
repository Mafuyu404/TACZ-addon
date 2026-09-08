package com.mafuyu404.taczaddon.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Config {
    public static final ModConfigSpec.BooleanValue SHOOT_WHILE_RELOADING;
    public static final ModConfigSpec.IntValue CONTAINER_SCAN_RADIUS;
    public static final ModConfigSpec.BooleanValue SHOW_ITEM_RELATION_IN_SOPHISTICATED_STORAGE;
    public static final ModConfigSpec.BooleanValue BETTER_AIM_CAMERA;
    public static final ModConfigSpec.BooleanValue GUNSMITHTABLE_CRAFT_TOAST;
    public static final ModConfigSpec.BooleanValue GUNSMITHTABLE_CONTAINER_READER;
    public static final ModConfigSpec.BooleanValue SHOW_ATTACHMENT_ATTRIBUTE;
    public static final ModConfigSpec.IntValue LESS_ALLOW_GUN;
    public static final ModConfigSpec.IntValue GUNSMITHTABLE_MASS_CRAFT_TIME;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MELEE_WEAPON_LIST;
    public static final ModConfigSpec.BooleanValue FAST_SWAP_GUN;
    public static final ModConfigSpec.BooleanValue SHOW_ITEM_RELATION;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    private static volatile Set<ResourceLocation> meleeGunIds = Set.of();

    private Config() {
    }

    static {
        BUILDER.push("Melee Setting");
        MELEE_WEAPON_LIST = BUILDER
                .comment("Gun IDs that should be treated as melee weapons.")
                .defineList("meleeGunIds", List.of("tacz:type_82", "tacz:type_83"),
                        null,
                        entry -> entry instanceof String id && ResourceLocation.tryParse(id) != null);
        BUILDER.pop();

        BUILDER.push("Gun Setting");
        SHOOT_WHILE_RELOADING = BUILDER.comment(
                "Allow firing to interrupt an active tactical reload.",
                "The gun must still contain immediately fireable loaded ammunition.",
                "A failed shot must not cancel the reload.")
                .define("enableShootWhileReloading", true);
        BUILDER.pop();

        BUILDER.push("GunSmithTable Setting");
        CONTAINER_SCAN_RADIUS = BUILDER.comment(
                "Horizontal nearby-container scan radius.",
                "Only loaded positions from Y-1 through Y+1 are inspected.")
                .defineInRange("containerScanRadius", 3, 1, 16);
        GUNSMITHTABLE_CRAFT_TOAST = BUILDER
                .comment("Show the actual server-confirmed result after gunsmith crafting.")
                .define("enableCraftToast", true);
        GUNSMITHTABLE_CONTAINER_READER = BUILDER
                .comment("Allow supported gunsmith/refit features to use nearby loaded containers.")
                .define("enableContainerReader", true);
        GUNSMITHTABLE_MASS_CRAFT_TIME = BUILDER
                .comment("Maximum server-authoritative craft executions requested by one shift-click.")
                .defineInRange("massCraftCount", 64, 1, 64);
        BUILDER.pop();

        BUILDER.push("Attachment Setting");
        LESS_ALLOW_GUN = BUILDER
                .comment("Maximum guns shown for attachment compatibility while holding shift.")
                .defineInRange("maxDisplayedCompatibleGuns", 16, 0, 1024);
        SHOW_ATTACHMENT_ATTRIBUTE = BUILDER
                .comment("Show detailed numeric attachment attributes.")
                .define("enableAttachmentDetail", true);
        BUILDER.pop();

        BUILDER.push("Other Setting");
        SHOW_ITEM_RELATION_IN_SOPHISTICATED_STORAGE = BUILDER.comment(
                "Highlight related items inside Sophisticated Backpacks/Storage screens.",
                "Requires enableShowItemRelation.")
                .define("enableShowItemRelationInSophisticatedStorage", true);
        BETTER_AIM_CAMERA = BUILDER
                .comment("Temporarily switch to first person while aiming from another camera mode.")
                .define("enableBetterAimCamera", true);
        FAST_SWAP_GUN = BUILDER
                .comment("Allow fast gun swapping without swap delay.")
                .define("enableFastSwapGun", true);
        SHOW_ITEM_RELATION = BUILDER
                .comment("Highlight related items while hovering inventory items.")
                .define("enableShowItemRelation", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static void updateItemBlacklist() {
        List<? extends String> configuredItems = MELEE_WEAPON_LIST.get();
        if (configuredItems == null) {
            meleeGunIds = Set.of();
            return;
        }

        Set<ResourceLocation> next = new HashSet<>();
        for (String itemId : configuredItems) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id != null) {
                next.add(id);
            }
        }
        meleeGunIds = Set.copyOf(next);
    }

    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
        }
    }

    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            updateItemBlacklist();
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> {
                var packet = com.mafuyu404.taczaddon.network.ConfigSyncPacket.fromServerConfig();
                for (var player : server.getPlayerList().getPlayers()) NetworkHandler.sendToClient(player, packet);
            });
        }
    }

    public static boolean isItemInBlacklist(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        ResourceLocation gunId = ResourceLocation.tryParse(ItemStackData.getCustomDataCopy(itemStack).getString("GunId"));
        return gunId != null && meleeGunIds.contains(gunId);
    }

    public static boolean enableBetterAimCamera() {
        return BETTER_AIM_CAMERA.get();
    }

    public static boolean enableGunSmithTableCraftToast() {
        return GUNSMITHTABLE_CRAFT_TOAST.get();
    }

    public static boolean enableGunSmithTableContainerReader() {
        return GUNSMITHTABLE_CONTAINER_READER.get();
    }

    public static int getAllowGunAmount() {
        return LESS_ALLOW_GUN.get();
    }

    public static boolean enableShootWhileReloading() { return SHOOT_WHILE_RELOADING.get(); }
    public static int getContainerScanRadius() { return CONTAINER_SCAN_RADIUS.get(); }
    public static int getBatchCraftMax() { return Math.max(1, Math.min(GUNSMITHTABLE_MASS_CRAFT_TIME.get(), 64)); }
    public static boolean enableNearbyContainerSources() { return GUNSMITHTABLE_CONTAINER_READER.get(); }
    public static boolean showItemRelationInSophisticatedStorage() {
        return SHOW_ITEM_RELATION.get() && SHOW_ITEM_RELATION_IN_SOPHISTICATED_STORAGE.get();
    }

    public static int getMassCraftTime() {
        return getBatchCraftMax();
    }
}
