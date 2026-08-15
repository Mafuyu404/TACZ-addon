package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Optional Sophisticated Backpacks facade.
 *
 * The outer class contains no Sophisticated API types. If the installed
 * Backpacks version changes incompatibly, the backend is disabled for the
 * rest of the session instead of crashing the client.
 */
public final class SophisticatedBackpacksCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "sophisticatedbackpacks";

    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED =
            new AtomicBoolean();

    private SophisticatedBackpacksCompat() {
    }

    public static boolean isInstalled() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    public static boolean visitInventoryBackpacks(
            Player player,
            Predicate<IItemHandler> visitor
    ) {
        if (!isUsable() || player == null) {
            return false;
        }
        try {
            return SophisticatedBackpacksCompatInner
                    .visitInventoryBackpacks(player, visitor);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static boolean mutateInventoryBackpacks(
            ServerPlayer player,
            Predicate<IItemHandler> visitor
    ) {
        if (!isUsable() || player == null) {
            return false;
        }
        try {
            return SophisticatedBackpacksCompatInner
                    .mutateInventoryBackpacks(player, visitor);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
            return false;
        }
    }

    public static void syncAllBackpack(Player player) {
        if (!isUsable() || player == null) {
            return;
        }
        try {
            SophisticatedBackpacksCompatInner.syncAllBackpack(player);
        } catch (LinkageError linkageError) {
            breakLinkage(linkageError);
        }
    }

    private static boolean isUsable() {
        return isInstalled() && !linkageBroken;
    }

    private static void breakLinkage(LinkageError linkageError) {
        linkageBroken = true;
        if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[TACZ-addon] Sophisticated Backpacks API is unavailable; "
                            + "backpack integration disabled for this session",
                    linkageError
            );
        }
    }
}
