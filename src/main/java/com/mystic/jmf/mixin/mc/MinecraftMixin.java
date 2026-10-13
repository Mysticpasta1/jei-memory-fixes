package com.mystic.jmf.mixin.mc;

import com.mystic.jmf.JeiMemoryHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Releases JEI's cached ingredient list whenever the player returns to the world.
 * <p>
 * Closing the last overlay screen is the point where nothing needs the list any more. Releasing it there
 * bounds the retention to "while a GUI is actually open" instead of "for the rest of the session".
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void jmf$releaseJeiElementList(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            JeiMemoryHooks.releaseCachedElementList();
        }
    }
}