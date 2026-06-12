package com.mafuyu404.taczaddon.init;

import com.mafuyu404.taczaddon.common.LiberateAttachment;
import net.minecraft.world.level.GameRules;

public class RuleRegistry {
    public static void init() {
        // Loads this class so the game rule keys are registered.
    }
    public static final GameRules.Key<GameRules.BooleanValue> LIBERATE_ATTACHMENT = GameRules
        .register("liberateAttachment",GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false, LiberateAttachment::onRuleChange));
    public static final GameRules.Key<GameRules.BooleanValue> SHOW_ATTACHMENT_DETAIL = GameRules
        .register("showAttachmentDetail",GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false, LiberateAttachment::onRuleChange));
}