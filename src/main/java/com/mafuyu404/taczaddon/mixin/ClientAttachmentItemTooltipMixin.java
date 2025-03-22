package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.init.Config;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ClientAttachmentItemTooltip.class, remap = false)
public class ClientAttachmentItemTooltipMixin {
    @Inject(method = "getAllAllowGuns", at = @At("RETURN"), cancellable = true)
    private static void modifyShowAllowGun(List<ItemStack> output, ResourceLocation attachmentId, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> result = new ArrayList<>();
        int amount = Config.getAllowGunAmount();
        for (int i = 0; i < Math.min(amount, output.size()); i++) {
            result.add(output.get(i).copy());
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "lambda$addText$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/index/ClientAttachmentIndex;getTooltipKey()Ljava/lang/String;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void modifyAttachmentDetail(AttachmentType type, ClientAttachmentIndex index, CallbackInfo ci, AttachmentData data) {
//        data.getModifier().forEach((s, jsonProperty) -> {
//            jsonProperty.getComponents().forEach(component -> {
//                System.out.print("\n");
//                System.out.print(component.getString() + "**********");
//                double multiplier = ((Modifier) jsonProperty.getValue()).getMultiplier();
//                double addend = ((Modifier) jsonProperty.getValue()).getAddend();
//                if (multiplier != 1) {
//                    System.out.print(multiplier);
//                }
//                if (addend != 0) {
//                    System.out.print(addend);
//                }
//            });
//            Modifier modifier = (Modifier) jsonProperty.getValue();
//        });
//        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(Minecraft.getInstance().player).getCacheProperty();
//        System.out.print(cacheProperty.getCache("head_shot").toString());
    }
}
