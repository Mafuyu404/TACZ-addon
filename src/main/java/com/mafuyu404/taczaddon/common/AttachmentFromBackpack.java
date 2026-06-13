package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.VirtualInventoryChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class AttachmentFromBackpack {
    public static void syncBackpackWhenLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Intentionally disabled; backpack inventory syncing is handled through SophisticatedBackpacksCompat.
    }

    public static void onAttachmentChange(VirtualInventoryChangeEvent.SetItemEvent event) {
        // Intentionally disabled; virtual inventory mutations should not write directly into backpack storage here.
    }

    public static void onAttachmentUnload(VirtualInventoryChangeEvent.AddEvent event) {
        // Intentionally disabled; unloaded attachments remain in the virtual inventory flow.
    }
}
