package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Optional boundary: no JEI types are resolved until an installed backend is used. */
@OnlyIn(Dist.CLIENT)
public final class JeiCompat {
    private static final String MOD_ID = "jei";
    private static boolean installed;
    private static volatile boolean linkageBroken;
    private static final AtomicBoolean LINKAGE_WARNING_LOGGED = new AtomicBoolean();

    private JeiCompat() {
    }

    public static void init() {
        ModList mods = ModList.get();
        installed = mods != null && mods.isLoaded(MOD_ID);
    }

    public static boolean showRecipes(ItemStack itemStack) {
        if (!installed || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return runGuarded(() -> JeiPlugin.showRecipes(itemStack));
    }

    static boolean runGuarded(BooleanSupplier operation) {
        if (linkageBroken) {
            return false;
        }
        try {
            return operation.getAsBoolean();
        } catch (LinkageError error) {
            linkageBroken = true;
            if (LINKAGE_WARNING_LOGGED.compareAndSet(false, true)) {
                LogUtils.getLogger().warn(
                        "[TACZ-addon] JEI API unavailable; recipe navigation disabled for this session", error);
            }
            return false;
        }
    }
}
