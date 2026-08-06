package com.mafuyu404.taczaddon.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * Owns gamerules added by TACZ-addon.
 *
 * {@link #bootstrap()} is called explicitly from the mod constructor so the
 * rule is registered before any world is created. This class must not rely on
 * Forge event-bus registration as a class-loading side effect.
 */
public final class RuleRegistry {
    public static final GameRules.Key<GameRules.BooleanValue>
            LIBERATE_ATTACHMENT = GameRules.register(
            "liberateAttachment",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(
                    false,
                    RuleRegistry::onLiberateAttachmentChanged
            )
    );

    private RuleRegistry() {
    }

    public static void bootstrap() {
        // Calling this method is the explicit class-initialization boundary.
    }

    private static void onLiberateAttachmentChanged(
            MinecraftServer server,
            GameRules.BooleanValue value
    ) {
        boolean enabled = value.get();
        server.getPlayerList()
                .getPlayers()
                .forEach(player ->
                        NetworkHandler.sendLiberateAttachmentState(
                                player,
                                enabled
                        )
                );
    }
}
