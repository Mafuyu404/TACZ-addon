package com.mafuyu404.taczaddon.mixin.beyond;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Pseudo
@Mixin(targets = "com.solr98.beyondintegration.client.TaczAmmoCache", remap = false)
public class TaczAmmoCacheMixin {
    @Redirect(method = "applyPending()V", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;putAll(Ljava/util/Map;)V"), require = 1)
    private static void taczaddon$replaceSnapshot(
            Map<String, Integer> cache, Map<String, Integer> snapshot
    ) {
        // Beyond sends a complete snapshot, omitting ammo types whose count reached zero.
        cache.clear();
        cache.putAll(snapshot);
    }
}
