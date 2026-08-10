package com.mafuyu404.taczaddon.compat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Consumer;

public final class CuriosCompat {
    private static final String MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void forEachCuriosHandler(
            Player player,
            Consumer<IItemHandler> action
    ) {
        if (!isInstalled() || player == null) {
            return;
        }

        CuriosCompatInner.forEachCuriosHandler(player, action);
    }
}
