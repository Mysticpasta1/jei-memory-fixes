package com.mystic.jmf.mixin.fixes;

import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.library.recipes.PluginManager;
import mezz.jei.library.recipes.collect.RecipeTypeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

/**
 * Fixes the de-duplication in {@code PluginManager.getRecipes}.
 * <p>
 * JEI de-duplicates the recipe stream with {@link Stream#distinct()}, which has two problems:
 * <ul>
 *   <li><b>It is expensive.</b> {@code distinct()} uses {@code equals}/{@code hashCode}. Recipe types are
 *       commonly records whose {@code equals} compares {@code ItemStack}s, and {@code ItemStack.equals}
 *       compares NBT — so de-duplicating a large recipe type is a deep-equality walk over every recipe,
 *       on every lookup.</li>
 *   <li><b>It is inconsistent.</b> {@link RecipeTypeData} keys {@code focusLinks} and {@code hiddenRecipes}
 *       by reference identity, so JEI elsewhere treats two distinct-but-equal recipes as two recipes, while
 *       {@code distinct()} collapses them. That is a way for a hidden recipe to still appear.</li>
 * </ul>
 * The pass is also skipped entirely when there is nothing to de-duplicate: with only the internal plugin
 * registered, which is the normal case, a single source cannot produce a duplicate.
 */
@Mixin(value = PluginManager.class, remap = false)
public abstract class PluginManagerMixin {

    @Shadow(remap = false)
    private List<IRecipeManagerPlugin> plugins;

    @Redirect(
        method = "getRecipes(Lmezz/jei/library/recipes/collect/RecipeTypeData;Lmezz/jei/api/recipe/IFocusGroup;Z)Ljava/util/stream/Stream;",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;distinct()Ljava/util/stream/Stream;"),
        remap = false
    )
    private Stream<Object> jmf$identityDistinct(Stream<Object> stream) {
        return IdentityDistinct.of(stream, plugins.size());
    }
}