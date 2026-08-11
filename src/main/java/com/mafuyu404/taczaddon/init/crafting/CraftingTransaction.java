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

    private final ArrayList<SlotPlan> slotPlans = new ArrayList<>();
    private final ArrayList<ExtractedEntry> extractedForRollback =
            new ArrayList<>();

    private TransactionPhase phase = TransactionPhase.PLANNING;
    private boolean mutationStarted;
    private boolean outputSpawned;

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

        try {
            if (player.isCreative()) {
                ItemStack result = transaction.createResultStack();
                if (result.isEmpty()
                        || !transaction.spawnOutput(result)) {
                    return CraftResult.fail(
                            CraftFailure.TRANSACTION_FAILED
                    );
                }
                transaction.finishBestEffort();
                return CraftResult.success(result);
            }

            transaction.phase = TransactionPhase.PLANNING;
            if (!transaction.plan()) {
                return CraftResult.fail(
                        CraftFailure.INSUFFICIENT_MATERIALS
                );
            }

            transaction.phase = TransactionPhase.SIMULATING;
            if (!transaction.simulate()) {
                return CraftResult.fail(
                        CraftFailure.INSUFFICIENT_MATERIALS
                );
            }

            transaction.phase = TransactionPhase.REVALIDATING;
            if (!transaction.revalidate()) {
                return CraftResult.fail(
                        CraftFailure.SOURCE_CHANGED
                );
            }

            transaction.phase = TransactionPhase.COMMITTING;
            transaction.mutationStarted = true;
            if (!transaction.commit()) {
                transaction.rollbackSafely();
                return CraftResult.fail(
                        CraftFailure.TRANSACTION_FAILED
                );
            }

            transaction.phase = TransactionPhase.OUTPUT_CREATION;
            ItemStack result = transaction.createResultStack();
            if (result.isEmpty()
                    || !transaction.spawnOutput(result)) {
                transaction.rollbackSafely();
                return CraftResult.fail(
                        CraftFailure.TRANSACTION_FAILED
                );
            }

            transaction.phase = TransactionPhase.COMPLETE;
            transaction.finishBestEffort();
            return CraftResult.success(result);
        } catch (RuntimeException exception) {
            if (!transaction.outputSpawned
                    && (transaction.mutationStarted
                    || !transaction.extractedForRollback.isEmpty())) {
                transaction.rollbackSafely();
            }

            LOGGER.error(
                    "Gunsmith transaction failed for player {} recipe {} phase {}",
                    player.getGameProfile().getName(),
                    recipe.getId(),
                    transaction.phase,
                    exception
            );
            return CraftResult.fail(
                    CraftFailure.TRANSACTION_FAILED
            );
        }
    }

    /**
     * Builds one deterministic global material allocation.
     *
     * A simple ingredient-by-ingredient greedy allocator is not sufficient:
     * overlapping predicates can have a valid assignment that greedy ordering
     * misses. Example: ingredient A accepts {iron,gold}, ingredient B accepts
     * {iron}, inventory has one iron and one gold. A greedy A-first pass may
     * reserve iron and incorrectly reject B.
     *
     * Model the recipe as an integral max-flow problem instead:
     *
     * source -> ingredient demand -> candidate slot -> sink
     *
     * Slot order remains deterministic (player inventory first, then the
     * already deterministic external-source order). Max flow may reroute an
     * earlier broad predicate only when that is required to satisfy a narrower
     * predicate.
     */
    private boolean plan() {
        this.slotPlans.clear();

        List<GunSmithTableIngredient> inputs =
                this.recipe.getInputs();
        if (inputs == null) {
            return false;
        }

        ArrayList<IngredientDemand> demands = new ArrayList<>();
        long totalRequired = 0L;

        for (GunSmithTableIngredient input : inputs) {
            if (input == null) {
                return false;
            }

            int required = input.getCount();
            if (required <= 0) {
                continue;
            }

            Ingredient ingredient = input.getIngredient();
            if (ingredient == null) {
                return false;
            }

            demands.add(new IngredientDemand(
                    ingredient,
                    required
            ));
            totalRequired += required;
        }

        if (totalRequired == 0L) {
            return true;
        }

        ArrayList<SlotCandidate> candidates =
                collectSlotCandidates(demands);
        if (candidates.isEmpty()) {
            return false;
        }

        return allocateByMaxFlow(
                demands,
                candidates,
                totalRequired
        );
    }

    private ArrayList<SlotCandidate> collectSlotCandidates(
            List<IngredientDemand> demands
    ) {
        ArrayList<SlotCandidate> candidates =
                new ArrayList<>();

        for (int sourceIndex = 0;
             sourceIndex < this.sources.size();
             sourceIndex++) {
            CraftingItemSource source =
                    this.sources.get(sourceIndex);
            int slotCount = source.slotCount();
            if (slotCount <= 0) {
                continue;
            }

            for (int slot = 0; slot < slotCount; slot++) {
                ItemStack current =
                        source.getStackInSlot(slot);
                if (current == null
                        || current.isEmpty()
                        || current.getCount() <= 0) {
                    continue;
                }

                boolean matchesAny = false;
                for (IngredientDemand demand : demands) {
                    if (demand.ingredient().test(current)) {
                        matchesAny = true;
                        break;
                    }
                }

                if (!matchesAny) {
                    continue;
                }

                candidates.add(new SlotCandidate(
                        sourceIndex,
                        source.key(),
                        slot,
                        current.copyWithCount(1),
                        current.getCount()
                ));
            }
        }

        return candidates;
    }

    private boolean allocateByMaxFlow(
            List<IngredientDemand> demands,
            List<SlotCandidate> candidates,
            long totalRequired
    ) {
        int demandCount = demands.size();
        int candidateCount = candidates.size();

        int sourceNode = 0;
        int demandNodeBase = 1;
        int candidateNodeBase =
                demandNodeBase + demandCount;
        int sinkNode =
                candidateNodeBase + candidateCount;

        FlowNetwork network =
                new FlowNetwork(sinkNode + 1);
        ArrayList<AllocationArc> allocationArcs =
                new ArrayList<>();

        for (int demandIndex = 0;
             demandIndex < demandCount;
             demandIndex++) {
            IngredientDemand demand =
                    demands.get(demandIndex);
            network.addEdge(
                    sourceNode,
                    demandNodeBase + demandIndex,
                    demand.amount()
            );
        }

        for (int candidateIndex = 0;
             candidateIndex < candidateCount;
             candidateIndex++) {
            SlotCandidate candidate =
                    candidates.get(candidateIndex);
            network.addEdge(
                    candidateNodeBase + candidateIndex,
                    sinkNode,
                    candidate.available()
            );
        }

        /*
         * Add demand->slot edges in deterministic recipe/slot order. Dinic's
         * residual graph can reroute earlier choices when necessary, while
         * preserving deterministic results for an unchanged source snapshot.
         */
        for (int demandIndex = 0;
             demandIndex < demandCount;
             demandIndex++) {
            IngredientDemand demand =
                    demands.get(demandIndex);

            for (int candidateIndex = 0;
                 candidateIndex < candidateCount;
                 candidateIndex++) {
                SlotCandidate candidate =
                        candidates.get(candidateIndex);

                if (!demand.ingredient().test(
                        candidate.expectedStack()
                )) {
                    continue;
                }

                long capacity = Math.min(
                        (long) demand.amount(),
                        (long) candidate.available()
                );
                FlowEdge edge = network.addEdge(
                        demandNodeBase + demandIndex,
                        candidateNodeBase + candidateIndex,
                        capacity
                );
                allocationArcs.add(new AllocationArc(
                        demandIndex,
                        candidateIndex,
                        edge
                ));
            }
        }

        long allocated = network.maxFlow(
                sourceNode,
                sinkNode,
                totalRequired
        );
        if (allocated != totalRequired) {
            return false;
        }

        MutableSlotPlan[] plansByCandidate =
                new MutableSlotPlan[candidateCount];

        for (AllocationArc arc : allocationArcs) {
            long assignedLong = arc.edge().flow();
            if (assignedLong <= 0L) {
                continue;
            }

            int assigned = Math.toIntExact(assignedLong);
            SlotCandidate candidate =
                    candidates.get(arc.candidateIndex());
            IngredientDemand demand =
                    demands.get(arc.demandIndex());

            MutableSlotPlan mutable =
                    plansByCandidate[arc.candidateIndex()];
            if (mutable == null) {
                mutable = new MutableSlotPlan(
                        candidate.sourceIndex(),
                        candidate.sourceKey(),
                        candidate.slot(),
                        candidate.expectedStack()
                );
                plansByCandidate[arc.candidateIndex()] =
                        mutable;
            }

            mutable.amount += assigned;
            mutable.predicates.add(demand.ingredient());
        }

        /*
         * Freeze in candidate order, which is source order then slot order.
         * This keeps commit/rollback deterministic.
         */
        for (MutableSlotPlan mutable : plansByCandidate) {
            if (mutable != null && mutable.amount > 0) {
                this.slotPlans.add(mutable.freeze());
            }
        }

        return true;
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
        if (this.session == null
                || !this.session.validate(
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

    private RollbackResult rollback() {
        boolean fullyRestored = true;
        LinkedHashSet<CraftingItemSource> affectedSources =
                new LinkedHashSet<>();

        ArrayList<ExtractedEntry> reversed =
                new ArrayList<>(this.extractedForRollback);
        Collections.reverse(reversed);

        PlayerInventorySource playerFallback =
                this.player == null
                        ? null
                        : new PlayerInventorySource(this.player);

        for (ExtractedEntry entry : reversed) {
            ItemStack remainder = entry.stack().copy();
            affectedSources.add(entry.source());

            remainder = insertItemSafely(
                    entry.source(),
                    entry.slot(),
                    remainder,
                    "original-slot"
            );

            if (!remainder.isEmpty()) {
                remainder = insertIntoOtherSlots(
                        entry.source(),
                        entry.slot(),
                        remainder
                );
            }

            if (!remainder.isEmpty() && playerFallback != null) {
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
                        "Gunsmith transaction rollback could not restore "
                                + "{} x{}; player {} source {} slot {} "
                                + "phase {} item {} amount {}; dropping "
                                + "remainder",
                        remainder.getHoverName().getString(),
                        remainder.getCount(),
                        playerName(),
                        entry.source().key(),
                        entry.slot(),
                        this.phase,
                        remainder.getHoverName().getString(),
                        remainder.getCount()
                );
                dropRemainder(entry, remainder);
            }
        }

        synchronizeSources(affectedSources);
        this.extractedForRollback.clear();
        return fullyRestored
                ? RollbackResult.FULLY_RESTORED
                : RollbackResult.PARTIALLY_COMPENSATED;
    }

    private RollbackResult rollbackSafely() {
        try {
            RollbackResult result = rollback();
            if (result == RollbackResult.PARTIALLY_COMPENSATED) {
                LOGGER.warn(
                        "Gunsmith transaction was only partially compensated "
                                + "for player {} phase {}",
                        playerName(),
                        this.phase
                );
            }
            return result;
        } catch (RuntimeException exception) {
            /*
             * Individual source operations are already guarded inside
             * rollback(). This outer boundary is a final containment layer:
             * rollback must never escape and crash the packet handler.
             *
             * Do not attempt a second blanket rollback here because some
             * entries may already have been restored; replaying them could
             * duplicate materials.
             */
            LOGGER.error(
                    "Unexpected gunsmith rollback failure for player {} "
                            + "recipe {} phase {}",
                    playerName(),
                    this.recipe.getId(),
                    this.phase,
                    exception
            );
            return RollbackResult.PARTIALLY_COMPENSATED;
        }
    }

    private ItemStack insertItemSafely(
            CraftingItemSource source,
            int slot,
            ItemStack stack,
            String operation
    ) {
        try {
            return source.insertItem(slot, stack, false);
        } catch (RuntimeException exception) {
            logRollbackOperation(
                    operation,
                    source,
                    slot,
                    stack,
                    exception
            );
            return stack;
        }
    }

    private ItemStack insertIntoOtherSlots(
            CraftingItemSource source,
            int excludedSlot,
            ItemStack stack
    ) {
        ItemStack remainder = stack;
        int slots;
        try {
            slots = source.slotCount();
        } catch (RuntimeException exception) {
            logRollbackOperation(
                    "slot-count",
                    source,
                    -1,
                    stack,
                    exception
            );
            return remainder;
        }

        for (int slot = 0;
             slot < slots && !remainder.isEmpty();
             slot++) {
            if (slot == excludedSlot) {
                continue;
            }
            remainder = insertItemSafely(
                    source,
                    slot,
                    remainder,
                    "other-slot"
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

    private void finishBestEffort() {
        synchronizePlannedSources();
        try {
            this.player.inventoryMenu.broadcastFullState();
        } catch (RuntimeException exception) {
            logSynchronizationFailure(
                    "player-inventory-broadcast",
                    null,
                    exception
            );
        }
    }

    private void synchronizeSources(
            Set<CraftingItemSource> sourcesToSynchronize
    ) {
        for (CraftingItemSource source : sourcesToSynchronize) {
            try {
                source.markChanged();
            } catch (RuntimeException exception) {
                logSynchronizationFailure(
                        "mark-changed",
                        source,
                        exception
                );
            }

            try {
                source.synchronize(this.player);
            } catch (RuntimeException exception) {
                logSynchronizationFailure(
                        "synchronize",
                        source,
                        exception
                );
            }
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
        boolean added = this.player.level().addFreshEntity(itemEntity);
        if (added) {
            this.outputSpawned = true;
        }
        return added;
    }

    private void dropRemainder(
            ExtractedEntry entry,
            ItemStack remainder
    ) {
        if (this.player == null) {
            return;
        }

        try {
            this.player.drop(remainder.copy(), false);
        } catch (RuntimeException exception) {
            logRollbackOperation(
                    "drop-remainder",
                    entry.source(),
                    entry.slot(),
                    remainder,
                    exception
            );
        }
    }

    private void logRollbackOperation(
            String operation,
            CraftingItemSource source,
            int slot,
            ItemStack stack,
            RuntimeException exception
    ) {
        LOGGER.error(
                "Gunsmith rollback {} threw for player {} source {} "
                        + "slot {} item {} amount {} phase {}",
                operation,
                playerName(),
                source.key(),
                slot,
                stack.getHoverName().getString(),
                stack.getCount(),
                this.phase,
                exception
        );
    }

    private void logSynchronizationFailure(
            String operation,
            @Nullable CraftingItemSource source,
            RuntimeException exception
    ) {
        LOGGER.error(
                "Gunsmith post-commit {} failed for player {} source {} "
                        + "phase {}",
                operation,
                playerName(),
                source == null ? "player-inventory" : source.key(),
                this.phase,
                exception
        );
    }

    private String playerName() {
        return this.player == null
                ? "unknown"
                : this.player.getGameProfile().getName();
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

    private record IngredientDemand(
            Ingredient ingredient,
            int amount
    ) {
    }

    private record SlotCandidate(
            int sourceIndex,
            CraftingSourceKey sourceKey,
            int slot,
            ItemStack expectedStack,
            int available
    ) {
    }

    private record AllocationArc(
            int demandIndex,
            int candidateIndex,
            FlowEdge edge
    ) {
    }

    private static final class FlowNetwork {
        private final List<FlowEdge>[] graph;
        private final int[] level;
        private final int[] nextEdge;

        @SuppressWarnings("unchecked")
        private FlowNetwork(int nodeCount) {
            this.graph = new List[nodeCount];
            for (int node = 0; node < nodeCount; node++) {
                this.graph[node] = new ArrayList<>();
            }
            this.level = new int[nodeCount];
            this.nextEdge = new int[nodeCount];
        }

        private FlowEdge addEdge(
                int from,
                int to,
                long capacity
        ) {
            if (capacity < 0L) {
                throw new IllegalArgumentException(
                        "Negative flow capacity"
                );
            }

            FlowEdge forward = new FlowEdge(
                    to,
                    this.graph[to].size(),
                    capacity,
                    capacity
            );
            FlowEdge reverse = new FlowEdge(
                    from,
                    this.graph[from].size(),
                    0L,
                    0L
            );

            this.graph[from].add(forward);
            this.graph[to].add(reverse);
            return forward;
        }

        private long maxFlow(
                int source,
                int sink,
                long target
        ) {
            long flow = 0L;

            while (flow < target
                    && buildLevels(source, sink)) {
                Arrays.fill(this.nextEdge, 0);

                while (flow < target) {
                    long pushed = push(
                            source,
                            sink,
                            target - flow
                    );
                    if (pushed <= 0L) {
                        break;
                    }
                    flow += pushed;
                }
            }

            return flow;
        }

        private boolean buildLevels(
                int source,
                int sink
        ) {
            Arrays.fill(this.level, -1);
            ArrayDeque<Integer> queue =
                    new ArrayDeque<>();
            this.level[source] = 0;
            queue.add(source);

            while (!queue.isEmpty()) {
                int node = queue.removeFirst();

                for (FlowEdge edge : this.graph[node]) {
                    if (edge.residualCapacity <= 0L
                            || this.level[edge.to] >= 0) {
                        continue;
                    }

                    this.level[edge.to] =
                            this.level[node] + 1;
                    if (edge.to == sink) {
                        return true;
                    }
                    queue.addLast(edge.to);
                }
            }

            return this.level[sink] >= 0;
        }

        private long push(
                int node,
                int sink,
                long limit
        ) {
            if (node == sink) {
                return limit;
            }

            for (;
                 this.nextEdge[node] < this.graph[node].size();
                 this.nextEdge[node]++) {
                FlowEdge edge = this.graph[node]
                        .get(this.nextEdge[node]);

                if (edge.residualCapacity <= 0L
                        || this.level[edge.to]
                        != this.level[node] + 1) {
                    continue;
                }

                long pushed = push(
                        edge.to,
                        sink,
                        Math.min(
                                limit,
                                edge.residualCapacity
                        )
                );
                if (pushed <= 0L) {
                    continue;
                }

                edge.residualCapacity -= pushed;
                FlowEdge reverse =
                        this.graph[edge.to]
                                .get(edge.reverseIndex);
                reverse.residualCapacity += pushed;
                return pushed;
            }

            return 0L;
        }
    }

    private static final class FlowEdge {
        private final int to;
        private final int reverseIndex;
        private final long originalCapacity;
        private long residualCapacity;

        private FlowEdge(
                int to,
                int reverseIndex,
                long residualCapacity,
                long originalCapacity
        ) {
            this.to = to;
            this.reverseIndex = reverseIndex;
            this.residualCapacity = residualCapacity;
            this.originalCapacity = originalCapacity;
        }

        private long flow() {
            return this.originalCapacity
                    - this.residualCapacity;
        }
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

    private enum TransactionPhase {
        PLANNING,
        SIMULATING,
        REVALIDATING,
        COMMITTING,
        OUTPUT_CREATION,
        COMPLETE
    }

    enum RollbackResult {
        FULLY_RESTORED,
        PARTIALLY_COMPENSATED
    }

    public enum CraftFailure {
        INVALID_MENU,
        INVALID_SESSION,
        INVALID_RECIPE,
        DUPLICATE_REQUEST,
        SOURCE_CHANGED,
        INSUFFICIENT_MATERIALS,
        TRANSACTION_FAILED
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