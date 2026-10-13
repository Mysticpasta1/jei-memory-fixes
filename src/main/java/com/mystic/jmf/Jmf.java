package com.mystic.jmf;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
         */
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new PreparableReloadListener() {
                @Override
                public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                    ProfilerFiller backgroundProfiler, ProfilerFiller gameProfiler,
                    Executor backgroundExecutor, Executor gameExecutor) {
                    JeiMemoryHooks.onClientResourceReload();
                    return CompletableFuture.completedFuture(null);
                }
            });
        }
    }
}
