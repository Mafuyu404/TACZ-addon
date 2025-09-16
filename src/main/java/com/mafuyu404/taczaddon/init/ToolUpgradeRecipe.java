package com.mafuyu404.taczaddon.init;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class ToolUpgradeRecipe implements SmithingRecipe {
    private final ResourceLocation id;
    private final String nbtKey = "CombinedItems";

    public ToolUpgradeRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return (container.getItem(0).getItem() instanceof SmithingTemplateItem) &&
                isGun(container.getItem(1)) &&
                !container.getItem(2).isEmpty();
    }

    private boolean isGun(ItemStack itemStack) {
        return IGun.getIGunOrNull(itemStack) != null;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack tool = container.getItem(1).copy();
        ItemStack itemA = container.getItem(2);

        CompoundTag nbt = tool.getOrCreateTag();
        ListTag itemList;

        if (nbt.contains(nbtKey, 9)) { // 9是ListTag的ID
            itemList = nbt.getList(nbtKey, 8); // 8是StringTag的ID
        } else {
            itemList = new ListTag();
        }

        // 添加物品A的ID到列表
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemA.getItem());
        if (!itemList.contains(StringTag.valueOf(itemId.toString()))) itemList.add(StringTag.valueOf(itemId.toString()));

        // 保存回NBT
        nbt.put(nbtKey, itemList);

        return tool;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return true; // 接受任何物品
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SMITHING_TRANSFORM;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ItemTags.TRIM_TEMPLATES),
                Ingredient.of(ModernKineticGunItem::new),
                Ingredient.EMPTY); // 任意物品
    }
}
