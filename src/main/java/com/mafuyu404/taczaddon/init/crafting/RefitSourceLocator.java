package com.mafuyu404.taczaddon.init.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RefitSourceLocator(
        ResourceKey<Level> dimension,
        BlockPos pos,
        int slot
) {
    public RefitSourceLocator {
        pos = pos.immutable();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.dimension.location());
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.slot);
    }

    public static RefitSourceLocator decode(FriendlyByteBuf buffer) {
        ResourceLocation dimensionId = buffer.readResourceLocation();
        BlockPos pos = buffer.readBlockPos();
        int slot = buffer.readInt();
        return new RefitSourceLocator(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                pos,
                slot
        );
    }

    public static RefitSourceLocator fromBlockSource(
            CraftingSourceKey.BlockEntity key,
            int slot
    ) {
        return new RefitSourceLocator(
                key.dimension(),
                key.pos(),
                slot
        );
    }
}
