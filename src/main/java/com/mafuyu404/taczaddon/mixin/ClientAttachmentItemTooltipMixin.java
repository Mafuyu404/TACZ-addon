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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

//    @Unique
//    private AttachmentType attachmentType;
//
//    @Inject(method = "addText", at = @At("HEAD"))
//    private void getAttachmentItem(AttachmentType type, CallbackInfo ci) {
//        attachmentType = type;
//    }

    @Redirect(method = "lambda$addText$4", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/JsonProperty;getComponents()Ljava/util/List;"))
    private List<Component> modifyAttachmentDetail(JsonProperty<?> value) {
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
                defaultAttr.put(Component.translatable(diagramsData.titleKey()).getString(), extractValue(diagramsData.defaultString()));
                newAttr.putAll(handleData(diagramsData));
            }));
        });
        newAttr.forEach((title, newVal) -> {
            if (originAttr.containsKey(title)) {
                double originVal = originAttr.get(title);
                double offset = newVal - originVal;
                double defaultValue = defaultAttr.get(title);
                String text = title + " ";
                if (offset > 0) text += "+";
//                text += Math.round(offset / 1e7) * 0.01d;
                text += (double) (Math.round(offset * 100d) / 100d);
                if (Objects.equals(title, "重量")) text += "kg";
                if (title.contains("时间") || title.contains("延迟")) text += "s";
                if (title.contains("瞄准精度") || title.contains("穿甲倍率")) text += "%";
                if (title.contains("射速")) text += "rpm";
                if (title.contains("射程")) text += "m";
                if (title.contains("弹速")) text += "m/s";
                text += " (" + (offset > 0 ? "+" : "") + Math.ceil(offset / defaultValue * 100) + "%)";
                attr.put(title, text);
            }
        });
        value.getComponents().forEach(component -> {
            String title = component.getString().split(" ")[1];
//            System.out.print(title + component.getStyle().getColor().toString() + "\n");
            if (title.equals("腰射精度")) title = "腰射扩散";
            if (title.equals("竖直后座力")) title = "垂直后坐力";
            if (title.equals("水平后座力")) title = "水平后坐力";
            String text;
            if (attr.containsKey(title)) text = attr.get(title);
            else text = title + " " + component.getString().split(" ")[0];
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
        String text = Component.translatable(titleKey).getString();
        if (!positivelyString.split(" ")[1].contains("+-")) text += positivelyString.split(" ")[1];
        else text += negativeString.split(" ")[1];
//        System.out.print(text + "\n");
        HashMap<String, Double> result = new HashMap<>();
        result.put(Component.translatable(titleKey).getString(), extractValue(text));
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
}
