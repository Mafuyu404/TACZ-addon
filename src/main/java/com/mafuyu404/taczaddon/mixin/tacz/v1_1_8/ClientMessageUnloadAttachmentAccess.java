package com.mafuyu404.taczaddon.mixin.tacz.v1_1_8;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public interface ClientMessageUnloadAttachmentAccess {
    @Accessor("gunSlotIndex")
    int taczaddon$getGunSlotIndex();

    @Accessor("attachmentType")
    AttachmentType taczaddon$getAttachmentType();
}
