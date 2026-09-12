package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Optional Forge Curios boundary; linkage failure never disables another integration. */
public final class CuriosCompat {
    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED = new AtomicBoolean();
    private CuriosCompat() {}

    public static boolean isInstalled() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("curios");
    }

    public static boolean visitHandlers(Player player, Predicate<IItemHandler> visitor) {
        if (player == null || !isInstalled()) return false;
        return runGuarded(() -> CuriosCompatInner.visitHandlers(player, visitor));
    }

    static boolean runGuarded(BooleanSupplier operation) {
        if (linkageBroken) return false;
        try {
            return operation.getAsBoolean();
        } catch (LinkageError error) {
            linkageBroken = true;
            if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
                LogUtils.getLogger().warn("[TACZ-addon] Curios API unavailable; Curios ammo disabled for this session", error);
            }
            return false;
        }
    }
}
