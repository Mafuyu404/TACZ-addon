package com.mafuyu404.taczaddon.common;

import com.mafuyu404.taczaddon.init.RuleRegistry;
import com.mafuyu404.taczaddon.init.VirtualInventory;
import com.mafuyu404.taczaddon.client.ClientDataStorage;
import com.mafuyu404.taczaddon.init.NetworkHandler;
import com.mafuyu404.taczaddon.network.PrimitivePacket;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class LiberateAttachment {
    public static Inventory useVirtualInventory(Inventory inventory) {
        Object gamerule = ClientDataStorage.get("gamerule.liberateAttachment");
        if ((boolean) gamerule) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<GunSmithTableRecipe> recipeList = recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
            int size = recipeList.size() + 9;
            VirtualInventory virtualInventory = new VirtualInventory(size, inventory.player);
            for (int i = 0; i < size; i++) {
                if (i < 9) virtualInventory.setItem(i, inventory.getSelected());
                else virtualInventory.setItem(i, recipeList.get(i - 9).getOutput());
            }
            return virtualInventory;
        }
        else return AttachmentFromBackpack.useVirtualInventory(inventory);
    }
    public static void onRuleChange(MinecraftServer server, GameRules.BooleanValue value) {
        server.getPlayerList().getPlayers().forEach(player -> {
            NetworkHandler.sendToClient(player, new PrimitivePacket("gamerule.liberateAttachment", value.get()));
        });
    }
    public static void syncRuleWhenLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        boolean value = serverPlayer.level().getGameRules().getBoolean(RuleRegistry.LIBERATE_ATTACHMENT);
        NetworkHandler.sendToClient(serverPlayer, new PrimitivePacket("gamerule.liberateAttachment", value));
    }
}
