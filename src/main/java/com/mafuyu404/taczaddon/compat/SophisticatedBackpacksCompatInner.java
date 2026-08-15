package com.mafuyu404.taczaddon.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SophisticatedBackpacksCompatInner {
    private SophisticatedBackpacksCompatInner() {
    }

    public static boolean visitInventoryBackpacks(
            Player player,
            Predicate<IItemHandler> visitor
    ) {
        boolean[] stopped = {false};
        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (ignoredBackpack, handlerName, identifier, slot) -> {
                    BackpackContext.Item context =
                            new BackpackContext.Item(
                                    handlerName,
                                    identifier,
                                    slot
                            );
                    IBackpackWrapper wrapper =
                            context.getBackpackWrapper(player);
                    if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
                        return false;
                    }
                    InventoryHandler handler =
                            wrapper.getInventoryHandler();
                    if (visitor.test(handler)) {
                        stopped[0] = true;
                        return true;
                    }
                    return false;
                }
        );
        return stopped[0];
    }

    public static boolean mutateInventoryBackpacks(
            ServerPlayer player,
            Predicate<IItemHandler> visitor
    ) {
        boolean[] stopped = {false};
        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (ignoredBackpack, handlerName, identifier, slot) -> {
                    BackpackContext.Item context =
                            new BackpackContext.Item(
                                    handlerName,
                                    identifier,
                                    slot
                            );
                    if (!context.canInteractWith(player)) {
                        return false;
                    }
                    IBackpackWrapper wrapper =
                            context.getBackpackWrapper(player);
                    if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
                        return false;
                    }
                    InventoryHandler handler =
                            wrapper.getInventoryHandler();
                    boolean stop = mutateBackpackHandler(
                            player,
                            wrapper,
                            handler,
                            visitor
                    );
                    if (stop) {
                        stopped[0] = true;
                    }
                    return stop;
                }
        );
        return stopped[0];
    }

    public static void syncAllBackpack(Player player) {
        PlayerInventoryProvider.get().runOnBackpacks(
                player,
                (ignoredBackpack, handlerName, identifier, slot) -> {
                    BackpackContext.Item context =
                            new BackpackContext.Item(
                                    handlerName,
                                    identifier,
                                    slot
                            );
                    IBackpackWrapper wrapper =
                            context.getBackpackWrapper(player);
                    if (wrapper != IBackpackWrapper.Noop.INSTANCE) {
                        wrapper.getContentsUuid().ifPresent(uuid ->
                                SBPPacketHandler.INSTANCE.sendToServer(
                                        new RequestBackpackInventoryContentsMessage(
                                                uuid
                                        )
                                )
                        );
                    }
                    return false;
                }
        );
    }

    private static boolean mutateBackpackHandler(
            ServerPlayer player,
            IBackpackWrapper wrapper,
            InventoryHandler inventoryHandler,
            Predicate<IItemHandler> visitor
    ) {
        List<ItemStack> before = new ArrayList<>(
                inventoryHandler.getSlots()
        );
        for (int slot = 0;
             slot < inventoryHandler.getSlots();
             slot++) {
            before.add(
                    inventoryHandler.getStackInSlot(slot).copy()
            );
        }

        boolean stop = visitor.test(inventoryHandler);
        boolean changed = false;

        /*
         * TaCZ mutates ammo-box ItemStacks in place through
         * IAmmoBox#setAmmoCount. Push changed stacks back through the handler
         * so Sophisticated Core refreshes its slot-NBT cache.
         */
        for (int slot = 0;
             slot < inventoryHandler.getSlots();
             slot++) {
            ItemStack current =
                    inventoryHandler.getStackInSlot(slot);
            if (!ItemStack.matches(before.get(slot), current)) {
                inventoryHandler.setStackInSlot(
                        slot,
                        current.copy()
                );
                changed = true;
            }
        }

        if (changed) {
            inventoryHandler.saveInventory();
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            syncBackpackContents(player, wrapper);
        }
        return stop;
    }

    private static void syncBackpackContents(
            ServerPlayer player,
            IBackpackWrapper wrapper
    ) {
        wrapper.getContentsUuid().ifPresent(uuid -> {
            CompoundTag backpackContent =
                    BackpackStorage.get()
                            .getOrCreateBackpackContents(uuid)
                            .copy();
            SBPPacketHandler.INSTANCE.sendToClient(
                    player,
                    new BackpackContentsMessage(
                            uuid,
                            backpackContent
                    )
            );
        });
    }
}
