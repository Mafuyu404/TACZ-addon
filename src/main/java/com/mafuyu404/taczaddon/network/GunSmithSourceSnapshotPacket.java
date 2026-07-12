package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.client.ClientGunSmithPacketHandler;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class GunSmithSourceSnapshotPacket {
    public static final int MAX_EXTERNAL_STACKS = 256;

    private final int containerId;
    private final long requestId;
    private final long sourceRevision;
    private final List<ItemStack> externalStacks;

    public GunSmithSourceSnapshotPacket(
            int containerId,
            long requestId,
            long sourceRevision,
            List<ItemStack> externalStacks
    ) {
        this.containerId = containerId;
        this.requestId = requestId;
        this.sourceRevision = sourceRevision;

        ArrayList<ItemStack> copies = new ArrayList<>();
        if (externalStacks != null) {
            int size = Math.min(
                    externalStacks.size(),
                    MAX_EXTERNAL_STACKS
            );
            for (int index = 0; index < size; index++) {
                ItemStack stack = externalStacks.get(index);
                if (stack != null && !stack.isEmpty()) {
                    copies.add(stack.copy());
                }
            }
        }
        this.externalStacks =
                Collections.unmodifiableList(copies);
    }

    public static void encode(
            GunSmithSourceSnapshotPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(message.containerId);
        buffer.writeLong(message.requestId);
        buffer.writeLong(message.sourceRevision);
        buffer.writeInt(message.externalStacks.size());

        for (ItemStack stack : message.externalStacks) {
            buffer.writeItemStack(stack, true);
        }
    }

    public static GunSmithSourceSnapshotPacket decode(
            FriendlyByteBuf buffer
    ) {
        int containerId = buffer.readInt();
        long requestId = buffer.readLong();
        long sourceRevision = buffer.readLong();
        int declaredSize = buffer.readInt();

        if (declaredSize < 0
                || declaredSize > MAX_EXTERNAL_STACKS) {
            throw new DecoderException(
                    "Invalid gunsmith source snapshot size: "
                            + declaredSize
            );
        }

        ArrayList<ItemStack> stacks =
                new ArrayList<>(declaredSize);
        for (int index = 0; index < declaredSize; index++) {
            stacks.add(buffer.readItem());
        }

        return new GunSmithSourceSnapshotPacket(
                containerId,
                requestId,
                sourceRevision,
                stacks
        );
    }

    public static void handle(
            GunSmithSourceSnapshotPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ClientGunSmithPacketHandler
                                        .handleSourceSnapshot(message)
                )
        );
        context.setPacketHandled(true);
    }

    public int containerId() {
        return this.containerId;
    }

    public long requestId() {
        return this.requestId;
    }

    public long sourceRevision() {
        return this.sourceRevision;
    }

    public List<ItemStack> externalStacks() {
        ArrayList<ItemStack> copies =
                new ArrayList<>(this.externalStacks.size());
        for (ItemStack stack : this.externalStacks) {
            copies.add(stack.copy());
        }
        return Collections.unmodifiableList(copies);
    }
}
