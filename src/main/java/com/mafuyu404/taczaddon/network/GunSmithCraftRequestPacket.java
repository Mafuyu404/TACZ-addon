package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.init.CommonConfig;
import com.mafuyu404.taczaddon.init.GunSmithCraftingSessionManager;
import com.mafuyu404.taczaddon.init.GunSmithCraftingSources;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.init.crafting.CraftingTransaction;
import com.mafuyu404.taczaddon.mixin.tacz.v1_1_8.GunSmithTableMenuAccess;
import com.mojang.logging.LogUtils;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class GunSmithCraftRequestPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int containerId;
    private final long requestId;
    private final ResourceLocation recipeId;
    private final int requestedCount;

    public GunSmithCraftRequestPacket(
            int containerId,
            long requestId,
            ResourceLocation recipeId,
            int requestedCount
    ) {
        this.containerId = containerId;
        this.requestId = requestId;
        this.recipeId = recipeId;
        this.requestedCount = requestedCount;
    }

    public static void encode(
            GunSmithCraftRequestPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(message.containerId);
        buffer.writeLong(message.requestId);
        buffer.writeResourceLocation(message.recipeId);
        buffer.writeInt(message.requestedCount);
    }

    public static GunSmithCraftRequestPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new GunSmithCraftRequestPacket(
                buffer.readInt(),
                buffer.readLong(),
                buffer.readResourceLocation(),
                buffer.readInt()
        );
    }

    public static void handle(
            GunSmithCraftRequestPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(
                message,
                context.getSender()
        ));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(
            GunSmithCraftRequestPacket message,
            @Nullable ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        GunSmithCraftingSessionManager.GunSmithCraftingSession session =
                GunSmithCraftingSessionManager.getSession(
                        player.getUUID()
                );

        boolean structurallyValid =
                session != null
                        && session.validate(
                        player,
                        message.containerId
                );
        GunSmithCraftingSessionManager.SessionRequestDecision decision =
                GunSmithCraftingSessionManager.evaluateRequest(
                        session,
                        message.containerId,
                        structurallyValid
                );

        if (decision.shouldRemoveMatchingSession()) {
            GunSmithCraftingSessionManager.removeSession(
                    player.getUUID(),
                    message.containerId
            );
        }

        if (!decision.accepted()) {
            sendFailure(
                    player,
                    message,
                    CraftingTransaction.CraftFailure.INVALID_SESSION
            );
            return;
        }

        if (!(player.containerMenu
                instanceof GunSmithTableMenu menu)
                || !(menu
                instanceof GunSmithTableMenuAccess menuAccess)) {
            sendFailure(
                    player,
                    message,
                    CraftingTransaction.CraftFailure.INVALID_MENU
            );
            return;
        }

        /*
         * Use TaCZ's own private getRecipe implementation. It contains the
         * default-table exception, RecipeFilter check, and tab-membership
         * validation. A recipe tab is not the workbench block id.
         */
        GunSmithTableRecipe recipe =
                menuAccess.taczaddon$invokeGetRecipe(
                        message.recipeId,
                        player.level().getRecipeManager()
                );

        if (recipe == null) {
            sendFailure(
                    player,
                    message,
                    CraftingTransaction.CraftFailure.INVALID_RECIPE
            );
            return;
        }

        if (!session.acceptCraftRequestId(message.requestId)) {
            sendFailure(
                    player,
                    message,
                    CraftingTransaction.CraftFailure.DUPLICATE_REQUEST
            );
            return;
        }

        int requestedCount = Math.max(
                1,
                Math.min(
                        message.requestedCount,
                        CommonConfig.getBatchCraftMax()
                )
        );

        GunSmithCraftingSources.ResolvedSources resolvedSources;
        try {
            resolvedSources =
                    GunSmithCraftingSources.resolve(player, session);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Gunsmith source resolution failed for player {}",
                    player.getGameProfile().getName(),
                    exception
            );
            sendFailure(
                    player,
                    message,
                    CraftingTransaction.CraftFailure.TRANSACTION_FAILED
            );
            return;
        }

        int craftedExecutions = 0;
        ItemStack outputPerCraft = ItemStack.EMPTY;
        CraftingTransaction.CraftFailure stopReason = null;

        for (int index = 0; index < requestedCount; index++) {
            if (!session.validate(player, message.containerId)) {
                GunSmithCraftingSessionManager.removeSession(
                        player.getUUID(),
                        session
                );
                stopReason =
                        CraftingTransaction.CraftFailure.INVALID_SESSION;
                break;
            }

            CraftingTransaction.CraftResult result =
                    CraftingTransaction.execute(
                            player,
                            session,
                            recipe,
                            resolvedSources.sources()
                    );

            if (!result.success()) {
                stopReason = result.failureReason();
                break;
            }

            craftedExecutions++;
            if (outputPerCraft.isEmpty()) {
                outputPerCraft = result.output();
            }
        }

        if (craftedExecutions <= 0) {
            sendFailure(
                    player,
                    message,
                    stopReason == null
                            ? CraftingTransaction.CraftFailure
                            .INSUFFICIENT_MATERIALS
                            : stopReason
            );
            return;
        }

        session.markSourcesChanged();

        NetworkHandler.sendToClient(
                player,
                new GunSmithCraftResultPacket(
                        message.containerId,
                        message.requestId,
                        true,
                        craftedExecutions,
                        outputPerCraft,
                        stopReason
                )
        );
    }

    private static void sendFailure(
            ServerPlayer player,
            GunSmithCraftRequestPacket request,
            CraftingTransaction.CraftFailure failure
    ) {
        NetworkHandler.sendToClient(
                player,
                new GunSmithCraftResultPacket(
                        request.containerId,
                        request.requestId,
                        false,
                        0,
                        ItemStack.EMPTY,
                        failure
                )
        );
    }
}
