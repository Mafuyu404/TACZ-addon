package com.mafuyu404.taczaddon.init;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-authoritative gunsmith-table policy and gameplay features.
 *
 * Sophisticated Backpacks crafting is intentionally absent from this phase.
 * It must be introduced later as a real CraftingItemSource implementation,
 * not as a virtual-copy inventory.
 */
public final class CommonConfig {
    public static final int MIN_CONTAINER_SCAN_RADIUS = 1;
    public static final int MAX_CONTAINER_SCAN_RADIUS = 16;
    public static final int MIN_BATCH_CRAFT = 1;
    public static final int MAX_BATCH_CRAFT = 64;

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CONTAINER_READER;
    public static final ForgeConfigSpec.IntValue CONTAINER_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue BATCH_CRAFT_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHOOT_WHILE_RELOADING;

    static {
        ForgeConfigSpec.Builder builder =
                new ForgeConfigSpec.Builder();

        builder.push("GunSmithTable");

        ENABLE_CONTAINER_READER = builder
                .comment(
                        "Allow a gunsmith table to use materials from nearby loaded block inventories."
                )
                .define("enableContainerReader", true);

        CONTAINER_SCAN_RADIUS = builder
                .comment(
                        "Horizontal radius around the authoritative table position.",
                        "Only loaded positions from Y-1 through Y+1 are inspected."
                )
                .defineInRange(
                        "containerScanRadius",
                        3,
                        MIN_CONTAINER_SCAN_RADIUS,
                        MAX_CONTAINER_SCAN_RADIUS
                );

        BATCH_CRAFT_MAX = builder
                .comment(
                        "Maximum number of craft executions accepted from one shift-click request."
                )
                .defineInRange(
                        "batchCraftMax",
                        64,
                        MIN_BATCH_CRAFT,
                        MAX_BATCH_CRAFT
                );

        builder.pop();

        builder.push("Gameplay");

        ENABLE_SHOOT_WHILE_RELOADING = builder
                .comment(
                        "Allow shooting to immediately interrupt a tactical reload",
                        "when the gun still has fireable ammunition."
                )
                .define("enableShootWhileReloading", true);

        builder.pop();

        SPEC = builder.build();
    }

    private CommonConfig() {
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

    public static boolean enableShootWhileReloading() {
        return ENABLE_SHOOT_WHILE_RELOADING.get();
    }
}
