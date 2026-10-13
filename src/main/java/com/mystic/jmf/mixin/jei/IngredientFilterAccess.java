package com.mystic.jmf.mixin.jei;

/**
 * Bridge to {@code IngredientFilter} so the cached element list can be released from outside the mixin.
 */
public interface IngredientFilterAccess {

    void invalidateCache();
}