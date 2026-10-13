package com.mystic.jmf.mixin.fixes;

import com.mojang.logging.LogUtils;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IIngredientFilterConfig;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.ingredients.IngredientFilter;
import mezz.jei.gui.ingredients.IngredientListElementFactory;
import mezz.jei.gui.search.ElementPrefixParser;
import mezz.jei.gui.search.IElementSearch;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Fixes {@code IngredientFilter.onIngredientsRemoved}, which is an explicit no-op:
 * <pre>{@code
 * @Override
 * public <V> void onIngredientsRemoved(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
 *     // ignore this, it's handled by onIngredientVisibilityChanged
 * }
 * }</pre>
 * <p>
 * That comment is wrong. {@code IngredientManager.removeIngredientsAtRuntime} removes the ingredients from
 * its own {@code IngredientInfo} index and then fires this event, but nothing removes them from
 * {@code elementSearch}. Ingredient visibility events only fire when something explicitly <em>hides</em> an
 * ingredient, not when it stops existing.
 * <p>
 * The result is a permanent desync: the ingredient list keeps rendering items that are no longer registered,
 * and no event repairs it short of a resource reload.
 * <p>
 * {@link IElementSearch} exposes no removal operation, so the only correct repair is to rebuild the search
 * from the ingredient manager, which is the authoritative source. This mirrors what the constructor does.
 */
@Mixin(value = IngredientFilter.class, remap = false)
public abstract class IngredientFilterRemovalMixin {

    @Unique
    private static final Logger jmf$logger = LogUtils.getLogger();

    @Shadow(remap = false)
    @Final
    private IClientConfig clientConfig;

    @Shadow(remap = false)
    @Final
    private IIngredientFilterConfig config;

    @Shadow(remap = false)
    @Final
    private IIngredientManager ingredientManager;

    @Shadow(remap = false)
    private Comparator<IListElement<?>> ingredientComparator;

    @Shadow(remap = false)
    @Final
    private IModIdHelper modIdHelper;

    @Shadow(remap = false)
    @Final
    private Function<List<IListElementInfo<?>>, Comparator<IListElement<?>>> sortIndexUpdater;

    @Shadow(remap = false)
    @Final
    private ElementPrefixParser elementPrefixParser;

    @Shadow(remap = false)
    private IElementSearch elementSearch;

    @Shadow(remap = false)
    private boolean searchIndexDirty;

    @Shadow(remap = false)
    private boolean sortIndexesDirty;

    @Shadow(remap = false)
    private static IElementSearch createElementSearch(
        IClientConfig clientConfig,
        ElementPrefixParser elementPrefixParser,
        List<IListElementInfo<?>> elementInfos,
        IIngredientManager ingredientManager
    ) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public abstract void invalidateCache();

    @Shadow(remap = false)
    private <V> boolean updateHiddenState(IListElement<V> element) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private void notifyListenersOfChange() {
        throw new AssertionError();
    }

    @Unique
    private boolean jmf$rebuilding;

    @Inject(
        method = "onIngredientsRemoved(Lmezz/jei/api/ingredients/IIngredientHelper;Ljava/util/Collection;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void jmf$removeFromSearch(
        IIngredientHelper<?> ingredientHelper,
        Collection<? extends ITypedIngredient<?>> ingredients,
        CallbackInfo ci
    ) {
        if (this.jmf$rebuilding || ingredients.isEmpty()) {
            return;
        }

        this.jmf$rebuilding = true;
        try {
            List<IListElementInfo<?>> rebuilt =
                IngredientListElementFactory.createBaseList(this.ingredientManager, this.config, this.modIdHelper);

            this.elementSearch = createElementSearch(
                this.clientConfig,
                this.elementPrefixParser,
                rebuilt,
                this.ingredientManager
            );
            this.ingredientComparator = this.sortIndexUpdater.apply(rebuilt);

            for (IListElementInfo<?> info : rebuilt) {
                updateHiddenState(info.getElement());
            }

            invalidateCache();
            this.searchIndexDirty = false;
            this.sortIndexesDirty = false;
            notifyListenersOfChange();

            jmf$logger.info("Ingredient search rebuilt after {} ingredient(s) were removed", ingredients.size());
        } catch (RuntimeException | LinkageError e) {
            jmf$logger.error("Failed to rebuild the ingredient search after a runtime ingredient removal", e);
        } finally {
            this.jmf$rebuilding = false;
        }
    }
}