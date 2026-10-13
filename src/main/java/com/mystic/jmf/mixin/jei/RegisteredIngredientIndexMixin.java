package com.mystic.jmf.mixin.jei;

import com.mystic.jmf.Config;
import mezz.jei.api.ingredients.ITypedIngredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shrinks JEI's per-group ingredient lists.
 * <p>
 * {@code RegisteredIngredientIndex} keeps every registered ingredient twice: once in a uid map and once in
 * a "grouping uid" list. Those lists are allocated with {@code new ArrayList<>()}, i.e. room for 10 entries,
 * but the overwhelming majority of groups hold exactly one ingredient because most items have no subtypes.
 * On a large pack that is tens of megabytes of empty array slots, and it grows linearly with mod count.
 * <p>
 * {@code trimToSize} is allocation free when the list is already right-sized, so this is safe to run per add.
 */
@Mixin(targets = "mezz.jei.library.ingredients.RegisteredIngredientIndex", remap = false)
public abstract class RegisteredIngredientIndexMixin {

    @Shadow(remap = false)
    @Final
    private Map<Object, List<ITypedIngredient<?>>> ingredientsByGroupingUid;

    @Inject(method = "addToGroup", at = @At("TAIL"), remap = false)
    private void jmf$trimGroupList(Object groupingUid, ITypedIngredient<?> ingredient, CallbackInfo ci) {
        if (!Config.trimGroupingLists || groupingUid == null) {
            return;
        }
        List<ITypedIngredient<?>> group = ingredientsByGroupingUid.get(groupingUid);
        if (group instanceof ArrayList<?> arrayList) {
            arrayList.trimToSize();
        }
    }
}