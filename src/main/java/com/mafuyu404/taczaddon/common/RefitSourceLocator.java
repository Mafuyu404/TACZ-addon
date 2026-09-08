package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.NearbyInventorySourceResolver.SourceKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record RefitSourceLocator(ResourceLocation dimension, BlockPos pos, SourceKind kind, int slot) {
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension); buffer.writeBlockPos(pos); buffer.writeEnum(kind); buffer.writeVarInt(slot);
    }
    public static RefitSourceLocator read(RegistryFriendlyByteBuf buffer) {
        return new RefitSourceLocator(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readEnum(SourceKind.class), buffer.readVarInt());
    }
}
