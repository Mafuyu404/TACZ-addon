package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.compat.CuriosCompat;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public final class CuriosAmmoService {
    private CuriosAmmoService() {}

    public static int consumeAmmo(ServerPlayer player, ItemStack gun, int requested) {
        if (player == null || gun == null || gun.isEmpty() || requested <= 0) return 0;
        int[] consumed = {0};
        CuriosCompat.visitHandlers(player, handler -> {
            consumeHandler(handler, gun, requested, consumed);
            return consumed[0] >= requested;
        });
        // Keep already committed slot consumption even if a later handler loses linkage.
        return consumed[0];
    }

    static int consumeHandler(IItemHandler handler, ItemStack gun, int requested) {
        int[] consumed = {0};
        consumeHandler(handler, gun, requested, consumed);
        return consumed[0];
    }

    private static void consumeHandler(IItemHandler handler, ItemStack gun, int requested, int[] consumed) {
        if (handler == null || gun == null || gun.isEmpty()) return;
        for (int slot = 0; slot < handler.getSlots() && consumed[0] < requested; slot++) {
            ItemStack snapshot = handler.getStackInSlot(slot).copy();
            if (snapshot.isEmpty()) continue;
            int remaining = requested - consumed[0];
            if (snapshot.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gun, snapshot)) {
                ItemStack extracted = handler.extractItem(slot, remaining, false);
                consumed[0] += BackpackAmmoService.clampConsumed(remaining, extracted.getCount());
            } else if (snapshot.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gun, snapshot)
                    && handler instanceof IItemHandlerModifiable modifiable) {
                int count = Math.max(0, box.getAmmoCount(snapshot));
                int amount = Math.min(count, remaining);
                if (amount == 0) continue;
                box.setAmmoCount(snapshot, count - amount);
                if (count == amount) box.setAmmoId(snapshot, DefaultAssets.EMPTY_AMMO_ID);
                modifiable.setStackInSlot(slot, snapshot);
                consumed[0] += amount;
            }
            // Generic handlers cannot guarantee a safe ammo-box writeback; leave that slot untouched.
        }
    }
}
