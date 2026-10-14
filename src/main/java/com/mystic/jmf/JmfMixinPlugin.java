package com.mystic.jmf;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Every mod-targeted mixin in this config reaches into another mod, so each one must be skipped when its
 * target is not on the classpath. The check is a classpath probe, because this plugin runs long before the
 * mod list exists.
 * <p>
 * This plugin also honours a {@code -Djmf.mixins=...} system property that restricts the mixins allowed to
 * apply, as a comma-separated list of mixin simple class names ({@code none} or {@code all} are also
 * accepted). The property is absent by default, which applies every mixin as normal.
 * <p>
 * This exists because a pack can load {@code IngredientFilter} before this config is registered, and Mixin
 * then refuses the whole target class with no way to influence the ordering from inside the jar. When a
 * change to this mod coincides with a pack that will not finish loading, the property turns "which change"
 * from a guess into a bisect that does not need a rebuild per step: one run with {@code -Djmf.mixins=none}
 * isolates the mixins from the mod's own reload listener, and each further run halves the remaining set.
 */
public class JmfMixinPlugin implements IMixinConfigPlugin {

    private static final String MIXIN_FILTER_PROPERTY = "jmf.mixins";

    private Set<String> allowedMixins;

    @Override
    public void onLoad(String mixinPackage) {
        this.allowedMixins = parseFilter(System.getProperty(MIXIN_FILTER_PROPERTY));
    }

    private static Set<String> parseFilter(String property) {
        if (property == null || property.isBlank()) {
            return null;
        }
        Set<String> allowed = new LinkedHashSet<>();
        String[] names = property.split(",");
        for (String name : names) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                allowed.add(trimmed);
            }
        }
        return allowed;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!isEnabledByProperty(mixinClassName)) {
            return false;
        }
        if (!isExternalTarget(targetClassName)) {
            return true;
        }
        try {
            Class.forName(targetClassName, false, JmfMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private boolean isEnabledByProperty(String mixinClassName) {
        if (allowedMixins == null) {
            return true;
        }
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return allowedMixins.contains("all") || allowedMixins.contains(simpleName);
    }

    private static boolean isExternalTarget(String targetClassName) {
        return targetClassName.startsWith("mezz.jei.")
            || targetClassName.startsWith("com.tonywww.jeioptimize.");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
