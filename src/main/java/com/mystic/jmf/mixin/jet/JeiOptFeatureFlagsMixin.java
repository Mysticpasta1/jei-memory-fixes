package com.mystic.jmf.mixin.jet;

import com.mystic.jmf.Config;
import com.tonywww.jeioptimize.config.JeiOptFeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pins JET's memory-relevant feature flags instead of re-implementing what JET already does.
 * <p>
 * JET already ships mixins into the same JEI internals this mod targets. Where JET has an equivalent
 * mechanism, it is better to switch that on with JET's own tuned values than to duplicate the work here and
 * risk applying the same truncation twice. This mixin therefore forces on the flags that control JET's
 * already-implemented limits.
 *
 * <h2>Tooltip search index</h2>
 * JET's index is an inverted index holding one {@code BitSet} per distinct search token, each sized by the
 * total ingredient count, so its footprint is {@code tokens x ingredients / 8}. That is precisely the growth
 * this mod exists to stop. Search stays correct; it falls back to JEI's own backend.
 *
 * <h2>Anvil and grindstone representative limits</h2>
 * JEI generates an anvil enchanting recipe per enchantable item and applicable enchantment, and a grindstone
 * disenchant recipe per enchantment and book level whose input list holds <em>every</em> enchantable item the
 * enchantment applies to. Both are {@code items x enchantments} in size and are the largest things JEI
 * allocates on a large pack. JET already implements limits for both
 * ({@code AnvilRecipeControlMixin}, {@code AnvilEnchantmentRepresentativeMixin},
 * {@code GrindstoneRepresentativeMixin}), but they are opt-in, so they are forced on here.
 * <p>
 * The disable switches are deliberately left alone — turning anvil recipes off entirely changes what JEI can
 * show, which is a gameplay decision rather than a memory one.
 */
@Mixin(value = JeiOptFeatureFlags.class, remap = false)
public abstract class JeiOptFeatureFlagsMixin {

    @Inject(method = "tooltipSearchIndex", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void jmf$disableTooltipSearchIndex(CallbackInfoReturnable<Boolean> cir) {
        if (Config.disableJetTooltipIndex) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "optimizeAnvilRepresentatives",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void jmf$enableAnvilRepresentatives(CallbackInfoReturnable<Boolean> cir) {
        if (Config.forceJetRecipeLimits) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "optimizeGrindstoneRepresentatives",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void jmf$enableGrindstoneRepresentatives(CallbackInfoReturnable<Boolean> cir) {
        if (Config.forceJetRecipeLimits) {
            cir.setReturnValue(true);
        }
    }
}