package com.mafuyu404.taczaddon.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class SophisticatedBackpacksCompatInner {
    private static final Logger LOGGER = LogUtils.getLogger();

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
                    InventoryHandler handler = getFreshInventoryHandler(
                            player,
                            wrapper
                    );
                    if (visitor.test(handler)) {
                        stopped[0] = true;
                        return true;
                    }
                    return false;
                }
        );
        return stopped[0];
    }

    private static InventoryHandler getFreshInventoryHandler(
            Player player,
            IBackpackWrapper wrapper
    ) {
        InventoryHandler handler = wrapper.getInventoryHandler();
        if (player == null || !player.level().isClientSide) {
            return handler;
        }

        Optional<UUID> contentsUuid = wrapper.getContentsUuid();
        if (contentsUuid.isEmpty()) {
            return handler;
        }

        UUID uuid = contentsUuid.get();
        CompoundTag contents =
                BackpackStorage.get()
                        .getOrCreateBackpackContents(uuid);
        if (!hasSynchronizedInventoryTag(contents)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "[TACZ-addon/SophisticatedBackpacks] "
                                + "clientInventoryRefresh=false "
                                + "reason=inventory_tag_missing uuid={}",
                        uuid
                );
            }
            return handler;
        }

        CompoundTag synchronizedInventory =
                contents.getCompound(
                        InventoryHandler.INVENTORY_TAG
                );
        CompoundTag cachedInventory = handler.serializeNBT();
        if (!needsClientInventoryRefresh(
                synchronizedInventory,
                cachedInventory
        )) {
            return handler;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "[TACZ-addon/SophisticatedBackpacks] "
                            + "clientInventoryRefresh=true uuid={} "
                            + "cachedHash={} syncedHash={}",
                    uuid,
                    Integer.toHexString(
                            cachedInventory.hashCode()
                    ),
                    Integer.toHexString(
                            synchronizedInventory.hashCode()
                    )
            );
        }

        wrapper.onContentsNbtUpdated();
        return wrapper.getInventoryHandler();
    }

    static boolean hasSynchronizedInventoryTag(
            CompoundTag contents
    ) {
        return contents != null
                && contents.contains(
                        InventoryHandler.INVENTORY_TAG,
                        Tag.TAG_COMPOUND
                );
    }

    static boolean needsClientInventoryRefresh(
            CompoundTag synchronizedInventory,
            CompoundTag cachedInventory
    ) {
        if (Objects.equals(
                synchronizedInventory,
                cachedInventory
        )) {
            return false;
        }
        if (synchronizedInventory == null
                || cachedInventory == null) {
            return true;
        }
        if (synchronizedInventory.getInt("Size")
                != cachedInventory.getInt("Size")) {
            return true;
        }
        return !sameItemsBySlot(
                synchronizedInventory.getList(
                        "Items",
                        Tag.TAG_COMPOUND
                ),
                cachedInventory.getList(
                        "Items",
                        Tag.TAG_COMPOUND
                )
        );
    }

    private static boolean sameItemsBySlot(
            ListTag synchronizedItems,
            ListTag cachedItems
    ) {
        if (synchronizedItems.size() != cachedItems.size()) {
            return false;
        }

        Map<Integer, CompoundTag> bySlot =
                new HashMap<>();
        for (Tag tag : synchronizedItems) {
            CompoundTag item = (CompoundTag) tag;
            bySlot.put(item.getInt("Slot"), item);
        }

        for (Tag tag : cachedItems) {
            CompoundTag item = (CompoundTag) tag;
            CompoundTag synchronizedItem =
                    bySlot.get(item.getInt("Slot"));
            if (synchronizedItem == null
                    || !synchronizedItem.equals(item)) {
                return false;
            }
        }
        return true;
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
