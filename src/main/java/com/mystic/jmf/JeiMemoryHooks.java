package com.mystic.jmf;

import com.mystic.jmf.mixin.jei.IngredientFilterAccess;
import net.minecraftforge.fml.ModList;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static bridge between the mod and its mixins.
 * <p>
 * Mixin classes should not expose static members, so anything the mod needs to call into a mixin lands here.
 */
public final class JeiMemoryHooks {

    private static final String JET_MOD_ID = "justenoughthreads";

    private static volatile IngredientFilterAccess ingredientFilter;
    private static final AtomicInteger RESOURCE_RELOAD_GENERATION = new AtomicInteger();
    private static volatile boolean jetLoaded;

    private JeiMemoryHooks() {
    }

    public static void bindIngredientFilter(IngredientFilterAccess filter) {
        ingredientFilter = filter;
    }

    public static void onClientResourceReload() {
        RESOURCE_RELOAD_GENERATION.incrementAndGet();
    }

    public static int getResourceReloadGeneration() {
        return RESOURCE_RELOAD_GENERATION.get();
    }

    /**
     * Whether Just Enough Threads is present.
     * <p>
     * JET ships its own mixins into the same JEI internals this mod targets. Where both do the same job, JET's
     * implementation is used and ours stands down, so the two never apply the same truncation twice.
     * <p>
     * Cached because it is queried from recipe-generation hot paths and cannot change once mods are loaded.
     */
    public static boolean isJetLoaded() {
        if (!jetLoaded) {
            ModList modList = ModList.get();
            jetLoaded = modList != null && modList.isLoaded(JET_MOD_ID);
        }
        return jetLoaded;
    }

    /**
     * {@code IngredientFilter} keeps one wrapper object per visible ingredient alive in
     * {@code ingredientListCached} for as long as the filter exists, which is the rest of the session. On a
     * large pack that is hundreds of thousands of objects retained even with every GUI closed. JEI rebuilds
     * the list on demand, so releasing it is safe.
     */
    public static void releaseCachedElementList() {
        if (!Config.releaseElementList) {
            return;
        }
        IngredientFilterAccess filter = ingredientFilter;
        if (filter != null) {
            filter.invalidateCache();
        }
    }
}