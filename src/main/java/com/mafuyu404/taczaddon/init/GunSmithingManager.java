package com.mafuyu404.taczaddon.init;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Binary compatibility facade for integrations compiled against TACZ-addon
 * 1.1.6. The removed CombinedItems cache is intentionally not restored.
 */
@Deprecated(forRemoval = false)
public final class GunSmithingManager {
    private GunSmithingManager() {
    }

    /**
     * Current non-liberated refit semantics always use the real inventory, so
     * no legacy cached attachment IDs are exposed.
     */
    @Deprecated(forRemoval = false)
    public static List<String> getResult(ItemStack itemStack) {
        return List.of();
    }
}
