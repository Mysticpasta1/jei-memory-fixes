package com.mystic.jmf.mixin.jei;

import com.mystic.jmf.Config;
import com.mystic.jmf.JeiMemoryHooks;
import mezz.jei.library.plugins.vanilla.anvil.AnvilRecipeMaker;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Caps how many enchanted results JEI keeps per anvil enchanting recipe.
 * <p>
 * For every enchantable item and every enchantment that applies to it, JEI creates one anvil recipe whose
 * output list holds one enchanted result per book level up to the enchantment's max level. Modded
 * enchantments frequently have very high max levels, and each output is a distinct ItemStack with NBT that
 * is then retained for the rest of the session. The number of recipes is itself
 * {@code enchantableItems x applicableEnchantments}, so this grows sharply with mod count.
 * <p>
 * The highest levels are kept, since those are the results players look up. Anvil outputs are never indexed
 * for lookup, only inputs are, so trimming them never makes a recipe undiscoverable. JEI already drops
 * outputs whose anvil result is empty, so the output list is not index-aligned with the book list to begin
 * with and trimming the tail is consistent with that.
 * <p>
 * This is a fallback for players without Just Enough Threads. When JET is present its
 * {@code AnvilEnchantmentRepresentativeMixin} and {@code AnvilRecipeControlMixin} already limit this same
 * generator, and {@link com.mystic.jmf.mixin.jet.JeiOptFeatureFlagsMixin} forces those limits on, so
 * applying this one as well would cap twice with two unrelated numbers. This mixin stands down when JET is
 * loaded.
 */
@Mixin(value = AnvilRecipeMaker.class, remap = false)
public abstract class AnvilRecipeMakerMixin {

    @Inject(
        method = "getEnchantedIngredients(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Ljava/util/List;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void jmf$capAnvilOutputLevels(
        ItemStack ingredient,
        List<ItemStack> enchantedBooks,
        CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        int max = Config.maxAnvilOutputLevels;
        if (max <= 0 || JeiMemoryHooks.isJetLoaded()) {
            return;
        }
        List<ItemStack> outputs = cir.getReturnValue();
        if (outputs.size() <= max) {
            return;
        }
        cir.setReturnValue(List.copyOf(outputs.subList(outputs.size() - max, outputs.size())));
    }
}