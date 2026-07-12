package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.BetterGunSmithTable;
import com.mafuyu404.taczaddon.init.DataStorage;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds an attachment-property filter to the gunsmith-table recipe list.
 *
 * <p>This Mixin operates on the recipe collection <em>after</em> TaCZ has
 * already applied namespace filtering, main-hand suitability filtering,
 * name filtering, and the table RecipeFilter. It does not duplicate
 * TaCZ's by-hand filter.</p>
 */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTablePropertyFilterMixin {

    @Unique
    private final ArrayList<String> taczaddon$attachmentProp = new ArrayList<>();

    @Redirect(
            method = "classifyRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
            )
    )
    private boolean taczaddon$filterAttachmentProperty(List<Pair<ResourceLocation, ResourceLocation>> list, Object e) {
        if (!(e instanceof Pair<?, ?> rawPair)
                || !(rawPair.left() instanceof ResourceLocation group)
                || !(rawPair.right() instanceof ResourceLocation recipeId)) {
            return false;
        }

        Pair<ResourceLocation, ResourceLocation> pair = Pair.of(group, recipeId);

        int selectedIndex = getSelectedPropIndex();
        if (selectedIndex <= 0) {
            return list.add(pair);
        }

        String id = recipeId.toString();
        if (!id.contains("/")) {
            return list.add(pair);
        }

        ResourceLocation itemId = ResourceLocation.tryParse(
                id.split(":")[0] + ":" + id.split("/")[1]);
        if (itemId == null) {
            return false;
        }

        if (selectedIndex >= taczaddon$attachmentProp.size()) {
            return list.add(pair);
        }

        String propKey = taczaddon$attachmentProp.get(selectedIndex);
        Component selectedOption = Component.translatable(propKey);
        Object data = DataStorage.get("BetterGunSmithTable.storedAttachmentData");
        if (!(data instanceof Map<?, ?> attachmentData)) {
            return list.add(pair);
        }

        Object storedText = attachmentData.get(itemId.toString());
        if (!(storedText instanceof String text)) return false;
        return text.contains(selectedOption.getString()) && list.add(pair);
    }

    @Unique
    private int getSelectedPropIndex() {
        // Default to 0 (no filter) since the dropdown is disabled by the monolithic mixin removal
        return 0;
    }
}
