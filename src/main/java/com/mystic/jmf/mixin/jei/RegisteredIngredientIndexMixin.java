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
 * Only the first entry into a group is trimmed, which is where the whole saving comes from: that is the one
 * moment where a ten-slot array holds a single element. Trimming on every later add would be actively harmful,
 * because trimming sizes the array to exactly {@code size}, so the next add has to reallocate and copy again.
 * For a group of {@code n} ingredients that turns registration into {@code O(n^2)} copying plus a fresh array
 * allocation per add, all of it garbage. Groups that keep growing need no help either, since {@code ArrayList}'s
 * own 1.5x growth leaves its slack bounded at a third of its size.
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
        if (group instanceof ArrayList<?> arrayList && arrayList.size() == 1) {
            arrayList.trimToSize();
        }
    }
}