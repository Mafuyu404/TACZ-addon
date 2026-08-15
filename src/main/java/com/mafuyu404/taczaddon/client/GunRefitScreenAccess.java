package com.mafuyu404.taczaddon.client;

import com.mafuyu404.taczaddon.common.RefitSourceResolver;
import com.mafuyu404.taczaddon.init.crafting.GunSmithSourceScreenAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public interface GunRefitScreenAccess {
    void taczaddon$rebuildLiberatedAttachmentButtons();

    GunSmithSourceScreenAccess.AcceptResult
    taczaddon$acceptRefitSourceSnapshot(
            long requestId,
            List<RefitSourceResolver.RefitExternalCandidate> candidates
    );

    void taczaddon$requestRefitSourceRefresh();

    void taczaddon$tickRefitSourceRefresh();

    void taczaddon$onRefitScreenInit();

    void taczaddon$rebuildRefitCandidateButtons();
}
