package com.mystic.jmf;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-side knobs for the JEI/JET memory mixins.
 * <p>
 * Every option defaults to the memory-conservative value, because this mod exists to keep JEI's footprint
 * flat as mod count (and therefore ingredient and recipe count) grows.
 */
@Mod.EventBusSubscriber(modid = Jmf.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue LOW_MEMORY_SEARCH = BUILDER
        .comment("Use JEI's low-memory search backend (a flat string scan) instead of its suffix-tree index.",
            "This is the single largest saving: the suffix tree over every search string of every ingredient",
            "grows without bound as mods are added. Searches take longer, but nothing is precomputed.")
        .define("lowMemorySearch", true);

    public static final ForgeConfigSpec.BooleanValue RELEASE_ELEMENT_LIST = BUILDER
        .comment("Drop JEI's cached ingredient list when a GUI is closed.",
            "JEI keeps one wrapper object per visible ingredient alive for the whole session.",
            "Set to false to keep it cached at the cost of that memory.")
        .define("releaseElementList", true);

    public static final ForgeConfigSpec.BooleanValue TRIM_GROUPING_LISTS = BUILDER
        .comment("Shrink JEI's per-group ingredient lists to their exact size after registration.",
            "JEI allocates every one of these lists with room for 10 entries, but most groups hold a single",
            "ingredient. On large packs this wastes tens of megabytes.")
        .define("trimGroupingLists", true);

    public static final ForgeConfigSpec.IntValue MAX_NAVIGATION_HISTORY = BUILDER
        .comment("How many recipe navigations JEI keeps for the back/forward buttons.",
            "JEI keeps these forever, pinning every visited recipe layout and its inputs.",
            "Set to 0 to disable the history entirely.")
        .defineInRange("maxNavigationHistory", 32, 0, 256);

    public static final ForgeConfigSpec.IntValue MAX_BATCH_RENDERER_CACHE = BUILDER
        .comment("How many pre-baked item batch renderers JEI keeps alive.",
            "Each one pins a BakedModel and item stack for every ingredient on screen.")
        .defineInRange("maxBatchRendererCache", 2, 1, 16);

    public static final ForgeConfigSpec.BooleanValue CLEAR_BATCH_CACHE_ON_RELOAD = BUILDER
        .comment("Drop cached item batch renderers on resource reload so stale BakedModel references are not retained.",
            "JEI never invalidates this cache itself.")
        .define("clearBatchCacheOnReload", true);

    public static final ForgeConfigSpec.IntValue MAX_ANVIL_OUTPUT_LEVELS = BUILDER
        .comment("How many enchanted-book levels JEI keeps as possible anvil results for each",
            "(item, enchantment) pair. JEI generates one result per book level up to the enchantment's max,",
            "so on big packs a handful of items with high-max-level enchantments expand enormously.",
            "Outputs are not indexed for lookup, so this never makes a recipe undiscoverable.",
            "Set to 0 to keep every level.")
        .defineInRange("maxAnvilOutputLevels", 3, 0, 64);

    public static final ForgeConfigSpec.IntValue MAX_GRINDSTONE_DISENCHANT_INPUTS = BUILDER
        .comment("How many example inputs JEI keeps per grindstone disenchant recipe (one per book level).",
            "JEI builds a single disenchant recipe per enchantment and level whose input list holds EVERY",
            "enchantable item that the enchantment applies to, and keeps a copy of each. On a large pack this",
            "is by far the largest thing JEI allocates, and it grows with both item and enchantment count.",
            "WARNING: these inputs ARE indexed, so items past the cap will no longer have a grindstone",
            "disenchant recipe. Raise it if you use the grindstone recipe lookup.",
            "Set to 0 to keep every item.")
        .defineInRange("maxGrindstoneDisenchantInputs", 256, 0, 100000);

    public static final ForgeConfigSpec.BooleanValue FORCE_JET_RECIPE_LIMITS = BUILDER
        .comment("Turn on JET's own anvil and grindstone representative limits.",
            "Just Enough Threads already implements limits for JEI's two largest recipe generators,",
            "but they are opt-in. This switches them on and makes this mod stand down its own caps for",
            "those two generators, so the recipes are limited once rather than twice.",
            "Only has an effect when JET is installed. The disable-anvil-recipes switches are left alone,",
            "because removing recipes entirely is a gameplay choice rather than a memory one.")
        .define("forceJetRecipeLimits", true);

    public static final ForgeConfigSpec.BooleanValue DISABLE_JET_TOOLTIP_INDEX = BUILDER
        .comment("Force JET's tooltip search index off.",
            "JET's index is an inverted index holding one BitSet per distinct search token, each sized by the",
            "total ingredient count. Its memory grows with (tokens x ingredients) / 8, which is exactly the",
            "growth this mod is meant to stop. Search stays correct, it just falls back to JEI's own backend.")
        .define("disableJetTooltipIndex", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean lowMemorySearch = true;
    public static boolean releaseElementList = true;
    public static boolean trimGroupingLists = true;
    public static int maxNavigationHistory = 32;
    public static int maxBatchRendererCache = 2;
    public static boolean clearBatchCacheOnReload = true;
    public static int maxAnvilOutputLevels = 3;
    public static int maxGrindstoneDisenchantInputs = 256;
    public static boolean forceJetRecipeLimits = true;
    public static boolean disableJetTooltipIndex = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Jmf.MODID) && event.getConfig().getType() == ModConfig.Type.CLIENT) {
            lowMemorySearch = LOW_MEMORY_SEARCH.get();
            releaseElementList = RELEASE_ELEMENT_LIST.get();
            trimGroupingLists = TRIM_GROUPING_LISTS.get();
            maxNavigationHistory = MAX_NAVIGATION_HISTORY.get();
            maxBatchRendererCache = MAX_BATCH_RENDERER_CACHE.get();
            clearBatchCacheOnReload = CLEAR_BATCH_CACHE_ON_RELOAD.get();
            maxAnvilOutputLevels = MAX_ANVIL_OUTPUT_LEVELS.get();
            maxGrindstoneDisenchantInputs = MAX_GRINDSTONE_DISENCHANT_INPUTS.get();
            forceJetRecipeLimits = FORCE_JET_RECIPE_LIMITS.get();
            disableJetTooltipIndex = DISABLE_JET_TOOLTIP_INDEX.get();
        }
    }
}