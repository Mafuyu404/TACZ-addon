package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.TACZaddon;
import com.mafuyu404.taczaddon.init.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mafuyu404.taczaddon.init.crafting.CraftingTransaction;
import com.mafuyu404.taczaddon.init.crafting.CraftingTransaction.CraftFailure;
import com.mafuyu404.taczaddon.mixin.GunSmithTableMenuAccess;
import com.tacz.guns.inventory.GunSmithTableMenu;

public record GunSmithCraftRequestPacket(int containerId, long requestId, ResourceLocation recipeId, int requestedCount) implements CustomPacketPayload {
    public static final Type<GunSmithCraftRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TACZaddon.MODID, "gunsmith_craft_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithCraftRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVarInt(packet.containerId); buffer.writeLong(packet.requestId); buffer.writeResourceLocation(packet.recipeId); buffer.writeVarInt(packet.requestedCount); },
            buffer -> new GunSmithCraftRequestPacket(buffer.readVarInt(), buffer.readLong(), buffer.readResourceLocation(), buffer.readVarInt()));
    public static void handle(GunSmithCraftRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> { if (!(context.player() instanceof ServerPlayer player)) return;
            if (ServerboundPacketGuard.isRateLimited(player, TYPE.id(), 2)) return;
            handleOnServer(packet, player); });
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    private static void handleOnServer(GunSmithCraftRequestPacket packet, ServerPlayer player) {
        int craftedExecutions = 0;
        ItemStack outputPerCraft = ItemStack.EMPTY;
        CraftFailure failure = null;
        var session = GunSmithCraftingSessionManager.getSession(player.getUUID());
        if (!(player.containerMenu instanceof GunSmithTableMenu menu) || menu.containerId != packet.containerId) {
            failure = CraftFailure.INVALID_MENU;
        } else if (session == null || !session.validate(player, packet.containerId)) {
            failure = CraftFailure.INVALID_SESSION;
        } else if (!session.acceptCraftRequestId(packet.requestId)) {
            failure = CraftFailure.DUPLICATE_REQUEST;
        } else {
            try {
                var recipe = ((GunSmithTableMenuAccess) menu).taczaddon$invokeGetRecipe(packet.recipeId, player.level().getRecipeManager());
                if (recipe == null) {
                    failure = CraftFailure.INVALID_RECIPE;
                } else {
                    int requestedCount = Math.max(1, Math.min(packet.requestedCount, Config.getBatchCraftMax()));
                    for (int i = 0; i < requestedCount; i++) {
                        if (!session.validate(player, packet.containerId)) {
                            failure = CraftFailure.INVALID_SESSION;
                            break;
                        }
                        var sources = GunSmithCraftingSources.resolve(player, session);
                        var result = CraftingTransaction.execute(player, session, recipe, sources);
                        if (!result.success()) {
                            failure = result.failureReason();
                            break;
                        }
                        craftedExecutions++;
                        if (outputPerCraft.isEmpty()) outputPerCraft = result.output().copy();
                    }
                }
            } catch (RuntimeException exception) {
                com.mojang.logging.LogUtils.getLogger().error("Gunsmith request failed for {}", player.getUUID(), exception);
                failure = CraftFailure.TRANSACTION_FAILED;
            }
        }
        if (craftedExecutions > 0) session.markSourcesChanged();
        NetworkHandler.sendToClient(player, new GunSmithCraftResultPacket(packet.containerId, packet.requestId,
                craftedExecutions > 0, craftedExecutions, outputPerCraft, failure));
        ContainerPositionPacket.refreshStoredSnapshot(player);
    }
}
