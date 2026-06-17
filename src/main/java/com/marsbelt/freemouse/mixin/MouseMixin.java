package com.marsbelt.freemouse.mixin;

import com.marsbelt.freemouse.FreeMouseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @ModifyArg(
            method = "lockCursor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(Lnet/minecraft/client/util/Window;IDD)V"
            ),
            index = 1
    )
    private int freeMouseLockedView$keepOsCursorNormal(int cursorMode) {
        return FreeMouseClient.isActive() ? GLFW.GLFW_CURSOR_NORMAL : cursorMode;
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
