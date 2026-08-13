package com.mafuyu404.taczaddon.compat;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import com.mafuyu404.taczaddon.init.GunSmithingManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class LegacyBinaryFacadeTest {
    @Test
    void exposesLegacyExpectedJvmMethods() throws Exception {
        Method useVirtualInventory = LiberateAttachment.class
                .getDeclaredMethod(
                        "useVirtualInventory",
                        Inventory.class
                );
        Method isLiberated = LiberateAttachment.class
                .getDeclaredMethod("isLiberated", Player.class);
        Method getResult = GunSmithingManager.class
                .getDeclaredMethod("getResult", ItemStack.class);

        assertEquals(Inventory.class, useVirtualInventory.getReturnType());
        assertEquals(boolean.class, isLiberated.getReturnType());
        assertEquals(List.class, getResult.getReturnType());
        assertTrue(Modifier.isStatic(useVirtualInventory.getModifiers()));
        assertTrue(Modifier.isStatic(isLiberated.getModifiers()));
        assertTrue(Modifier.isStatic(getResult.getModifiers()));
        assertTrue(LiberateAttachment.class.isAnnotationPresent(
                Deprecated.class
        ));
        assertTrue(GunSmithingManager.class.isAnnotationPresent(
                Deprecated.class
        ));
    }

    @Test
    void legacyGunSmithingLookupDoesNotRestoreRemovedCache() {
        assertTrue(GunSmithingManager.getResult(null).isEmpty());
    }
}
