package com.mafuyu404.taczaddon.compat.sophisticated;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The only normal runtime implementation that statically references
 * Sophisticated Backpacks / Sophisticated Core APIs.
 *
 * <p>This class is intentionally never referenced by class literal or
 * constructor reference from the safe facade or runtime. It is loaded by
 * name only after the optional dependency group has been confirmed present,
 * which keeps optional classes out of the ordinary TACZAddon class-loading
 * path.
 *
 * <p>Volatile {@code PlayerInventoryProvider} calls go through the cached
 * {@link PlayerInventoryProviderBridge}; stable typed APIs are used directly
 * and remain protected by the per-capability {@link SophisticatedRuntime}
 * linkage guard.
 *
 * <p>Carried backpacks are always resolved through
 * {@code BackpackContext.Item} built from the provider location
 * (inventory handler name, identifier, slot). The exact stack/location
 * supplied by {@code PlayerInventoryProvider} is authoritative; defensive
 * stack copies are never used as wrapper identity.
 *
 * <p>Read traversal self-heals stale client-side wrapper handlers by
 * comparing the handler's serialized inventory against the synchronized
 * {@code BackpackStorage} NBT. Server mutation snapshots every slot, detects
 * both {@code extractItem} and in-place ammo-box modifications, persists the
 * changed handler and immediately pushes the authoritative contents payload
 * to the client.
 */
