package com.mafuyu404.taczaddon.init;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class DataDrivenSmithingRecipe implements SmithingRecipe {
    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;

    public DataDrivenSmithingRecipe(ResourceLocation id, Ingredient template, Ingredient base, Ingredient addition) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return template.test(container.getItem(0)) &&
                base.test(container.getItem(1)) &&
                addition.test(container.getItem(2));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack baseItem = container.getItem(1).copy();
        ItemStack additionItem = container.getItem(2);

        // 获取或创建NBT标签
        CompoundTag nbt = baseItem.getOrCreateTag();
        ListTag itemList;

        if (nbt.contains("CombinedItems", 9)) { // 9是ListTag的ID
            itemList = nbt.getList("CombinedItems", 8); // 8是StringTag的ID
        } else {
            itemList = new ListTag();
        }

        // 添加物品A的ID到列表
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(additionItem.getItem());
        if (!itemList.contains(StringTag.valueOf(itemId.toString()))) itemList.add(StringTag.valueOf(itemId.toString()));

        // 保存回NBT
        nbt.put("CombinedItems", itemList);

        return baseItem;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DATA_DRIVEN_SMITHING.get();
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
        return NonNullList.of(Ingredient.EMPTY, template, base, addition);
    }

    // Getter方法，用于序列化
    public Ingredient getTemplate() {
        return template;
    }

    public Ingredient getBase() {
        return base;
    }

    public Ingredient getAddition() {
        return addition;
    }
}
