package com.mystic.jmf.mixin.fixes;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Builds a duplicate-tolerant replacement for the two-argument {@code Collectors.toMap} that
 * {@code RegisteredIngredients} uses to build its class lookup maps.
 * <p>
 * The two-argument form delegates to a merge function that throws {@code IllegalStateException} on a
 * duplicate key, which turns two JEI plugins registering ingredient types over the same class into a hard
 * startup failure.
 */
final class RegisteredIngredientsCollector {

    private static final Logger jmf$logger = LogUtils.getLogger();

    private RegisteredIngredientsCollector() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Collector<?, ?, Map<Object, Object>> firstWins(
        Function<?, Object> keyMapper,
        Function<?, Object> valueMapper,
        String what
    ) {
        return Collectors.toMap(
            (Function) keyMapper,
            (Function) valueMapper,
            (first, duplicate) -> {
                jmf$logger.warn(
                    "Multiple JEI ingredient types share the same {}. Keeping the first ({}), ignoring {}.",
                    what,
                    first,
                    duplicate
                );
                return first;
            },
            HashMap::new
        );
    }
}