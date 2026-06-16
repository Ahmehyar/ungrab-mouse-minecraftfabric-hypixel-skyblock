package com.marsbelt.freemouse.mixin;

import com.marsbelt.freemouse.FreeMouseClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "onWindowFocusChanged", at = @At("TAIL"))
    private void freeMouseLockedView$onWindowFocusChanged(boolean focused, CallbackInfo ci) {
        FreeMouseClient.onWindowFocusChanged((MinecraftClient) (Object) this, focused);
    }
}
