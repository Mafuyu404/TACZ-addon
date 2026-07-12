package com.mafuyu404.taczaddon.init.crafting;

import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mojang.logging.LogUtils;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public final class CraftingTransaction {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServerPlayer player;
    private final GunSmithCraftingSessionManager.GunSmithCraftingSession session;
    private final GunSmithTableRecipe recipe;
    private final List<CraftingItemSource> sources;

    private final LinkedHashMap<SlotRef, MutableSlotPlan> mutablePlans =
            new LinkedHashMap<>();
    private final ArrayList<SlotPlan> slotPlans = new ArrayList<>();
    private final ArrayList<ExtractedEntry> extractedForRollback =
            new ArrayList<>();

    private CraftingTransaction(
            ServerPlayer player,
            GunSmithCraftingSessionManager.GunSmithCraftingSession session,
            GunSmithTableRecipe recipe,
            List<CraftingItemSource> sources
    ) {
        this.player = player;
        this.session = session;
        this.recipe = recipe;
        this.sources = List.copyOf(sources);
    }

    public static CraftResult execute(
            ServerPlayer player,
            GunSmithCraftingSessionManager.GunSmithCraftingSession session,
            GunSmithTableRecipe recipe,
            List<CraftingItemSource> sources
    ) {
        CraftingTransaction transaction =
                new CraftingTransaction(
                        player,
                        session,
                        recipe,
                        sources
                );

        if (player.isCreative()) {
            ItemStack result = transaction.createResultStack();
            if (result.isEmpty() || !transaction.spawnOutput(result)) {
                return CraftResult.fail(CraftFailure.TRANSACTION_FAILED);
            }
            player.inventoryMenu.broadcastFullState();
            return CraftResult.success(result);
        }

        if (!transaction.plan()) {
            return CraftResult.fail(
                    CraftFailure.INSUFFICIENT_MATERIALS
            );
        }

        if (!transaction.simulate()) {
            return CraftResult.fail(
                    CraftFailure.INSUFFICIENT_MATERIALS
            );
        }

        if (!transaction.revalidate()) {
            return CraftResult.fail(CraftFailure.SOURCE_CHANGED);
        }

        if (!transaction.commit()) {
            transaction.rollback();
            return CraftResult.fail(
                    CraftFailure.TRANSACTION_FAILED
            );
        }

        ItemStack result = transaction.createResultStack();
        if (result.isEmpty() || !transaction.spawnOutput(result)) {
            transaction.rollback();
            return CraftResult.fail(
                    CraftFailure.TRANSACTION_FAILED
            );
        }

        transaction.synchronizePlannedSources();
        player.inventoryMenu.broadcastFullState();
        transaction.extractedForRollback.clear();
        return CraftResult.success(result);
    }

    private boolean plan() {
        List<GunSmithTableIngredient> inputs =
                this.recipe.getInputs();
        if (inputs == null) {
            return false;
        }

        for (GunSmithTableIngredient input : inputs) {
            if (input == null) {
                return false;
            }

            int required = input.getCount();
            if (required <= 0) {
                continue;
            }

            Ingredient ingredient = input.getIngredient();
            if (ingredient == null
                    || !allocateIngredient(ingredient, required)) {
                return false;
            }
        }

        for (MutableSlotPlan mutable : this.mutablePlans.values()) {
            this.slotPlans.add(mutable.freeze());
        }
        return true;
    }

    private boolean allocateIngredient(
            Ingredient ingredient,
            int required
    ) {
        int remaining = required;

        for (int sourceIndex = 0;
             sourceIndex < this.sources.size() && remaining > 0;
             sourceIndex++) {
            CraftingItemSource source =
                    this.sources.get(sourceIndex);
            int slotCount = source.slotCount();

            for (int slot = 0;
                 slot < slotCount && remaining > 0;
                 slot++) {
                ItemStack current =
                        source.getStackInSlot(slot);
                if (current.isEmpty()
                        || !ingredient.test(current)) {
                    continue;
                }

                SlotRef ref = new SlotRef(sourceIndex, slot);
                MutableSlotPlan existing =
                        this.mutablePlans.get(ref);
                int alreadyReserved =
                        existing == null ? 0 : existing.amount;
                int available =
                        current.getCount() - alreadyReserved;
                if (available <= 0) {
                    continue;
                }

                if (existing != null
                        && !sameIdentity(
                        current,
                        existing.expectedStack
                )) {
                    return false;
                }

                int amount = Math.min(available, remaining);
                if (existing == null) {
                    existing = new MutableSlotPlan(
                            sourceIndex,
                            source.key(),
                            slot,
                            current.copyWithCount(1)
                    );
                    this.mutablePlans.put(ref, existing);
                }

                existing.amount += amount;
                existing.predicates.add(ingredient);
                remaining -= amount;
            }
        }

        return remaining == 0;
    }

    private boolean simulate() {
        for (SlotPlan plan : this.slotPlans) {
            CraftingItemSource source =
                    this.sources.get(plan.sourceIndex());
            ItemStack simulated = source.extractItem(
                    plan.slot(),
                    plan.amount(),
                    true
            );

            if (!matchesExtracted(simulated, plan)) {
                return false;
            }
        }

        return true;
    }

    private boolean revalidate() {
        if (!this.session.validate(
                this.player,
                this.session.getContainerId()
        )) {
            return false;
        }

        for (SlotPlan plan : this.slotPlans) {
            CraftingItemSource source =
                    this.sources.get(plan.sourceIndex());

            if (!source.key().equals(plan.sourceKey())
                    || !source.isValid(this.player)) {
                return false;
            }

            ItemStack current =
                    source.getStackInSlot(plan.slot());
            if (current.isEmpty()
                    || current.getCount() < plan.amount()
                    || !sameIdentity(
                    current,
                    plan.expectedStack()
            )
                    || !matchesPredicates(
                    current,
                    plan.predicates()
            )) {
                return false;
            }

            ItemStack simulated = source.extractItem(
                    plan.slot(),
                    plan.amount(),
                    true
            );
            if (!matchesExtracted(simulated, plan)) {
                return false;
            }
        }

        return true;
    }

    private boolean commit() {
        for (SlotPlan plan : this.slotPlans) {
            CraftingItemSource source =
                    this.sources.get(plan.sourceIndex());

            ItemStack extracted = source.extractItem(
                    plan.slot(),
                    plan.amount(),
                    false
            );

            if (!extracted.isEmpty()) {
                this.extractedForRollback.add(
                        new ExtractedEntry(
                                source,
                                plan.slot(),
                                extracted.copy()
                        )
                );
            }

            if (!matchesExtracted(extracted, plan)) {
                return false;
            }
        }

        return true;
    }

    private boolean rollback() {
        boolean fullyRestored = true;
        LinkedHashSet<CraftingItemSource> affectedSources =
                new LinkedHashSet<>();

        ArrayList<ExtractedEntry> reversed =
                new ArrayList<>(this.extractedForRollback);
        Collections.reverse(reversed);

        PlayerInventorySource playerFallback =
                new PlayerInventorySource(this.player);

        for (ExtractedEntry entry : reversed) {
            ItemStack remainder = entry.stack().copy();
            affectedSources.add(entry.source());

            remainder = entry.source().insertItem(
                    entry.slot(),
                    remainder,
                    false
            );

            remainder = insertIntoOtherSlots(
                    entry.source(),
                    entry.slot(),
                    remainder
            );

            if (!remainder.isEmpty()) {
                affectedSources.add(playerFallback);
                remainder = insertIntoOtherSlots(
                        playerFallback,
                        -1,
                        remainder
                );
            }

            if (!remainder.isEmpty()) {
                fullyRestored = false;
                LOGGER.error(
                        "Gunsmith transaction rollback could not restore {} x{} to {}; dropping the remainder for player {}",
                        remainder.getHoverName().getString(),
                        remainder.getCount(),
                        entry.source().key(),
                        this.player.getGameProfile().getName()
                );
                this.player.drop(remainder.copy(), false);
            }
        }

        synchronizeSources(affectedSources);
        this.extractedForRollback.clear();
        return fullyRestored;
    }

    private ItemStack insertIntoOtherSlots(
            CraftingItemSource source,
            int excludedSlot,
            ItemStack stack
    ) {
        ItemStack remainder = stack;
        int slots = source.slotCount();

        for (int slot = 0;
             slot < slots && !remainder.isEmpty();
             slot++) {
            if (slot == excludedSlot) {
                continue;
            }
            remainder = source.insertItem(
                    slot,
                    remainder,
                    false
            );
        }

        return remainder;
    }

    private void synchronizePlannedSources() {
        LinkedHashSet<CraftingItemSource> used =
                new LinkedHashSet<>();
        for (SlotPlan plan : this.slotPlans) {
            used.add(this.sources.get(plan.sourceIndex()));
        }
        synchronizeSources(used);
    }

    private void synchronizeSources(
            Set<CraftingItemSource> sourcesToSynchronize
    ) {
        for (CraftingItemSource source : sourcesToSynchronize) {
            source.markChanged();
            source.synchronize(this.player);
        }
    }

    private ItemStack createResultStack() {
        ItemStack result = this.recipe
                .getResultItem(
                        this.player.level().registryAccess()
                )
                .copy();
        return result;
    }

    private boolean spawnOutput(ItemStack result) {
        ItemEntity itemEntity = new ItemEntity(
                this.player.level(),
                this.player.getX(),
                this.player.getY() + 0.5D,
                this.player.getZ(),
                result.copy()
        );
        itemEntity.setPickUpDelay(0);
        return this.player.level().addFreshEntity(itemEntity);
    }

    private static boolean matchesExtracted(
            ItemStack extracted,
            SlotPlan plan
    ) {
        return !extracted.isEmpty()
                && extracted.getCount() == plan.amount()
                && sameIdentity(
                extracted,
                plan.expectedStack()
        )
                && matchesPredicates(
                extracted,
                plan.predicates()
        );
    }

    private static boolean matchesPredicates(
            ItemStack stack,
            List<Ingredient> predicates
    ) {
        for (Ingredient predicate : predicates) {
            if (!predicate.test(stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameIdentity(
            ItemStack first,
            ItemStack second
    ) {
        return ItemStack.isSameItemSameTags(first, second);
    }

    private record SlotRef(int sourceIndex, int slot) {
    }

    private static final class MutableSlotPlan {
        private final int sourceIndex;
        private final CraftingSourceKey sourceKey;
        private final int slot;
        private final ItemStack expectedStack;
        private final ArrayList<Ingredient> predicates =
                new ArrayList<>();
        private int amount;

        private MutableSlotPlan(
                int sourceIndex,
                CraftingSourceKey sourceKey,
                int slot,
                ItemStack expectedStack
        ) {
            this.sourceIndex = sourceIndex;
            this.sourceKey = sourceKey;
            this.slot = slot;
            this.expectedStack = expectedStack;
        }

        private SlotPlan freeze() {
            return new SlotPlan(
                    this.sourceIndex,
                    this.sourceKey,
                    this.slot,
                    this.expectedStack.copy(),
                    this.amount,
                    List.copyOf(this.predicates)
            );
        }
    }

    private record SlotPlan(
            int sourceIndex,
            CraftingSourceKey sourceKey,
            int slot,
            ItemStack expectedStack,
            int amount,
            List<Ingredient> predicates
    ) {
    }

    public record ExtractedEntry(
            CraftingItemSource source,
            int slot,
            ItemStack stack
    ) {
    }

    public enum CraftFailure {
        INVALID_MENU,
        INVALID_SESSION,
        INVALID_RECIPE,
        DUPLICATE_REQUEST,
        OUT_OF_RANGE,
        SOURCE_CHANGED,
        INSUFFICIENT_MATERIALS,
        TRANSACTION_FAILED,
        NO_OUTPUT_SPACE,
        FEATURE_DISABLED
    }

    public static final class CraftResult {
        private final boolean success;
        private final ItemStack output;
        @Nullable
        private final CraftFailure failureReason;

        private CraftResult(
                boolean success,
                ItemStack output,
                @Nullable CraftFailure failureReason
        ) {
            this.success = success;
            this.output = output;
            this.failureReason = failureReason;
        }

        public static CraftResult success(ItemStack output) {
            return new CraftResult(
                    true,
                    output.copy(),
                    null
            );
        }

        public static CraftResult fail(CraftFailure reason) {
            return new CraftResult(
                    false,
                    ItemStack.EMPTY,
                    reason
            );
        }

        public boolean success() {
            return this.success;
        }

        public ItemStack output() {
            return this.output.copy();
        }

        @Nullable
        public CraftFailure failureReason() {
            return this.failureReason;
        }
    }
}
