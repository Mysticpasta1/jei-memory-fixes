package com.mystic.jmf.mixin.fixes;

import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.library.ingredients.RegisteredIngredients;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Two fixes for {@link RegisteredIngredients}.
 *
 * <h2>1. Duplicate ingredient classes crash startup</h2>
 * The constructor builds its lookup maps with {@link Collectors#toMap(Function, Function)}, whose two-argument
 * form uses a throwing merge function. If two {@link IIngredientType}s declare the same
 * {@code getIngredientClass()} — entirely possible once a few mods ship JEI plugins — this throws
 * {@code IllegalStateException: Duplicate key} while JEI is starting, with no recovery.
 * <p>
 * The class already contains a fallback linear scan in {@code getIngredientType(Class)} for exactly the case
 * where the exact-class map has no hit, so duplicates were clearly anticipated; construction just crashes
 * before that path can ever be reached. Keeping the first registration matches the rule
 * {@code getIngredientType} already applies when it caches a fallback answer.
 *
 * <h2>2. Subclass resolution depends on plugin registration order</h2>
 * {@code getIngredientType(Class)} scans the registered types and returns the <em>first</em> whose class is
 * assignable from the queried class, then caches that answer permanently. For a subclass that means whichever
 * type happened to be registered first wins, even when another registered type is strictly more specific, so
 * which type an ingredient resolves to varies with mod load order. This picks the most specific candidate.
 */
@Mixin(value = RegisteredIngredients.class, remap = false)
public abstract class RegisteredIngredientsMixin {

    @Shadow(remap = false)
    @Final
    private List<IIngredientType<?>> orderedTypes;

    @Shadow(remap = false)
    @Final
    private Map<Class<?>, IIngredientType<?>> classToType;

    /**
     * The first {@code Collectors.toMap} in the constructor builds the ingredient class lookup.
     */
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Collectors;toMap(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/stream/Collector;",
            ordinal = 0
        ),
        remap = false
    )
    private Collector<?, ?, Map<Object, Object>> jmf$tolerateDuplicateIngredientClasses(
        Function<?, Object> keyMapper,
        Function<?, Object> valueMapper
    ) {
        return RegisteredIngredientsCollector.firstWins(keyMapper, valueMapper, "ingredient class");
    }

    /**
     * The second {@code Collectors.toMap} in the constructor builds the base class lookup.
     */
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Collectors;toMap(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/stream/Collector;",
            ordinal = 1
        ),
        remap = false
    )
    private Collector<?, ?, Map<Object, Object>> jmf$tolerateDuplicateBaseClasses(
        Function<?, Object> keyMapper,
        Function<?, Object> valueMapper
    ) {
        return RegisteredIngredientsCollector.firstWins(keyMapper, valueMapper, "ingredient base class");
    }

    /**
     * Resolves a subclass to the most specific registered type instead of the first one found.
     */
    @Inject(
        method = "getIngredientType(Ljava/lang/Class;)Lmezz/jei/api/ingredients/IIngredientType;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    @SuppressWarnings("unchecked")
    private <V> void jmf$resolveMostSpecificType(
        Class<? extends V> ingredientClass,
        CallbackInfoReturnable<IIngredientType<V>> cir
    ) {
        IIngredientType<?> exact = this.classToType.get(ingredientClass);
        if (exact != null) {
            cir.setReturnValue((IIngredientType<V>) exact);
            return;
        }

        IIngredientType<?> best = null;
        for (IIngredientType<?> candidate : this.orderedTypes) {
            Class<?> candidateClass = candidate.getIngredientClass();
            if (!candidateClass.isAssignableFrom(ingredientClass)) {
                continue;
            }
            if (best == null || best.getIngredientClass().isAssignableFrom(candidateClass)) {
                best = candidate;
            }
        }

        if (best == null) {
            return;
        }
        this.classToType.put(ingredientClass, best);
        cir.setReturnValue((IIngredientType<V>) best);
    }
}