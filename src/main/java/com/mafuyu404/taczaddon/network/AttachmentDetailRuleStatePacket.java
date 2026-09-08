package com.mafuyu404.taczaddon.network;

import com.mafuyu404.taczaddon.client.ClientAttachmentDetailRuleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Type-safe S2C mirror of the server-owned showAttachmentDetail gamerule.
 */
public final class AttachmentDetailRuleStatePacket {
    private final boolean enabled;

    public AttachmentDetailRuleStatePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(
            AttachmentDetailRuleStatePacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(message.enabled);
    }

    public static AttachmentDetailRuleStatePacket decode(
            FriendlyByteBuf buffer
    ) {
        return new AttachmentDetailRuleStatePacket(
                buffer.readBoolean()
        );
    }

    public static void handle(
            AttachmentDetailRuleStatePacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ClientAttachmentDetailRuleHandler.handle(
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
