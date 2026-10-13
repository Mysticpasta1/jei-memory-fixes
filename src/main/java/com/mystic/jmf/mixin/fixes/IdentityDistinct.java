package com.mystic.jmf.mixin.fixes;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Shared helper for identity-based de-duplication of recipe streams.
 * <p>
 * JEI de-duplicates recipe streams with {@link Stream#distinct()}, which has two problems:
 * <ul>
 *   <li>It uses {@code equals}/{@code hashCode}. Most recipe types are records whose {@code equals}
 *       compares {@code ItemStack}s, which compares NBT — so the de-duplication pass is a deep-equality walk
 *       over the entire recipe set, on every lookup.</li>
 *   <li>Its semantics disagree with the rest of JEI's recipe bookkeeping. {@code RecipeTypeData} keys
 *       {@code focusLinks} and {@code hiddenRecipes} by <em>identity</em> (both are {@code IdentityHashMap}s),
 *       so two distinct-but-equal recipe instances are tracked separately by JEI while {@code distinct()}
 *       collapses them. That can surface a recipe the user has hidden.</li>
 * </ul>
 * De-duplicating by identity fixes both the cost and the inconsistency, and it can be done lazily with a
 * stateful predicate so no intermediate set is built up front.
 */
public final class IdentityDistinct {

    private IdentityDistinct() {
    }

    /**
     * Lazily drops elements already seen, comparing by reference identity.
     */
    public static <T> Stream<T> of(Stream<T> stream) {
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return stream.filter(seen::add);
    }

    /**
     * Same as {@link #of(Stream)}, but skips the work entirely when a single source cannot contain
     * duplicates. Callers know how many sources contributed; this is the common case.
     */
    public static <T> Stream<T> of(Stream<T> stream, int sourceCount) {
        if (sourceCount <= 1) {
            return stream;
        }
        return of(stream);
    }
}