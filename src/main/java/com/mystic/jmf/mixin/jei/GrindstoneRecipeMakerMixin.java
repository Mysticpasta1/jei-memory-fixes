package com.mystic.jmf.mixin.jei;

import com.mystic.jmf.Config;
import com.mystic.jmf.JeiMemoryHooks;
import mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Caps the input list of JEI's grindstone disenchant recipes.
 * <p>
 * During startup JEI builds one disenchant recipe per enchantment and book level, and every one of those
 * recipes holds the complete list of enchantable items that the enchantment applies to, with a fresh copy of
 * each item in both the input and the output list. That is
 * {@code enchantments x levels x enchantableItems} retained ItemStacks, and both factors grow with the mod
 * count. On a few hundred mods it is the single largest allocation JEI makes.
 * <p>
 * The inputs are truncated from the end, which is aligned with the outputs, so the recipe stays internally
 * consistent. Note that these inputs are what JEI indexes for "show recipes", so items past the cap lose
 * their grindstone disenchant entry. Raise {@code maxGrindstoneDisenchantInputs} to 0 if that matters.
 * <p>
 * This is a fallback for players without Just Enough Threads. When JET is present its own
 * {@code GrindstoneRepresentativeMixin} already applies a limit to this same generator, and
 * {@code JeiOptFeatureFlagsMixin} forces that limit on, so applying this one as well would truncate twice
 * with two unrelated numbers. This mixin therefore stands down when JET is loaded.
 */
@Mixin(targets = "mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipeMaker", remap = false)
public abstract class GrindstoneRecipeMakerMixin {

    @Redirect(
        method = "getDisenchantRecipes(Lmezz/jei/common/platform/IPlatformRecipeHelper;Lnet/minecraft/world/inventory/GrindstoneMenu;)Ljava/util/stream/Stream;",
        at = @At(value = "NEW", target = "Lmezz/jei/library/plugins/vanilla/grindstone/GrindstoneRecipe;"),
        remap = false
    )
    private static GrindstoneRecipe jmf$capDisenchantInputs(
        List<ItemStack> topInputs,
        List<ItemStack> bottomInputs,
        List<ItemStack> outputs,
        int minXpReward,
        int maxXpReward,
        ResourceLocation uid
    ) {
        int max = Config.maxGrindstoneDisenchantInputs;
        if (max <= 0 || topInputs.size() <= max || JeiMemoryHooks.isJetLoaded()) {
            return new GrindstoneRecipe(topInputs, bottomInputs, outputs, minXpReward, maxXpReward, uid);
        }

        return new GrindstoneRecipe(
            List.copyOf(topInputs.subList(0, max)),
            List.copyOf(bottomInputs.subList(0, max)),
            List.copyOf(outputs.subList(0, max)),
            minXpReward,
            maxXpReward,
            uid
        );
    }
}