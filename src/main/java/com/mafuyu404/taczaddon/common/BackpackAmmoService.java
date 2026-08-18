package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public final class BackpackAmmoService {
    private BackpackAmmoService() {
    }

    public static boolean hasCompatibleAmmo(
            Player player,
            ItemStack gunStack,
            @Nullable IItemHandler vanillaHandler
    ) {
        if (player == null || gunStack == null || gunStack.isEmpty()) {
            return false;
        }

        boolean foundInBackpack =
                SophisticatedBackpacksCompat.visitInventoryBackpacks(
                        player,
                        handler -> containsCompatibleAmmo(
                                handler,
                                gunStack
                        )
                );
        if (foundInBackpack) {
            return true;
        }

        IItemHandler handler = vanillaHandler;
        if (handler == null) {
            handler = player.getCapability(
                            ForgeCapabilities.ITEM_HANDLER,
                            null
                    )
                    .orElse(null);
        }
        return handler != null
                && containsCompatibleAmmo(handler, gunStack);
    }

    public static int consumeBackpackAmmoRaw(
            ServerPlayer player,
            ItemStack gunStack,
            int requested
    ) {
        if (!SophisticatedBackpacksCompat.isInstalled()
                || player == null
                || gunStack == null
                || gunStack.isEmpty()
                || requested <= 0) {
            return 0;
        }

        int[] remaining = {requested};
        SophisticatedBackpacksCompat.mutateInventoryBackpacks(
                player,
                handler -> {
                    if (remaining[0] <= 0) {
                        return true;
                    }

                    int consumed = extractCompatibleAmmoDirectly(
                            handler,
                            gunStack,
                            remaining[0]
                    );
                    remaining[0] -= clampConsumed(
                            remaining[0],
                            consumed
                    );
                    return remaining[0] <= 0;
                }
        );

        return requested - remaining[0];
    }

    public static IItemHandler createQueryHandler(
            Player player,
            @Nullable IItemHandler vanillaHandler
    ) {
        ArrayList<ItemStack> allItems = new ArrayList<>();
        SophisticatedBackpacksCompat.visitInventoryBackpacks(
                player,
                handler -> {
                    for (int index = 0;
                         index < handler.getSlots();
                         index++) {
                        ItemStack stack =
                                handler.getStackInSlot(index);
                        if (!stack.isEmpty()) {
                            allItems.add(stack.copy());
                        }
                    }
                    return false;
                }
        );

        IItemHandler handler = vanillaHandler;
        if (handler == null && player != null) {
            handler = player.getCapability(
                            ForgeCapabilities.ITEM_HANDLER,
                            null
                    )
                    .orElse(null);
        }
        if (handler != null) {
            for (int index = 0;
                 index < handler.getSlots();
                 index++) {
                allItems.add(handler.getStackInSlot(index));
            }
        }

        VirtualInventory inventory = new VirtualInventory(
                allItems.size(),
                player
        );
        for (int index = 0; index < allItems.size(); index++) {
            inventory.setItem(index, allItems.get(index));
        }
        return inventory.getHandler();
    }

    static boolean containsCompatibleAmmoInHandlers(
            Iterable<? extends IItemHandler> backpackHandlers,
            Predicate<ItemStack> matcher
    ) {
        for (IItemHandler handler : backpackHandlers) {
            for (int slot = 0;
                 slot < handler.getSlots();
                 slot++) {
                if (matcher.test(handler.getStackInSlot(slot))) {
                    return true;
                }
            }
        }
        return false;
    }

    static int extractCompatibleAmmoDirectly(
            IItemHandler handler,
            ItemStack gunStack,
            int requested
    ) {
        if (requested <= 0 || handler == null
                || gunStack == null || gunStack.isEmpty()) {
            return 0;
        }

        int remaining = requested;
        for (int slot = 0;
             slot < handler.getSlots() && remaining > 0;
             slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (item instanceof IAmmo ammo
                    && ammo.isAmmoOfGun(gunStack, stack)) {
                ItemStack extracted = handler.extractItem(
                        slot,
                        remaining,
                        false
                );
                remaining -= clampConsumed(
                        remaining,
                        Math.max(0, extracted.getCount())
                );
                continue;
            }

            if (item instanceof IAmmoBox ammoBox
                    && ammoBox.isAmmoBoxOfGun(gunStack, stack)) {
                int boxAmmoCount = Math.max(
                        0,
                        ammoBox.getAmmoCount(stack)
                );
                int extracted = Math.min(
                        boxAmmoCount,
                        remaining
                );
                if (extracted > 0) {
                    int newCount = boxAmmoCount - extracted;
                    ammoBox.setAmmoCount(stack, newCount);
                    if (newCount <= 0) {
                        ammoBox.setAmmoId(
                                stack,
                                DefaultAssets.EMPTY_AMMO_ID
                        );
                    }
                    remaining -= extracted;
                }
            }
        }
        return requested - remaining;
    }

    static int clampConsumed(int requested, int consumed) {
        if (requested <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(requested, consumed));
    }

    private static boolean containsCompatibleAmmo(
            IItemHandler handler,
            ItemStack gunStack
    ) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (isCompatibleAmmo(
                    gunStack,
                    handler.getStackInSlot(slot)
            )) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCompatibleAmmo(
            ItemStack gunStack,
            ItemStack candidate
    ) {
        if (candidate.isEmpty()) {
            return false;
        }

        Item item = candidate.getItem();
        if (item instanceof IAmmo ammo) {
            return ammo.isAmmoOfGun(gunStack, candidate);
        }
        if (item instanceof IAmmoBox ammoBox) {
            return ammoBox.isAmmoBoxOfGun(gunStack, candidate);
        }
        return false;
    }
}
