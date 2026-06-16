package com.marsbelt.freemouse.mixin;

import com.marsbelt.freemouse.FreeMouseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void freeMouseLockedView$cancelCursorRelock(CallbackInfo ci) {
        if (FreeMouseClient.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void freeMouseLockedView$cancelMouseLook(double timeDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (FreeMouseClient.shouldCancelMouseLook(client)) {
            this.cursorDeltaX = 0.0D;
            this.cursorDeltaY = 0.0D;
            ci.cancel();
        }
    }
}
