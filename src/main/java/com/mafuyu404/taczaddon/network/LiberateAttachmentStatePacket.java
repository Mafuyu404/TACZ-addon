package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.client.ClientLiberateAttachmentHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Type-safe S2C mirror of the server-owned liberateAttachment gamerule.
 */
public final class LiberateAttachmentStatePacket {
    private final boolean enabled;

    public LiberateAttachmentStatePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(
            LiberateAttachmentStatePacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(message.enabled);
    }

    public static LiberateAttachmentStatePacket decode(
            FriendlyByteBuf buffer
    ) {
        return new LiberateAttachmentStatePacket(
                buffer.readBoolean()
        );
    }

    public static void handle(
            LiberateAttachmentStatePacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ClientLiberateAttachmentHandler.handle(
                                        message.enabled
                                )
                )
        );
        context.setPacketHandled(true);
    }

    public boolean enabled() {
        return this.enabled;
    }
}
