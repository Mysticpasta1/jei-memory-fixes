package com.mystic.jmf.mixin.jei;

import com.mystic.jmf.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Deque;
import java.util.Optional;

/**
 * Bounds JEI's recipe back/forward navigation history.
 * <p>
 * {@code NavigationHistory} is a pair of unbounded deques holding focuses and recipe layouts. Every entry
 * pins recipe objects, layouts and their inputs for the rest of the session, so browsing recipes is a
 * slow leak. This trims it to a configurable depth.
 */
@Mixin(targets = "mezz.jei.gui.recipes.NavigationHistory", remap = false)
public abstract class NavigationHistoryMixin {

    @Shadow(remap = false)
    @Final
    private Deque<?> backwardHistory;

    @Shadow(remap = false)
    @Final
    private Deque<?> forwardHistory;

    @Inject(method = "record", at = @At("TAIL"), remap = false)
    private void jmf$trimAfterRecord(Object value, CallbackInfo ci) {
        jmf$trim();
    }

    @Inject(method = "goBack", at = @At("TAIL"), remap = false)
    private void jmf$trimAfterGoBack(Object currentValue, CallbackInfoReturnable<Optional<Object>> cir) {
        jmf$trim();
    }

    @Inject(method = "goForward", at = @At("TAIL"), remap = false)
    private void jmf$trimAfterGoForward(Object currentValue, CallbackInfoReturnable<Optional<Object>> cir) {
        jmf$trim();
    }

    @Unique
    private void jmf$trim() {
        int max = Config.maxNavigationHistory;
        while (backwardHistory.size() > max) {
            backwardHistory.pollLast();
        }
        while (forwardHistory.size() > max) {
            forwardHistory.pollLast();
        }
    }
}
