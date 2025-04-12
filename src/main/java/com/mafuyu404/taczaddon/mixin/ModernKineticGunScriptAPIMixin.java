package com.mafuyu404.taczaddon.mixin;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow private LivingEntity shooter;

//    @Redirect(method = "consumeAmmoFromPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"))
//    private <T> LazyOptional<T> useBackpackAmmo(LivingEntity instance, Capability<T> capability, Direction facing) {
//        if (!(instance instanceof Player player)) return instance.getCapability(capability, facing);
//        ArrayList<ItemStack> backpack = new ArrayList<>();
//        ArrayList<Consumer<Integer>> ChangeEvent = new ArrayList<>();
//        player.getInventory().items.forEach(itemStack -> {
//            if (itemStack.isEmpty()) return;
//            String[] id = itemStack.getItem().getDescriptionId().split("\\.");
//            if (!(id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack"))) return;
//            IBackpackWrapper backpackWrapper = itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(IBackpackWrapper.Noop.INSTANCE);
//            InventoryHandler handler = backpackWrapper.getInventoryHandler();
//            int size = itemStack.getTag().getInt("inventorySlots");
//            for (int i = 0; i < size; i++) {
//                ItemStack item = handler.getStackInSlot(i);
//                backpack.add(item);
//            }
//            UUID uuid = backpackWrapper.getContentsUuid().get();
//            CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
//            ChangeEvent.add(slot -> {
//                SBPPacketHandler.INSTANCE.sendToClient((ServerPlayer) player, new BackpackContentsMessage(uuid, backpackContent));
//            });
//        });
//        backpack.addAll(player.getInventory().items);
//        VirtualInventory virtualInventory = new VirtualInventory(backpack.size(), player);
//        for (int i = 0; i < backpack.size(); i++) {
//            virtualInventory.setItem(i, backpack.get(i));
//        }
//        VirtualInventory.ItemHandler handler = virtualInventory.getHandler();
//        handler.ChangeEvent.addAll(ChangeEvent);
//        return LazyOptional.of(() -> (T) handler);
//    }

    @Redirect(method = "lambda$consumeAmmoFromPlayer$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;findAndExtractInventoryAmmos(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int useBackpackAmmo(AbstractGunItem abstractGunItem, IItemHandler cap, ItemStack gunItem, int neededAmount) {
        if (!(shooter instanceof Player player)) return abstractGunItem.findAndExtractInventoryAmmos(cap, gunItem, neededAmount);
        final int[] cnt = {neededAmount};
        player.getInventory().items.forEach(itemStack -> {
            if (itemStack.isEmpty()) return;
            String[] id = itemStack.getItem().getDescriptionId().split("\\.");
            if (!(id[1].equals("sophisticatedbackpacks") && id[2].contains("backpack"))) return;
            BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
            IBackpackWrapper iBackpackWrapper = itemStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(IBackpackWrapper.Noop.INSTANCE);
            InventoryHandler itemHandler = iBackpackWrapper.getInventoryHandler();
            abstractGunItem.findAndExtractInventoryAmmos(backpackWrapper.getInventoryHandler(), gunItem, cnt[0]);
            int used = abstractGunItem.findAndExtractInventoryAmmos(itemHandler, gunItem, cnt[0]);
            cnt[0] -= used;
            UUID uuid = iBackpackWrapper.getContentsUuid().get();
            CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
            SBPPacketHandler.INSTANCE.sendToClient((ServerPlayer) player, new BackpackContentsMessage(uuid, backpackContent));
        });
        int inventoryAmmoUsed = abstractGunItem.findAndExtractInventoryAmmos(cap, gunItem, cnt[0]);
        cnt[0] -= inventoryAmmoUsed;
        return neededAmount - cnt[0];
    }
}
