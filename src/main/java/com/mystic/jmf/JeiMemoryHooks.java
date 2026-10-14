package com.mystic.jmf;

import com.mojang.logging.LogUtils;
import com.mystic.jmf.mixin.jei.IngredientFilterAccess;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static bridge between the mod and its mixins.
 * <p>
 * Mixin classes should not expose static members, so anything the mod needs to call into a mixin lands here.
 */
public final class JeiMemoryHooks {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String JET_MOD_ID = "justenoughthreads";

    private static volatile IngredientFilterAccess ingredientFilter;
    private static final AtomicInteger RESOURCE_RELOAD_GENERATION = new AtomicInteger();
    private static final AtomicBoolean UNBOUND_WARNING_LOGGED = new AtomicBoolean();
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
     * <p>
     * That last assumption does not hold when JET is installed. JET's {@code deferredIngredientFilter} and
     * {@code asyncIngredientFilter} options replace that list with an index built incrementally across
     * client ticks, and JET publishes it only once the whole index is complete. Invalidating it therefore
     * resets the build rather than freeing it, so the index never completes, the sidebar is never published,
     * and loading never finishes. JET also clears its own build-local caches at publication, so the
     * retention this option targets is already handled on its side.
     */
    public static void releaseCachedElementList() {
        warnIfFilterUnbound();
        if (!Config.releaseElementList || isJetLoaded()) {
            return;
        }
        IngredientFilterAccess filter = ingredientFilter;
        if (filter != null) {
            filter.invalidateCache();
        }
    }

    /**
     * Warns once if the {@code IngredientFilter} mixins never ran.
     * <p>
     * Both of this mod's {@code IngredientFilter} mixins bind themselves from the target's constructor, so an
     * unbound filter means Mixin refused to apply them. That happens when some other mod loads
     * {@code IngredientFilter} during mod construction, before this config is registered, and Mixin reports
     * it as a critical "loaded too early" problem and skips the target entirely. The pack still runs, but the
     * low-memory search backend and the element-list release are silently doing nothing, so it is worth
     * saying so once instead of letting the player assume the mod is working.
     * <p>
     * Checked here because the filter is constructed during JEI plugin registration, long after client setup,
     * so any earlier check would report a false negative.
     */
    private static void warnIfFilterUnbound() {
        if (ingredientFilter == null && UNBOUND_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                "JEI memory fixes are only partially active: the mixins into"
                    + " mezz.jei.gui.ingredients.IngredientFilter were not applied, because another mod loaded"
                    + " that class during mod construction. lowMemorySearch and releaseElementList have no"
                    + " effect in this pack."
            );
        }
    }
}