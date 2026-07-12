package com.mafuyu404.taczaddon.init.crafting;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;

/**
 * Server-side transactional crafting for the gunsmith table.
 *
 * <p>All material extraction is planned (simulated) first, then committed
 * only after all requirements are verified. If any step fails during commit,
 * the transaction attempts rollback by returning extracted items to the player.</p>
 */
public final class CraftingTransaction {

    private final ServerPlayer player;
    private final CraftingSession session;
    private final GunSmithTableRecipe recipe;
    private final List<CraftingItemSource> sources;

    /** Slots allocated during planning: sourceIndex -> slot -> amount needed. */
    private final Map<Integer, Map<Integer, Integer>> allocations = new HashMap<>();

    /** Extracted items tracked during commit for potential rollback. */
    private final List<ItemStack> extractedForRollback = new ArrayList<>();

    private CraftingTransaction(
            ServerPlayer player,
            CraftingSession session,
            GunSmithTableRecipe recipe,
            List<CraftingItemSource> sources
    ) {
        this.player = player;
        this.session = session;
        this.recipe = recipe;
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
    }

    /**
     * Execute a full crafting transaction: plan, validate, commit.
     *
     * @return the crafted output, or EMPTY on failure
     */
    public static CraftResult execute(
            ServerPlayer player,
            CraftingSession session,
            GunSmithTableRecipe recipe,
            List<CraftingItemSource> sources
    ) {
        CraftingTransaction tx = new CraftingTransaction(player, session, recipe, sources);

        // Phase 1: Plan
        if (!tx.plan()) {
            return CraftResult.fail("insufficient_materials");
        }

        // Phase 2: Commit
        if (!tx.commit()) {
            return CraftResult.fail("commit_failed");
        }

        // Phase 3: Create output
        ItemStack result = recipe.getOutput().copy();
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }

        // Mark all sources changed
        for (CraftingItemSource source : sources) {
            source.markChanged();
        }

        return CraftResult.success(result);
    }

    /**
     * Simulate extraction for every ingredient. Returns true if all ingredients
     * can be fully satisfied from the available sources.
     */
    private boolean plan() {
        List<Ingredient> ingredients = recipe.getInputs();
        if (ingredients == null || ingredients.isEmpty()) {
            return true; // No ingredients needed
        }

        for (int ingIndex = 0; ingIndex < ingredients.size(); ingIndex++) {
            Ingredient ingredient = ingredients.get(ingIndex);
            int required = recipe.getInputCounts().get(ingIndex);

            if (!allocateIngredient(ingredient, required)) {
                return false;
            }
        }
        return true;
    }

    private boolean allocateIngredient(Ingredient ingredient, int required) {
        int remaining = required;

        for (int srcIdx = 0; srcIdx < sources.size() && remaining > 0; srcIdx++) {
            CraftingItemSource source = sources.get(srcIdx);
            for (int slot = 0; slot < source.slotCount() && remaining > 0; slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                if (!ingredient.test(stack)) continue;

                int available = stack.getCount();
                if (available <= 0) continue;

                int toTake = Math.min(available, remaining);
                remaining -= toTake;

                allocations
                        .computeIfAbsent(srcIdx, k -> new HashMap<>())
                        .put(slot, toTake);
            }
        }

        return remaining <= 0;
    }

    /**
     * Execute all planned extractions. On failure, attempt rollback.
     */
    private boolean commit() {
        // Revalidate session
        if (!session.validate(player, player.containerMenu.containerId)) {
            return false;
        }

        // Revalidate all sources
        for (CraftingItemSource source : sources) {
            if (!source.isValid(player)) {
                return false;
            }
        }

        // Execute extractions
        for (Map.Entry<Integer, Map<Integer, Integer>> srcEntry : allocations.entrySet()) {
            int srcIdx = srcEntry.getKey();
            CraftingItemSource source = sources.get(srcIdx);

            for (Map.Entry<Integer, Integer> slotEntry : srcEntry.getValue().entrySet()) {
                int slot = slotEntry.getKey();
                int expected = slotEntry.getValue();

                ItemStack extracted = source.extractItem(slot, expected, false);
                if (extracted.getCount() < expected) {
                    // Partial extraction — rollback
                    if (!extracted.isEmpty()) {
                        extractedForRollback.add(extracted);
                    }
                    rollback();
                    return false;
                }
                extractedForRollback.add(extracted);
            }
        }

        return true;
    }

    private void rollback() {
        // Return extracted items to the player inventory or drop them
        for (ItemStack stack : extractedForRollback) {
            if (!stack.isEmpty()) {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                        player.getInventory(), stack, false);
                if (!remainder.isEmpty()) {
                    player.drop(remainder, false);
                }
            }
        }
        extractedForRollback.clear();
    }

    public record CraftResult(boolean success, @javax.annotation.Nullable ItemStack output, @javax.annotation.Nullable String failureReason) {
        public static CraftResult success(ItemStack output) {
            return new CraftResult(true, output, null);
        }
        public static CraftResult fail(String reason) {
            return new CraftResult(false, null, reason);
        }
    }
}