public final class SophisticatedBackpacksIntegrationImpl
        implements SophisticatedBackpacksIntegration {

    private static final int MAX_AMMO_COUNT = 9999;

    private PlayerInventoryProviderBridge playerBridge;

    @Override
    public boolean probeCarriedBackpack() {
        bridge();
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack."
                        + "BackpackItem"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                        + "BackpackWrapper"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                        + "IBackpackWrapper"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.common.gui."
                        + "BackpackContext$Item"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack."
                        + "BackpackStorage"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedcore.inventory."
                        + "InventoryHandler"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedcore.upgrades."
                        + "UpgradeHandler"
        );
        return true;
    }

    @Override
    public boolean probeBlockBackpack() {
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.common.gui."
                        + "BackpackContext$Block"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                        + "IBackpackWrapper"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                        + "IBackpackWrapper$Noop"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack."
                        + "BackpackBlockEntity"
        );
        return true;
    }

    @Override
    public boolean probeClientSync() {
        bridge();
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.network."
                        + "RequestBackpackInventoryContentsPayload"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.network."
                        + "BackpackContentsPayload"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper."
                        + "BackpackWrapper"
        );
        checkClass(
                "net.p3pp3rf1y.sophisticatedcore.init."
                        + "ModCoreDataComponents"
        );
        return true;
    }

    @Override
    public List<ItemStack> getItemsFromBackpackBlock(
            BlockPos blockPos,
            Player player
    ) {
        List<ItemStack> items = new ArrayList<>();

        BackpackContext.Block context =
                new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper =
                context.getBackpackWrapper(player);

        if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
            return items;
        }

        addHandlerItems(items, wrapper.getInventoryHandler());
        return items;
    }

    @Override
    public int countInventoryBackpackAmmo(
            Player player,
            ItemStack gunStack
    ) {
        if (player == null || gunStack.isEmpty()) {
            return 0;
        }

        int[] total = {0};

        visitInventoryBackpacks(
                player,
                handler -> {
                    for (int slot = 0;
                         slot < handler.getSlots();
                         slot++) {
                        ItemStack candidate =
                                handler.getStackInSlot(slot);

                        if (candidate.isEmpty()) {
                            continue;
                        }

                        if (candidate.getItem() instanceof IAmmo ammo) {
                            if (ammo.isAmmoOfGun(gunStack, candidate)) {
                                total[0] = addAmmoSafely(
                                        total[0],
                                        candidate.getCount()
                                );
                            }
                        }

                        if (candidate.getItem() instanceof IAmmoBox ammoBox) {
                            if (!ammoBox.isAmmoBoxOfGun(
                                    gunStack,
                                    candidate
                            )) {
                                continue;
                            }

                            if (ammoBox.isAllTypeCreative(candidate)
                                    || ammoBox.isCreative(candidate)) {
                                total[0] = MAX_AMMO_COUNT;
                                return true;
                            }

                            total[0] = addAmmoSafely(
                                    total[0],
                                    ammoBox.getAmmoCount(candidate)
                            );
                        }

                        if (total[0] >= MAX_AMMO_COUNT) {
                            return true;
                        }
                    }

                    return false;
                }
        );

        return total[0];
    }

    private static int addAmmoSafely(int current, int amount) {
        if (amount <= 0) {
            return current;
        }

        long result = (long) current + amount;

        return result >= MAX_AMMO_COUNT
                ? MAX_AMMO_COUNT
                : (int) result;
    }

    @Override
    public boolean visitInventoryBackpacks(
            Player player,
            Predicate<IItemHandler> visitor
    ) {
        if (player == null || visitor == null) {
            return false;
        }

        boolean[] stopped = {false};

        bridge().forEachBackpack(
                player,
                (backpack, inventoryHandlerName, identifier, slot) -> {
                    BackpackContext.Item context =
                            new BackpackContext.Item(
                                    inventoryHandlerName,
                                    identifier,
                                    slot
                            );

                    IBackpackWrapper wrapper =
                            context.getBackpackWrapper(player);

                    if (wrapper == IBackpackWrapper.Noop.INSTANCE) {
                        return false;
                    }

                    IItemHandler handler =
                            getFreshInventoryHandler(
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

    @Override
    public boolean mutateInventoryBackpacks(
            ServerPlayer player,
            Predicate<IItemHandler> visitor
    ) {
        if (player == null || visitor == null) {
            return false;
        }

        boolean[] stopped = {false};

        bridge().forEachBackpack(
                player,
                (backpack, inventoryHandlerName, identifier, slot) -> {
                    BackpackContext.Item context =
                            new BackpackContext.Item(
                                    inventoryHandlerName,
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

                    IItemHandler handler =
                            wrapper.getInventoryHandler();

                    boolean stop =
                            mutateBackpackHandler(
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

    /**
     * Runs a server-side mutation visitor against one backpack handler and,
     * when the handler actually changed, persists the mutation and pushes the
     * authoritative contents to the client.
     *
     * <p>Both ordinary {@code extractItem} mutations and TaCZ
     * {@code IAmmoBox} in-place modifications on the existing stack are
     * detected by comparing every slot against the pre-visitor snapshot.
     */
    private boolean mutateBackpackHandler(
            ServerPlayer player,
            IBackpackWrapper wrapper,
            IItemHandler handler,
            Predicate<IItemHandler> visitor
    ) {
        List<ItemStack> before =
                snapshotHandler(handler);

        boolean stop =
                visitor.test(handler);

        boolean changed =
                writeBackChangedStacks(
                        before,
                        handler
                );

        if (shouldPersistAndSynchronize(changed)) {
            if (handler instanceof InventoryHandler inventoryHandler) {
                inventoryHandler.saveInventory();
            }

            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();

            syncBackpackContents(
                    player,
                    wrapper
            );
        }

        return stop;
    }

    /**
     * Persistence and client synchronization are only requested when the
     * mutation snapshot actually detected a handler change. An untouched
     * backpack never triggers {@code saveInventory} or a contents payload.
     */
    static boolean shouldPersistAndSynchronize(
            boolean changed
    ) {
        return changed;
    }

    /**
     * Copies every slot of a handler for pre-mutation change detection.
     */
    static List<ItemStack> snapshotHandler(
            IItemHandler handler
    ) {
        List<ItemStack> before =
                new ArrayList<>(handler.getSlots());

        for (int slot = 0;
             slot < handler.getSlots();
             slot++) {
            before.add(
                    handler.getStackInSlot(slot).copy()
            );
        }

        return before;
    }

    /**
     * Sends a {@code BackpackContentsPayload} containing the current
     * authoritative inventory and upgrade inventory NBT for one backpack.
     *
     * <p>This mirrors the response payload constructed by Sophisticated's own
     * {@code RequestBackpackInventoryContentsPayload} server handler.
     */
    private void syncBackpackContents(
            ServerPlayer player,
            IBackpackWrapper wrapper
    ) {
        wrapper.getContentsUuid().ifPresent(uuid -> {
            CompoundTag backpackContents =
                    BackpackStorage.get()
                            .getOrCreateBackpackContents(uuid);

            CompoundTag synchronizedContents =
                    new CompoundTag();

            Tag inventoryNbt =
                    backpackContents.get(
                            InventoryHandler.INVENTORY_TAG
                    );

            if (inventoryNbt != null) {
                synchronizedContents.put(
                        InventoryHandler.INVENTORY_TAG,
                        inventoryNbt.copy()
                );
            }

            Tag upgradeNbt =
                    backpackContents.get(
                            UpgradeHandler.UPGRADE_INVENTORY_TAG
                    );

            if (upgradeNbt != null) {
                synchronizedContents.put(
                        UpgradeHandler.UPGRADE_INVENTORY_TAG,
                        upgradeNbt.copy()
                );
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new BackpackContentsPayload(
                            uuid,
                            synchronizedContents
                    )
            );
        });
    }

    @Override
    public void modifyBlockBackpack(
            ServerPlayer player,
            BlockPos blockPos,
            Consumer<IItemHandler> action
    ) {
        forEachBlockBackpackHandler(player, blockPos, action);
    }

    @Override
    public void forEachBlockBackpackHandler(
            Player player,
            BlockPos blockPos,
            Consumer<IItemHandler> action
    ) {
        BackpackContext.Block context =
                new BackpackContext.Block(blockPos);
        IBackpackWrapper wrapper =
                context.getBackpackWrapper(player);

        if (wrapper != IBackpackWrapper.Noop.INSTANCE) {
            action.accept(wrapper.getInventoryHandler());
        }
    }

    @Override
    public List<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();

        if (itemStack.isEmpty()
                || !(itemStack.getItem() instanceof BackpackItem)) {
            return items;
        }

        /*
         * This method expects the actual backpack stack, not a defensive
         * copy. fromExistingData uses the ItemStack-keyed
         * StorageWrapperRepository and may return empty for copied stacks.
         */
        BackpackWrapper.fromExistingData(itemStack)
                .ifPresent(wrapper ->
                        addHandlerItems(items, wrapper.getInventoryHandler())
                );

        return items;
    }

    @Override
    public List<ItemStack> getItemsFromInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();

        visitInventoryBackpacks(
                player,
                handler -> {
                    addHandlerItems(items, handler);
                    return false;
                }
        );

        return items;
    }

    /**
     * Requests authoritative inventory NBT for every backpack currently
     * accessible through Sophisticated Backpacks' player inventory
     * providers.
     *
     * <p>Client side only.
     */
    @Override
    public void syncAllBackpack(Player player) {
        if (!player.level().isClientSide()) {
            return;
        }

        Set<UUID> requestedUuids = new HashSet<>();

        bridge().forEachBackpack(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    UUID uuid = backpack.get(
                            ModCoreDataComponents.STORAGE_UUID.get()
                    );

                    if (uuid == null || !requestedUuids.add(uuid)) {
                        return false;
                    }

                    /*
                     * Register a wrapper for this exact ItemStack instance.
                     *
                     * Do not initialize its InventoryHandler here because the
                     * client BackpackStorage may not contain the synchronized
                     * NBT yet.
                     */
                    BackpackWrapper.fromStack(backpack);

                    PacketDistributor.sendToServer(
                            new RequestBackpackInventoryContentsPayload(uuid)
                    );

                    return false;
                }
        );
    }

    /**
     * Called after BackpackContentsPayload has written the received NBT into
     * the client-side BackpackStorage.
     *
     * <p>Returns true when a carried backpack with the UUID was found.
     */
    @Override
    public boolean refreshInventoryBackpackWrapper(
            Player player,
            UUID updatedUuid
    ) {
        if (updatedUuid == null) {
            return false;
        }

        boolean[] refreshed = {false};

        bridge().forEachBackpack(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    UUID backpackUuid = backpack.get(
                            ModCoreDataComponents.STORAGE_UUID.get()
                    );

                    if (!updatedUuid.equals(backpackUuid)) {
                        return false;
                    }

                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    if (wrapper instanceof BackpackWrapper backpackWrapper) {
                        /*
                         * Clears the cached InventoryHandler and
                         * UpgradeHandler. Their next access reloads data from
                         * BackpackStorage.
                         */
                        backpackWrapper.onContentsNbtUpdated();
                        refreshed[0] = true;
                    }

                    return true;
                }
        );

        return refreshed[0];
    }

    @Override
    public void modifyInventoryBackpack(
            ServerPlayer player,
            ItemStack backpackItem,
            Consumer<IItemHandler> action
    ) {
        bridge().forEachBackpack(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    if (!ItemStack.isSameItemSameComponents(
                            backpack,
                            backpackItem
                    )) {
                        return false;
                    }

                    IBackpackWrapper wrapper =
                            BackpackWrapper.fromStack(backpack);

                    action.accept(wrapper.getInventoryHandler());
                    return false;
                }
        );
    }

    @Override
    public void forEachInventoryBackpackHandler(
            Player player,
            Consumer<IItemHandler> action
    ) {
        if (player == null || action == null) {
            return;
        }

        visitInventoryBackpacks(
                player,
                handler -> {
                    action.accept(handler);
                    return false;
                }
        );
    }

    /**
     * Returns defensive copies for callers that only need backpack item
     * snapshots. Do not use these copies for StorageWrapperRepository lookups.
     */
    @Override
    public List<ItemStack> getAllInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();

        bridge().forEachBackpack(
                player,
                (backpack, inventoryName, identifier, index) -> {
                    items.add(backpack.copy());
                    return false;
                }
        );

        return items;
    }

    @Override
    public boolean isBackpackBlock(
            Level level,
            BlockPos blockPos
    ) {
        return level.isLoaded(blockPos)
                && level.getBlockEntity(blockPos)
                instanceof BackpackBlockEntity;
    }

    /**
     * Client-only self-healing read.
     *
     * <p>Sophisticated's {@code BackpackContentsPayload} updates
     * {@code BackpackStorage}, but an already-created {@code BackpackWrapper}
     * can still retain an old cached {@code InventoryHandler}. When the
     * synchronized storage NBT differs from the handler's current contents,
     * the cached handler is discarded so the next access rebuilds it from the
     * authoritative client storage.
     *
     * <p>Never called from the server mutation path.
     */
    private IItemHandler getFreshInventoryHandler(
            Player player,
            IBackpackWrapper wrapper
    ) {
        InventoryHandler handler =
                wrapper.getInventoryHandler();

        if (player == null
                || !player.level().isClientSide()) {
            return handler;
        }

        Optional<UUID> contentsUuid =
                wrapper.getContentsUuid();

        if (contentsUuid.isEmpty()) {
            return handler;
        }

        CompoundTag contents =
                BackpackStorage.get()
                        .getOrCreateBackpackContents(
                                contentsUuid.get()
                        );

        if (!hasSynchronizedInventoryTag(contents)) {
            return handler;
        }

        CompoundTag synchronizedInventory =
                contents.getCompound(
                        InventoryHandler.INVENTORY_TAG
                );

        CompoundTag cachedInventory =
                handler.serializeNBT(
                        player.level().registryAccess()
                );

        if (!needsClientInventoryRefresh(
                synchronizedInventory,
                cachedInventory
        )) {
            return handler;
        }

        wrapper.onContentsNbtUpdated();

        return wrapper.getInventoryHandler();
    }

    /**
     * @return true when the contents compound contains the synchronized
     *         inventory tag written by {@code BackpackContentsPayload}
     */
    static boolean hasSynchronizedInventoryTag(
            CompoundTag contents
    ) {
        return contents != null
                && contents.contains(
                        InventoryHandler.INVENTORY_TAG,
                        Tag.TAG_COMPOUND
                );
    }

    /**
     * Compares the synchronized inventory NBT with the cached handler NBT,
     * ignoring harmless item-list ordering changes while still detecting
     * stack count changes, item NBT/component changes, slot content changes
     * and inventory size changes.
     */
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
        if (synchronizedItems.size()
                != cachedItems.size()) {
            return false;
        }

        Map<Integer, CompoundTag> synchronizedBySlot =
                new HashMap<>();

        for (Tag rawTag : synchronizedItems) {
            if (!(rawTag instanceof CompoundTag item)) {
                return false;
            }

            synchronizedBySlot.put(
                    item.getInt("Slot"),
                    item
            );
        }

        for (Tag rawTag : cachedItems) {
            if (!(rawTag instanceof CompoundTag item)) {
                return false;
            }

            CompoundTag synchronizedItem =
                    synchronizedBySlot.get(
                            item.getInt("Slot")
                    );

            if (synchronizedItem == null
                    || !synchronizedItem.equals(item)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Writes any slot whose current stack differs from the pre-visitor
     * snapshot back through the handler when it supports direct slot
     * replacement.
     *
     * <p>This forces Sophisticated's handler to observe TaCZ
     * {@code IAmmoBox} modifications that mutated the existing
     * {@code ItemStack} in place.
     *
     * @return true when at least one slot changed
     */
    static boolean writeBackChangedStacks(
            List<ItemStack> before,
            IItemHandler handler
    ) {
        boolean changed = false;

        for (int slot = 0;
             slot < handler.getSlots();
             slot++) {

            ItemStack current =
                    handler.getStackInSlot(slot);

            if (ItemStack.matches(
                    before.get(slot),
                    current
            )) {
                continue;
            }

            changed = true;

            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(
                        slot,
                        current.copy()
                );
            }
        }

        return changed;
    }

    private PlayerInventoryProviderBridge bridge() {
        if (playerBridge == null) {
            playerBridge = PlayerInventoryProviderBridge.createDefault();
        }
        return playerBridge;
    }

    private static void checkClass(String className) {
        try {
            Class.forName(
                    className,
                    false,
                    SophisticatedBackpacksIntegrationImpl.class
                            .getClassLoader()
            );
        } catch (ReflectiveOperationException
                 | LinkageError exception) {
            throw new SophisticatedCompatibilityException(
                    "Sophisticated class contract missing: "
                            + className,
                    exception
            );
        }
    }

    private static void addHandlerItems(
            List<ItemStack> items,
            IItemHandler handler
    ) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);

            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
    }
}
