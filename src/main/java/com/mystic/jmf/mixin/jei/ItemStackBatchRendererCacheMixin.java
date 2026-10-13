package com.mystic.jmf.mixin.jei;

import com.google.common.cache.LoadingCache;
import com.mystic.jmf.Config;
import com.mystic.jmf.JeiMemoryHooks;
import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import mezz.jei.library.render.batch.ItemStackBatchRendererCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Shrinks and self-heals JEI's item batch renderer cache.
 * <p>
 * Two problems with the vanilla cache:
 * <ul>
 *   <li>It keeps up to 6 pre-baked batches alive, and each batch pins a {@code BakedModel} plus the item
 *       stacks it was baked from, for every ingredient on screen. That is a lot of retained memory for
 *       what is only a per-frame optimization.</li>
 *   <li>JEI never invalidates it on resource reload, so after a reload it can hold batches built from
 *       stale models.</li>
 * </ul>
 */
@Mixin(value = ItemStackBatchRendererCache.class, remap = false)
public abstract class ItemStackBatchRendererCacheMixin {

    @Shadow(remap = false)
    @Final
    private LoadingCache<List<BatchRenderElement<ItemStack>>, Object> cache;

    @Unique
    private int jmf$reloadGeneration = -1;

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/cache/CacheBuilder;maximumSize(J)Lcom/google/common/cache/CacheBuilder;"
        ),
        index = 0,
        remap = false
    )
    private long jmf$clampBatchCacheSize(long originalSize) {
        return Math.max(1, Config.maxBatchRendererCache);
    }

    @Inject(method = "renderBatch", at = @At("HEAD"), remap = false)
    private void jmf$invalidateOnReload(
        GuiGraphics guiGraphics,
        ItemStackRenderer itemStackRenderer,
        List<BatchRenderElement<ItemStack>> elements,
        CallbackInfo ci
    ) {
        if (!Config.clearBatchCacheOnReload) {
            return;
        }
        int generation = JeiMemoryHooks.getResourceReloadGeneration();
        if (generation != jmf$reloadGeneration) {
            jmf$reloadGeneration = generation;
            cache.invalidateAll();
        }
    }
}