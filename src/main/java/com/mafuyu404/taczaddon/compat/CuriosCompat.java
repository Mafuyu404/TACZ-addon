package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Optional Forge Curios boundary.
 *
 * Read-only queries may safely degrade to "not available".
 * Mutation calls must propagate linkage failure because an external handler
 * may already have changed state before throwing.
 */
public final class CuriosCompat {
    private static final String MOD_ID = "curios";

    private static volatile boolean linkageBroken;

    private static final AtomicBoolean LINKAGE_WARNING_LOGGED =
            new AtomicBoolean();

    private CuriosCompat() {
    }

    public static boolean isInstalled() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    /**
     * Read-only traversal.
     *
     * Linkage failure is equivalent to this integration being unavailable;
     * no inventory mutation needs to be accounted for.
     */
    public static boolean visitHandlers(
            Player player,
            Predicate<IItemHandler> visitor
    ) {
        if (player == null || !isInstalled() || linkageBroken) {
            return false;
        }

        return runGuarded(
                () -> CuriosCompatInner.visitHandlers(
                        player,
                        visitor
                )
        );
    }

    /**
     * Mutation traversal.
     *
     * Never convert a linkage failure into normal false. The visitor may
     * already have committed part of an extraction, so the current ammo
     * request must stop instead of falling through to another source.
     */
    public static boolean mutateHandlers(
            Player player,
            Predicate<IItemHandler> visitor
    ) {
        if (player == null || !isInstalled() || linkageBroken) {
            return false;
        }

        try {
            return CuriosCompatInner.visitHandlers(
                    player,
                    visitor
            );
        } catch (LinkageError error) {
            breakLinkage(error);
            throw error;
        }
    }

    static boolean runGuarded(BooleanSupplier operation) {
        if (linkageBroken) {
            return false;
        }

        try {
            return operation.getAsBoolean();
        } catch (LinkageError error) {
            breakLinkage(error);
            return false;
        }
    }

    private static void breakLinkage(LinkageError error) {
        linkageBroken = true;

        if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
            LogUtils.getLogger().warn(
                    "[TACZ-addon] Curios API unavailable; "
                            + "Curios ammo disabled for this session",
                    error
            );
        }
    }
}