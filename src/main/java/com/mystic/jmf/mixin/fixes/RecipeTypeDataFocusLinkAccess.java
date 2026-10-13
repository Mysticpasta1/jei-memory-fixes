package com.mystic.jmf.mixin.fixes;

import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;

import java.util.List;

/**
 * Lets {@code RecipeManagerInternal} memoise focus links back into {@code RecipeTypeData}.
 */
public interface RecipeTypeDataFocusLinkAccess {

    void jmf$putFocusLinks(Object recipe, List<FocusLink> links);
}