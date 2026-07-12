package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.client.AttachmentTooltipDiffService;
import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = ClientAttachmentItemTooltip.class, remap = false)
public class ClientAttachmentItemTooltipMixin {

    @Shadow
    private ItemStack attachment;

    @Shadow
    @Final
    private List<Component> components;

    @Inject(
            method = "getAllAllowGuns",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void taczaddon$limitAllowGunDisplay(
            List<ItemStack> output,
            ResourceLocation attachmentId,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        int max = Config.getAllowGunAmount();
        if (output.size() <= max) {
            return;
        }

        List<ItemStack> limited = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            limited.add(output.get(i).copy());
        }

        cir.setReturnValue(limited);
    }

    @Inject(
            method =
                    "<init>(Lcom/tacz/guns/inventory/tooltip/"
                            + "AttachmentItemTooltip;)V",
            at = @At("TAIL"),
            require = 1
    )
    private void taczaddon$appendAttributeDifferences(
            AttachmentItemTooltip tooltip,
            CallbackInfo ci
    ) {
        if (!Config.SHOW_ATTACHMENT_ATTRIBUTE.get()) {
            return;
        }

        ItemStack candidateAttachment = this.attachment.copy();
        if (candidateAttachment.isEmpty()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack heldGun = player.getMainHandItem().copy();
        if (heldGun.isEmpty()) {
            return;
        }

        Map<String, AttachmentTooltipDiffService.PropertyDifference>
                differences;

        try {
            differences =
                    AttachmentTooltipDiffService.calculate(
                            heldGun,
                            candidateAttachment
                    );
        } catch (RuntimeException ignored) {
            return;
        }

        if (differences.isEmpty()) {
            return;
        }

        List<Component> transformed =
                new ArrayList<>(this.components.size());

        for (Component original : this.components) {
            transformed.add(
                    taczaddon$transformComponent(
                            original,
                            differences
                    )
            );
        }

        this.components.clear();
        this.components.addAll(transformed);
    }


    @Unique
    private static Component taczaddon$transformComponent(
            Component original,
            Map<String, AttachmentTooltipDiffService.PropertyDifference>
                    differences
    ) {
        String translationKey =
                taczaddon$getTranslationKey(original);

        if (translationKey == null) {
            return original;
        }

        String propertyKey =
                AttachmentTooltipDiffService
                        .normalizeTooltipKey(translationKey);

        if (propertyKey == null) {
            return original;
        }

        AttachmentTooltipDiffService.PropertyDifference diff =
                differences.get(propertyKey);

        if (diff == null) {
            return original;
        }

        String formatted =
                taczaddon$formatDifference(propertyKey, diff);

        if (formatted == null) {
            return original;
        }

        MutableComponent enhanced =
                Component.translatable(diff.diagramTitleKey())
                        .append(Component.literal(" "))
                        .append(Component.literal(formatted));

        enhanced.setStyle(original.getStyle());
        return enhanced;
    }

    @Unique
    private static String taczaddon$formatDifference(
            String propertyKey,
            AttachmentTooltipDiffService.PropertyDifference diff
    ) {
        double delta = diff.absoluteDelta();

        if (Double.isNaN(delta) || Double.isInfinite(delta)) {
            return null;
        }

        String sign = delta > 0.0D ? "+" : "";

        String formatted =
                sign + String.format(
                        Locale.ROOT,
                        "%.2f",
                        delta
                );

        if ("weight".equals(propertyKey)) {
            formatted += "kg";
        } else if ("ads".equals(propertyKey)
                || propertyKey.contains("time")) {
            formatted += "s";
        } else if ("aim_inaccuracy".equals(propertyKey)
                || "armor_ignore".equals(propertyKey)) {
            formatted += "%";
        } else if ("rpm".equals(propertyKey)) {
            formatted += "rpm";
        } else if ("effective_range".equals(propertyKey)) {
            formatted += "m";
        } else if (propertyKey.contains("ammo_speed")) {
            formatted += "m/s";
        }

        OptionalDouble relative =
                diff.relativePercent();

        if (relative.isPresent()) {
            double pct = relative.getAsDouble();

            if (Double.isFinite(pct) && pct != 0.0D) {
                String pctSign = pct > 0.0D ? "+" : "";

                formatted += " ("
                        + pctSign
                        + String.format(
                        Locale.ROOT,
                        "%.0f",
                        pct
                )
                        + "%)";
            }
        }

        return formatted;
    }

    @Unique
    private static String taczaddon$getTranslationKey(
            Component component
    ) {
        ComponentContents contents = component.getContents();

        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }

        return null;
    }
}
