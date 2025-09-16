package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(value = ClientAttachmentItemTooltip.class, remap = false)
public class ClientAttachmentItemTooltipMixin {
    @Shadow @Final private ResourceLocation attachmentId;

    @Shadow @Final private List<Component> components;

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
        if (!Config.SHOW_ATTACHMENT_ATTRIBUTE.get()) return value.getComponents();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return value.getComponents();
        }
        ItemStack gunItem = player.getMainHandItem().copy();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return value.getComponents();
        }
        boolean allowAttachment = AllowAttachmentTagMatcher.match(iGun.getGunId(gunItem), this.attachmentId);
        if (!allowAttachment) return value.getComponents();
        ItemStack attachmentItem = AttachmentItemBuilder.create().setId(this.attachmentId).build();
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (iAttachment == null) return value.getComponents();
        AttachmentType attachmentType = iAttachment.getType(attachmentItem);

        List<Component> result = new ArrayList<>();
        ResourceLocation gunId = iGun.getGunId(gunItem);
        HashMap<String, String> attr = new HashMap<>();
        HashMap<String, Double> originAttr = new HashMap<>();
        HashMap<String, Double> newAttr = new HashMap<>();
        HashMap<String, Double> defaultAttr = new HashMap<>();

        TimelessAPI.getCommonGunIndex(gunId).ifPresent(i -> {
            GunData gunData = i.getGunData();

            ItemStack attachmentTypeItem = iGun.getAttachment(gunItem, attachmentType);
            if (!attachmentTypeItem.isEmpty()) iGun.unloadAttachment(gunItem, attachmentType);

            AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty();
            cacheProperty.eval(gunItem, gunData);

            AttachmentPropertyManager.getModifiers().forEach((key, Modifier) -> Modifier.getPropertyDiagramsData(gunItem, gunData, cacheProperty).forEach(diagramsData -> originAttr.putAll(handleData(diagramsData))));

            iGun.installAttachment(gunItem, attachmentItem);

            cacheProperty.eval(gunItem, gunData);

            AttachmentPropertyManager.getModifiers().forEach((key, Modifier) -> Modifier.getPropertyDiagramsData(gunItem, gunData, cacheProperty).forEach(diagramsData -> {
                defaultAttr.put(diagramsData.titleKey().split("\\.")[4], extractValue(diagramsData.defaultString()));
                newAttr.putAll(handleData(diagramsData));
            }));
        });
        newAttr.forEach((titleKey, newVal) -> {
            if (originAttr.containsKey(titleKey)) {
                double originVal = originAttr.get(titleKey);
                double offset = newVal - originVal;
                double defaultValue = defaultAttr.get(titleKey);
                String remark = "";
                if (offset > 0) remark += "+";
                remark += (double) (Math.round(offset * 100d) / 100d);
                if (titleKey.equals("weight")) remark += "kg";
                if (titleKey.equals("ads") || titleKey.contains("time")) remark += "s";
                if (titleKey.equals("aim_inaccuracy") || titleKey.equals("armor_ignore")) remark += "%";
                if (titleKey.equals("rpm")) remark += "rpm";
                if (titleKey.equals("effective_range")) remark += "m";
                if (titleKey.contains("ammo_speed")) remark += "m/s";
                remark += " (" + (offset > 0 ? "+" : "") + Math.ceil(offset / defaultValue * 100) + "%)";
                attr.put(titleKey, remark);
            }
        });
        value.getComponents().forEach(component -> {
            String translationKey = getTranslationKey(component);
            if (translationKey == null) return;
            String titleKey = translationKey.split("\\.")[3];
            if (titleKey.equals("inaccuracy")) titleKey = "hipfire_inaccuracy";
            String title = titleKey.equals("hipfire_inaccuracy") ? Component.translatable("gui.tacz.gun_refit.property_diagrams.hipfire_inaccuracy").getString() : component.getString().replace("+ ", "").replace("- ", "");
            String remark;
            if (attr.containsKey(titleKey)) remark = attr.get(titleKey);
            else remark = component.getString().split(" ")[0];
            String text = title + " " + remark;
            if ((component.getStyle().getColor().toString().equals("red"))) text = "§c" + text;
            else text = "§a" + text;
            result.add(Component.translatable(text));
        });
        return result;
    }
    private HashMap<String, Double> handleData(IAttachmentModifier.DiagramsData diagramsData) {
        String titleKey = diagramsData.titleKey();
        String positivelyString = diagramsData.positivelyString();
        String negativeString = diagramsData.negativeString();
        String text;
        if (!positivelyString.split(" ")[1].contains("+-")) text = positivelyString;
        else text = negativeString;
        HashMap<String, Double> result = new HashMap<>();
        result.put(titleKey.split("\\.")[4], extractValue(text));
        return result;
    }
    private double extractValue(String text) {
        String pattern = "[-+]?\\d+(?:\\.\\d+)?";
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        double val = 0;
        while (matcher.find()) {
            val = Double.parseDouble(matcher.group());
        }
        return val;
    }
    private static String getTranslationKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }
}
