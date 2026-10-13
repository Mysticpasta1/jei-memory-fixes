package com.mystic.jmf.mixin.fixes;

import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import mezz.jei.library.recipes.collect.RecipeTypeData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;

/**
 * Exposes {@code RecipeTypeData}'s focus link cache so that results computed on demand can be stored back
 * into it. See {@link RecipeManagerInternalMixin} for why that matters.
 */
@Mixin(value = RecipeTypeData.class, remap = false)
public abstract class RecipeTypeDataMixin implements RecipeTypeDataFocusLinkAccess {

    @Shadow(remap = false)
    @Final
    private Map<Object, List<FocusLink>> focusLinks;

    @Unique
    public void jmf$putFocusLinks(Object recipe, List<FocusLink> links) {
        if (recipe == null || links == null) {
            return;
        }
        this.focusLinks.put(recipe, links);
    }
}