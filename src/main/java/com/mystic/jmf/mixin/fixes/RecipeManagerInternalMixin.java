package com.mystic.jmf.mixin.fixes;

import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import mezz.jei.library.recipes.RecipeManagerInternal;
import mezz.jei.library.recipes.collect.RecipeTypeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fixes an unbounded recompute storm in {@code RecipeManagerInternal.getFocusLinks}.
 * <p>
 * JEI only caches focus links for recipes that went through {@code addRecipes}, which builds the links once
 * and stores them. Anything else — notably recipes surfaced by a mod's advanced
 * {@code IRecipeManagerPlugin} — has no entry, so this method falls through to rebuilding the recipe's entire
 * layout on every single call:
 * <pre>{@code
 * RecipeIngredientSupplier supplier = IngredientSupplierHelper.getIngredientSupplier(recipe, category, ingredientManager);
 * return supplier.getFocusLinks();
 * }</pre>
 * That constructs a full {@code RecipeLayoutBuilder}, re-invokes the category's {@code setRecipe}, and
 * resolves every ingredient in it. {@code getRecipesStream} calls {@code isRecipeVisible} for every recipe in
 * the stream, which calls this. So a single recipe query re-builds every uncached recipe's layout, and the
 * next query does it again.
 * <p>
 * The fix stores the recomputed links back into {@code RecipeTypeData}, so the fallback runs at most once per
 * recipe. The head injection short-circuits JEI's own map lookup, and the tail injection memoises whatever
 * the recompute produced.
 */
@Mixin(value = RecipeManagerInternal.class, remap = false)
public abstract class RecipeManagerInternalMixin {

    @Inject(
        method = "getFocusLinks(Lmezz/jei/library/recipes/collect/RecipeTypeData;Ljava/lang/Object;)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private <T> void jmf$useCachedFocusLinks(
        RecipeTypeData<T> recipeTypeData,
        T recipe,
        CallbackInfoReturnable<List<FocusLink>> cir
    ) {
        List<FocusLink> cached = recipeTypeData.getFocusLinks(recipe);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(
        method = "getFocusLinks(Lmezz/jei/library/recipes/collect/RecipeTypeData;Ljava/lang/Object;)Ljava/util/List;",
        at = @At("TAIL"),
        remap = false
    )
    private <T> void jmf$memoizeFocusLinks(
        RecipeTypeData<T> recipeTypeData,
        T recipe,
        CallbackInfoReturnable<List<FocusLink>> cir
    ) {
        if (recipeTypeData instanceof RecipeTypeDataFocusLinkAccess access) {
            access.jmf$putFocusLinks(recipe, cir.getReturnValue());
        }
    }
}