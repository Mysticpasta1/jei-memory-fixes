package com.mystic.jmf.mixin.fixes;

import com.mojang.logging.LogUtils;
import mezz.jei.api.runtime.IIngredientManager.IIngredientListener;
import mezz.jei.library.ingredients.IngredientManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Consumer;

/**
 * Isolates ingredient listeners from each other in {@link IngredientManager}.
 * <p>
 * Both runtime mutation methods notify listeners with a bare {@code listeners.forEach(...)}:
 * <pre>{@code
 * ingredientInfo.addIngredients(validTypedIngredients);
 * if (!this.listeners.isEmpty()) {
 *     this.listeners.forEach(listener -> listener.onIngredientsAdded(ingredientHelper, validTypedIngredients));
 * }
 * }</pre>
 * The mutation above has already been applied by the time listeners run, so a listener that throws does two
 * things: it aborts every listener registered after it, and it propagates out of {@code addIngredientsAtRuntime},
 * aborting the caller too. Either way the ingredient manager and the ingredient filter/search index are left
 * permanently out of sync, with no event that would ever repair it short of a resource reload.
 * <p>
 * The same codebase already gets this right for mod plugins — {@code PluginCaller.callOnPlugins} wraps every
 * individual plugin call in its own try/catch. This applies the same rule to listeners.
 */
@Mixin(value = IngredientManager.class, remap = false)
public abstract class IngredientManagerMixin {

    @Unique
    private static final Logger jmf$logger = LogUtils.getLogger();

    @Redirect(
        method = "addIngredientsAtRuntime(Lmezz/jei/api/ingredients/IIngredientType;Ljava/util/Collection;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"),
        remap = false
    )
    private void jmf$notifyAdditionsIsolated(
        List<IIngredientListener> listeners,
        Consumer<IIngredientListener> action
    ) {
        jmf$notifyIsolated(listeners, action, "onIngredientsAdded");
    }

    @Redirect(
        method = "removeIngredientsAtRuntime(Lmezz/jei/api/ingredients/IIngredientType;Ljava/util/Collection;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"),
        remap = false
    )
    private void jmf$notifyRemovalsIsolated(
        List<IIngredientListener> listeners,
        Consumer<IIngredientListener> action
    ) {
        jmf$notifyIsolated(listeners, action, "onIngredientsRemoved");
    }

    @Unique
    private static void jmf$notifyIsolated(
        List<IIngredientListener> listeners,
        Consumer<IIngredientListener> action,
        String callback
    ) {
        for (IIngredientListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (RuntimeException | LinkageError e) {
                jmf$logger.error("Caught an error from JEI ingredient listener {} ({})",
                    listener.getClass(), callback, e);
            }
        }
    }
}