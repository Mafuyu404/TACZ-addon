package com.mafuyu404.taczaddon.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GunRefitScreenAccess {
    void taczaddon$rebuildLiberatedAttachmentButtons();
}
