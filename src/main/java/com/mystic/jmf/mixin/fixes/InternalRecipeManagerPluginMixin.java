package com.mystic.jmf.mixin.fixes;

import mezz.jei.library.recipes.InternalRecipeManagerPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.stream.Stream;

/**
 * Fixes the catalyst branch of {@code InternalRecipeManagerPlugin.getRecipes(category, focus)}.
 * <p>
 * When the focused ingredient is a catalyst for the category, JEI concatenates the matching recipes with
 * <em>every recipe in the category</em> and then calls {@link Stream#distinct()}. Almost every furnace, bench
 * and chest is a catalyst for its category, so an ordinary "show recipes for Iron Ingot" runs this path.
 * <p>
 * {@code distinct()} buffers the whole concatenated stream into a {@code LinkedHashSet} and hashes every
 * recipe through {@code equals}, which for record recipe types means comparing {@code ItemStack}s and their
 * NBT. Replacing it with a lazy identity filter removes the intermediate set entirely, avoids the deep
 * equality work, and makes the de-duplication consistent with the identity-based {@code focusLinks} and
 * {@code hiddenRecipes} maps that the rest of JEI uses.
 */
@Mixin(value = InternalRecipeManagerPlugin.class, remap = false)
public abstract class InternalRecipeManagerPluginMixin {

    @Redirect(
        method = "getRecipes(Lmezz/jei/api/recipe/category/IRecipeCategory;Lmezz/jei/api/recipe/IFocus;)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;distinct()Ljava/util/stream/Stream;"),
        remap = false
    )
    private Stream<Object> jmf$identityDistinct(Stream<Object> stream) {
        return IdentityDistinct.of(stream);
    }
}