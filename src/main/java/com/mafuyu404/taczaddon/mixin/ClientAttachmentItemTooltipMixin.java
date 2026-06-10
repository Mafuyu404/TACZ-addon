package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(value = ClientAttachmentItemTooltip.class, remap = false)
public class ClientAttachmentItemTooltipMixin {
    @Shadow @Final private ResourceLocation attachmentId;

    @Inject(method = "getAllAllowGuns", at = @At("RETURN"), cancellable = true)
    private static void modifyShowAllowGun(List<ItemStack> output, ResourceLocation attachmentId, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> result = new ArrayList<>();
        int amount = Config.getAllowGunAmount();
        for (int i = 0; i < Math.min(amount, output.size()); i++) {
            result.add(output.get(i).copy());
        }
        cir.setReturnValue(result);
    }

    @Redirect(method = "lambda$addText$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/JsonProperty;getComponents()Ljava/util/List;"))
    private List<Component> modifyAttachmentDetail(JsonProperty<?> value) {
        List<Component> originalComponents = value.getComponents();
        if (!Config.SHOW_ATTACHMENT_ATTRIBUTE.get()) return originalComponents;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return originalComponents;

        ItemStack gunItem = player.getMainHandItem().copy();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) return originalComponents;

        boolean allowAttachment = AllowAttachmentTagMatcher.match(iGun.getGunId(gunItem), this.attachmentId);
        if (!allowAttachment) return originalComponents;

        ItemStack attachmentItem = AttachmentItemBuilder.create().setId(this.attachmentId).build();
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (iAttachment == null) return originalComponents;

        AttachmentType attachmentType = iAttachment.getType(attachmentItem);
        ResourceLocation gunId = iGun.getGunId(gunItem);

        HashMap<String, String> attr = new HashMap<>();
        HashMap<String, Double> originAttr = new HashMap<>();
        HashMap<String, Double> newAttr = new HashMap<>();
        HashMap<String, Double> defaultAttr = new HashMap<>();

        try {
            TimelessAPI.getCommonGunIndex(gunId).ifPresent(i -> {
                GunData gunData = i.getGunData();

                ItemStack attachmentTypeItem = iGun.getAttachment(gunItem, attachmentType);
                if (!attachmentTypeItem.isEmpty()) {
                    iGun.unloadAttachment(gunItem, attachmentType);
                }

                AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty();
                cacheProperty.eval(gunItem, gunData);

                AttachmentPropertyManager.getModifiers().forEach((key, modifier) ->
                        modifier.getPropertyDiagramsData(gunItem, gunData, cacheProperty)
                                .forEach(diagramsData -> originAttr.putAll(handleData(diagramsData))));

                iGun.installAttachment(gunItem, attachmentItem);

                cacheProperty.eval(gunItem, gunData);

                AttachmentPropertyManager.getModifiers().forEach((key, modifier) ->
                        modifier.getPropertyDiagramsData(gunItem, gunData, cacheProperty).forEach(diagramsData -> {
                            Optional<String> propertyKey = taczaddon$getPropertyKey(diagramsData.titleKey());
                            OptionalDouble defaultValue = extractValue(diagramsData.defaultString());
                            if (propertyKey.isPresent() && defaultValue.isPresent()) {
                                defaultAttr.put(propertyKey.get(), defaultValue.getAsDouble());
                            }
                            newAttr.putAll(handleData(diagramsData));
                        }));
            });
        } catch (RuntimeException ignored) {
            return originalComponents;
        }

        newAttr.forEach((titleKey, newVal) -> {
            if (!originAttr.containsKey(titleKey)) {
                return;
            }

            double offset = newVal - originAttr.get(titleKey);
            String remark = "";
            if (offset > 0) remark += "+";
            remark += (double) (Math.round(offset * 100d) / 100d);
            if (titleKey.equals("weight")) remark += "kg";
            if (titleKey.equals("ads") || titleKey.contains("time")) remark += "s";
            if (titleKey.equals("aim_inaccuracy") || titleKey.equals("armor_ignore")) remark += "%";
            if (titleKey.equals("rpm")) remark += "rpm";
            if (titleKey.equals("effective_range")) remark += "m";
            if (titleKey.contains("ammo_speed")) remark += "m/s";

            Double defaultValue = defaultAttr.get(titleKey);
            if (defaultValue != null && defaultValue != 0.0D && Double.isFinite(defaultValue)) {
                remark += " (" + (offset > 0 ? "+" : "") + Math.ceil(offset / defaultValue * 100) + "%)";
            }
            attr.put(titleKey, remark);
        });

        List<Component> result = new ArrayList<>();
        originalComponents.forEach(component -> {
            Optional<String> titleKeyOptional = taczaddon$getTooltipPropertyKey(getTranslationKey(component));
            if (titleKeyOptional.isEmpty()) {
                result.add(component);
                return;
            }

            String titleKey = titleKeyOptional.get();
            if (titleKey.equals("inaccuracy")) {
                titleKey = "hipfire_inaccuracy";
            }

            String remark = attr.get(titleKey);
            if (remark == null) {
                result.add(component);
                return;
            }

            String title = titleKey.equals("hipfire_inaccuracy")
                    ? Component.translatable("gui.tacz.gun_refit.property_diagrams.hipfire_inaccuracy").getString()
                    : component.getString().replace("+ ", "").replace("- ", "");
            result.add(Component.literal(title + " " + remark).withStyle(component.getStyle()));
        });
        return result;
    }

    @Unique
    private HashMap<String, Double> handleData(IAttachmentModifier.DiagramsData diagramsData) {
        HashMap<String, Double> result = new HashMap<>();
        if (diagramsData == null) {
            return result;
        }

        Optional<String> propertyKey = taczaddon$getPropertyKey(diagramsData.titleKey());
        if (propertyKey.isEmpty()) {
            return result;
        }

        String positivelyString = diagramsData.positivelyString();
        String negativeString = diagramsData.negativeString();
        String text = positivelyString;
        String[] positiveParts = positivelyString == null ? new String[0] : positivelyString.split(" ");
        if (positiveParts.length > 1 && positiveParts[1].contains("+-")) {
            text = negativeString;
        }
        if (text == null || text.isBlank()) {
            text = negativeString == null || negativeString.isBlank() ? positivelyString : negativeString;
        }

        OptionalDouble value = extractValue(text);
        value.ifPresent(v -> result.put(propertyKey.get(), v));
        return result;
    }

    @Unique
    private OptionalDouble extractValue(String text) {
        if (text == null || text.isBlank()) {
            return OptionalDouble.empty();
        }

        String pattern = "[-+]?\\d+(?:\\.\\d+)?";
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        OptionalDouble value = OptionalDouble.empty();
        while (matcher.find()) {
            try {
                value = OptionalDouble.of(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
                return OptionalDouble.empty();
            }
        }
        return value;
    }

    @Unique
    private static Optional<String> taczaddon$getPropertyKey(String titleKey) {
        // Custom gun packs sometimes shorten or replace TaCZ's modifier keys.
        // Fall back to the last segment so malformed keys keep vanilla tooltip text.
        if (titleKey == null || titleKey.isBlank()) {
            return Optional.empty();
        }

        String[] parts = titleKey.split("\\.");
        if (parts.length >= 5) {
            return Optional.of(parts[4]);
        }

        if (parts.length > 0) {
            return Optional.of(parts[parts.length - 1]);
        }

        return Optional.empty();
    }

    @Unique
    private static Optional<String> taczaddon$getTooltipPropertyKey(String titleKey) {
        if (titleKey == null || titleKey.isBlank()) {
            return Optional.empty();
        }

        String[] parts = titleKey.split("\\.");
        if (parts.length >= 4) {
            return Optional.of(parts[3]);
        }

        if (parts.length > 0) {
            return Optional.of(parts[parts.length - 1]);
        }

        return Optional.empty();
    }

    @Unique
    private static String getTranslationKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }
}
