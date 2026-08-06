package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public interface ClientMessageRefitGunAccess {
    @Accessor("attachmentSlotIndex")
    int taczaddon$getAttachmentSlotIndex();

    @Accessor("gunSlotIndex")
    int taczaddon$getGunSlotIndex();

    @Accessor("attachmentType")
    AttachmentType taczaddon$getAttachmentType();
}
