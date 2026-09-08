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
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class SophisticatedBackpacksCompatInner {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LINKED_WRAPPER_CLASS =
            "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                    + "LinkedStorageBackpackWrapper";

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

    private static boolean isLinkedStorageWrapper(
            IBackpackWrapper wrapper
    ) {
        return wrapper != null
                && LINKED_WRAPPER_CLASS.equals(
                wrapper.getClass().getName()
        );
    }

    private static InventoryHandler getFreshInventoryHandler(
            Player player,
            IBackpackWrapper wrapper
    ) {
        InventoryHandler handler =
                wrapper.getInventoryHandler();

        SophisticatedLinkedStorageCompat.EndpointResolution linked =
                SophisticatedLinkedStorageCompat.resolve(
                        wrapper.getBackpack()
                );

        /*
         * Normal linked path.
         */
        if (linked.linked()) {
            /*
             * Linked wrappers delegate to
             * ClientLinkedStorageBackpackContents.
             *
             * Never compare linked contents with ordinary BackpackStorage.
             */
            return handler;
        }

        /*
         * If the optional linked bridge itself became ABI-incompatible, retain the
         * same protection using only the runtime wrapper class name.
         *
         * This lets ordinary wrappers continue through BackpackStorage while
         * preventing a genuine linked wrapper from being incorrectly treated as an
         * ordinary backpack.
         */
        if (linked.bridgeUnavailable()
                && isLinkedStorageWrapper(wrapper)) {
            return handler;
        }

        if (player == null
                || !player.level().isClientSide) {
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

    public static void syncAllBackpack(
            Player player
    ) {
        Set<UUID> requestedStorageUuids =
                new HashSet<>();

        Set<UUID> requestedLinkedGroups =
                new HashSet<>();

        PlayerInventoryProvider.get()
                .runOnBackpacks(
                        player,
                        (
                                backpack,
                                handlerName,
                                identifier,
                                slot
                        ) -> {
                            SophisticatedLinkedStorageCompat
                                    .EndpointResolution linked =
                                    SophisticatedLinkedStorageCompat
                                            .resolve(backpack);

                            if (linked.linked()) {
                                linked.groupIdOptional()
                                        .filter(
                                                requestedLinkedGroups::add
                                        )
                                        .ifPresent(
                                                SophisticatedLinkedStorageCompat
                                                        ::requestSnapshot
                                        );

                                /*
                                 * Includes malformed linked endpoints.
                                 * Never let those fall through to ordinary
                                 * BackpackStorage.
                                 */
                                return false;
                            }

                            BackpackContext.Item context =
                                    new BackpackContext.Item(
                                            handlerName,
                                            identifier,
                                            slot
                                    );

                            IBackpackWrapper wrapper =
                                    context.getBackpackWrapper(
                                            player
                                    );

                            if (wrapper
                                    == IBackpackWrapper.Noop.INSTANCE) {
                                return false;
                            }

                            /*
                             * Runtime linked bridge failed after being detected.
                             *
                             * Preserve ordinary backpack bootstrap, but do not
                             * accidentally route a real linked wrapper through the
                             * ordinary UUID protocol.
                             */
                            if (linked.bridgeUnavailable()
                                    && isLinkedStorageWrapper(
                                    wrapper
                            )) {
                                return false;
                            }

                            wrapper.getContentsUuid()
                                    .filter(
                                            requestedStorageUuids::add
                                    )
                                    .ifPresent(uuid ->
                                            SBPPacketHandler.INSTANCE
                                                    .sendToServer(
                                                            new RequestBackpackInventoryContentsMessage(
                                                                    uuid
                                                            )
                                                    )
                                    );

                            return false;
                        }
                );
    }

    public static void refreshLinkedBackpackSnapshots(
            Player player
    ) {
        Set<UUID> requestedLinkedGroups =
                new HashSet<>();

        PlayerInventoryProvider.get()
                .runOnBackpacks(
                        player,
                        (
                                backpack,
                                handlerName,
                                identifier,
                                slot
                        ) -> {
                            SophisticatedLinkedStorageCompat
                                    .EndpointResolution linked =
                                    SophisticatedLinkedStorageCompat
                                            .resolve(backpack);

                            if (!linked.linked()) {
                                return false;
                            }

                            linked.groupIdOptional()
                                    .filter(
                                            requestedLinkedGroups::add
                                    )
                                    .ifPresent(
                                            SophisticatedLinkedStorageCompat
                                                    ::refreshSnapshot
                                    );

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
        SophisticatedLinkedStorageCompat.EndpointResolution linked =
                SophisticatedLinkedStorageCompat.resolve(
                        wrapper.getBackpack()
                );

        if (linked.linked()) {
            linked.groupIdOptional()
                    .ifPresent(groupId ->
                            SophisticatedLinkedStorageCompat
                                    .sendSnapshot(
                                            player,
                                            groupId
                                    )
                    );

            /*
             * Includes malformed linked endpoints.
             * Never publish them through BackpackContentsMessage.
             */
            return;
        }

        /*
         * A linked bridge ABI failure must not disable ordinary backpacks.
         *
         * But if the wrapper itself is recognizably linked, fail closed instead
         * of sending its canonical host UUID through the ordinary client-storage
         * protocol.
         */
        if (linked.bridgeUnavailable()
                && isLinkedStorageWrapper(wrapper)) {
            return;
        }

        wrapper.getContentsUuid()
                .ifPresent(uuid -> {
                    CompoundTag backpackContent =
                            BackpackStorage.get()
                                    .getOrCreateBackpackContents(
                                            uuid
                                    )
                                    .copy();

                    SBPPacketHandler.INSTANCE
                            .sendToClient(
                                    player,
                                    new BackpackContentsMessage(
                                            uuid,
                                            backpackContent
                                    )
                            );
                });
    }
}
