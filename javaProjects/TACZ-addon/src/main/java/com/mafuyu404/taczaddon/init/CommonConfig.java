package com.mafuyu404.taczaddon.init;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final ForgeConfigSpec SPEC;

    // Phase 1: shoot-after-reload-cancel
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHOOT_AFTER_RELOAD_CANCEL;

    // Phase 4: nearby-container discovery
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONTAINER_READER;
    public static final ForgeConfigSpec.IntValue CONTAINER_SCAN_RADIUS;

    // Phase 10: batch crafting
    public static final ForgeConfigSpec.IntValue BATCH_CRAFT_MAX;

    // Phase 5/6: backpack access
    public static final ForgeConfigSpec.BooleanValue ENABLE_BACKPACK_CRAFTING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BACKPACK_ATTACHMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Shooting");
        ENABLE_SHOOT_AFTER_RELOAD_CANCEL = builder
                .comment("When enabled, pressing fire during a reload will cancel the reload and fire immediately after the cancellation completes.")
                .define("enableShootAfterReloadCancel", false);
        builder.pop();

        builder.push("ContainerReader");
        ENABLE_CONTAINER_READER = builder
                .comment("When enabled, the gunsmith table can read materials from nearby containers.")
                .define("enableContainerReader", true);
        CONTAINER_SCAN_RADIUS = builder
                .comment("Maximum distance (in blocks) from the gunsmith table to scan for containers. Range: 1-16.")
                .defineInRange("containerScanRadius", 3, 1, 16);
        builder.pop();

        builder.push("Crafting");
        BATCH_CRAFT_MAX = builder
                .comment("Maximum number of items that can be crafted in one batch (shift-click). Range: 1-64.")
                .defineInRange("batchCraftMax", 64, 1, 64);
        builder.pop();

        builder.push("Backpack");
        ENABLE_BACKPACK_CRAFTING = builder
                .comment("When enabled, the gunsmith table can use materials stored in Sophisticated Backpacks for crafting.")
                .define("enableBackpackCrafting", true);
        ENABLE_BACKPACK_ATTACHMENT = builder
                .comment("When enabled, attachments can be installed from and unloaded into Sophisticated Backpacks.")
                .define("enableBackpackAttachment", true);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean enableShootAfterReloadCancel() {
        return ENABLE_SHOOT_AFTER_RELOAD_CANCEL.get();
    }

    public static boolean enableContainerReader() {
        return ENABLE_CONTAINER_READER.get();
    }

    public static int getContainerScanRadius() {
        return CONTAINER_SCAN_RADIUS.get();
    }

    public static int getBatchCraftMax() {
        return BATCH_CRAFT_MAX.get();
    }

    public static boolean enableBackpackCrafting() {
        return ENABLE_BACKPACK_CRAFTING.get();
    }

    public static boolean enableBackpackAttachment() {
        return ENABLE_BACKPACK_ATTACHMENT.get();
    }
}
