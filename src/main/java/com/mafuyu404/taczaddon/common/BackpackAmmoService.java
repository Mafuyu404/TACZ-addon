package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompat;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
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

    public static int consumeCompatibleAmmo(
            ServerPlayer player,
            AbstractGunItem gun,
            ItemStack gunStack,
            int requested,
            @Nullable IItemHandler vanillaHandler
    ) {
        if (player == null
                || gun == null
                || gunStack == null
                || gunStack.isEmpty()
                || requested <= 0) {
            return 0;
        }

        int[] remaining = {requested};
        SophisticatedBackpacksCompat.mutateInventoryBackpacks(
                player,
                handler -> {
                    remaining[0] -= extractFromHandler(
                            gun,
                            gunStack,
                            remaining[0],
                            handler
                    );
                    return remaining[0] <= 0;
                }
        );

        if (remaining[0] > 0) {
            IItemHandler handler = vanillaHandler;
            if (handler == null) {
                handler = player.getCapability(
                                ForgeCapabilities.ITEM_HANDLER,
                                null
                        )
                        .orElse(null);
            }
            if (handler != null) {
                remaining[0] -= extractFromHandler(
                        gun,
                        gunStack,
                        remaining[0],
                        handler
                );
            }
        }

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

    static int consumeCompatibleAmmoFromHandlers(
            int requested,
            Iterable<? extends IItemHandler> backpackHandlers,
            AmmoExtractor extractor,
            @Nullable IItemHandler vanillaHandler
    ) {
        int remaining = requested;
        for (IItemHandler handler : backpackHandlers) {
            if (remaining <= 0) {
                break;
            }
            remaining -= usedAmount(
                    remaining,
                    extractor.extract(handler, remaining)
            );
        }
        if (remaining > 0 && vanillaHandler != null) {
            remaining -= usedAmount(
                    remaining,
                    extractor.extract(vanillaHandler, remaining)
            );
        }
        return requested - remaining;
    }

    @FunctionalInterface
    interface AmmoExtractor {
        int extract(IItemHandler handler, int remaining);
    }

    static int extractFromHandler(
            AbstractGunItem gun,
            ItemStack gunStack,
            int remaining,
            IItemHandler handler
    ) {
        if (remaining <= 0 || handler == null) {
            return 0;
        }
        int extracted = Math.max(
                0,
                gun.findAndExtractInventoryAmmo(
                        handler,
                        gunStack,
                        remaining
                )
        );
        return usedAmount(remaining, extracted);
    }

    static int usedAmount(int remaining, int extracted) {
        return Math.max(0, Math.min(remaining, extracted));
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
