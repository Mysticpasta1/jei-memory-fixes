package com.mystic.jmf;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;


@Mod(Jmf.MODID)
public class Jmf {

    public static final String MODID = "jmf";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Jmf() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("JEI/JET memory fixes active");
        }

        /**
         * JEI's batch renderer cache pins BakedModel references and is never invalidated on reload.
         * <p>
         * This must be a {@link SimplePreparableReloadListener} rather than a hand-rolled
         * {@link PreparableReloadListener}. The interface hands every listener a
         * {@link PreparableReloadListener.PreparationBarrier}, which completes when the reload's preparation
         * phase finishes, and the listener is expected to chain its work onto it. That is what
         * {@code SimplePreparableReloadListener} does for you: it runs {@code prepare} on the background
         * executor, {@code thenCompose}s the barrier, and runs {@code apply} on the game executor.
         * <p>
         * Returning {@code CompletableFuture.completedFuture(null)} from {@code reload} directly ignores the
         * barrier entirely and declares the listener finished at the start of the phase the reload coordinates
         * on. The reload never completes, so the game sits on the loading screen forever, silently and without
         * consuming CPU. Nothing about it points at the listener, which is why it needs spelling out here.
         */
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new SimplePreparableReloadListener<Object>() {
                @Override
                protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(Object unused, ResourceManager resourceManager, ProfilerFiller profiler) {
                    JeiMemoryHooks.onClientResourceReload();
                }
            });
        }
    }
}
