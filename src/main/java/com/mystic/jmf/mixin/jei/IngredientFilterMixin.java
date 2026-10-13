package com.mystic.jmf.mixin.jei;

import com.mystic.jmf.Config;
import com.mystic.jmf.JeiMemoryHooks;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.ingredients.IngredientFilter;
import mezz.jei.gui.search.ElementPrefixParser;
import mezz.jei.gui.search.ElementSearchLowMem;
import mezz.jei.gui.search.IElementSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Forces JEI onto its low-memory ingredient search backend, and binds the filter so the cached element list
 * can be released.
 * <p>
 * The default {@link mezz.jei.gui.search.ElementSearch} builds a suffix tree over every search string of
 * every registered ingredient. Its size scales with both mod count and the number of distinct tooltip strings
 * those mods contribute, which is the main reason JEI's footprint grows with the pack.
 * {@link ElementSearchLowMem} keeps only the element list and scans strings on demand.
 */
@Mixin(value = IngredientFilter.class, remap = false)
public abstract class IngredientFilterMixin implements IngredientFilterAccess {

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void jmf$bindFilter(CallbackInfo ci) {
        JeiMemoryHooks.bindIngredientFilter(this);
    }

    @Shadow(remap = false)
    public abstract void invalidateCache();

    @Inject(method = "createElementSearch", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jmf$forceLowMemorySearch(
        IClientConfig clientConfig,
        ElementPrefixParser elementPrefixParser,
        List<IListElementInfo<?>> elementInfos,
        IIngredientManager ingredientManager,
        CallbackInfoReturnable<IElementSearch> cir
    ) {
        if (!Config.lowMemorySearch) {
            return;
        }
        cir.setReturnValue(new ElementSearchLowMem(elementPrefixParser.getNoPrefix(), elementInfos));
    }
}
