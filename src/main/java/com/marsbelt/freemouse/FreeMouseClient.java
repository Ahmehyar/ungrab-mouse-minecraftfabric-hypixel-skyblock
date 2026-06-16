package com.marsbelt.freemouse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class FreeMouseClient implements ClientModInitializer {
    public static final String MOD_ID = "free_mouse_locked_view";

    private static KeyBinding toggleKey;
    private static boolean active = false;
    private static float lockedYaw;
    private static float lockedPitch;
    private static boolean releasedUnsafeInputLastTick = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(FreeMouseClient::onEndClientTick);
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldCancelMouseLook(MinecraftClient client) {
        return active && client != null && client.currentScreen == null;
    }

    public static void onWindowFocusChanged(MinecraftClient client, boolean focused) {
        if (!active || client == null) {
            return;
        }

        keepCursorFree(client);

        if (!focused) {
            // Focus loss can leave vanilla key bindings in a pressed state until a
            // matching release event arrives. Clear gameplay actions immediately so
            // Free Mouse Lock never continues movement, attacking, using, jumping,
            // sneaking, or sprinting while Minecraft is in the background.
            releaseGameplayInput(client);
            releasedUnsafeInputLastTick = true;
        }
    }

    private static void onEndClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            toggle(client);
        }

        if (!active) {
            releasedUnsafeInputLastTick = false;
            return;
        }

        if (client.player == null || client.world == null) {
            active = false;
            releasedUnsafeInputLastTick = false;
            return;
        }

        keepCursorFree(client);

        boolean gameplayInputAllowed = client.currentScreen == null && client.isWindowFocused();

        if (!gameplayInputAllowed) {
            // Screens and unfocused windows should not inherit held movement/action
            // keys from live gameplay. Clearing these bindings lets vanilla GUI
            // behavior happen normally and guarantees the mod does not keep the
            // player moving, attacking, or using items in the background.
            releaseGameplayInput(client);
            releasedUnsafeInputLastTick = true;
        } else if (releasedUnsafeInputLastTick) {
            // New physical key events drive movement from here; the mod does not
            // synthesize pressed states when returning to the live game screen.
            releasedUnsafeInputLastTick = false;
        }

        lockView(client);
    }

    private static void toggle(MinecraftClient client) {
        if (active) {
            disable(client, true);
        } else {
            enable(client);
        }
    }

    private static void enable(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            toast(client, "Free Mouse unavailable here");
            return;
        }

        lockedYaw = client.player.getYaw();
        lockedPitch = client.player.getPitch();
        active = true;

        client.mouse.unlockCursor();
        lockView(client);
        toast(client, "Free Mouse Lock: ON");
    }

    private static void disable(MinecraftClient client, boolean announce) {
        if (!active) {
            return;
        }

        active = false;
        releasedUnsafeInputLastTick = false;

        if (client.player != null && client.world != null && client.currentScreen == null && client.isWindowFocused()) {
            client.mouse.lockCursor();
        }

        if (announce) {
            toast(client, "Free Mouse Lock: OFF");
        }
    }

    private static void lockView(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        client.player.setYaw(lockedYaw);
        client.player.setPitch(lockedPitch);
        client.player.headYaw = lockedYaw;
        client.player.bodyYaw = lockedYaw;
    }

    private static void releaseGameplayInput(MinecraftClient client) {
        releaseGameplayKeyBindings(client.options);

        if (client.player != null && client.player.input != null) {
            // Refresh the current movement snapshot so cleared keys take effect now,
            // not only after vanilla's next input tick.
            client.player.input.tick();
        }
    }

    private static void releaseGameplayKeyBindings(GameOptions options) {
        options.forwardKey.setPressed(false);
        options.backKey.setPressed(false);
        options.leftKey.setPressed(false);
        options.rightKey.setPressed(false);
        options.jumpKey.setPressed(false);
        options.sneakKey.setPressed(false);
        options.sprintKey.setPressed(false);
        options.attackKey.setPressed(false);
        options.useKey.setPressed(false);
    }

    private static void toast(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private static void keepCursorFree(MinecraftClient client) {
        // Active means "keep the mouse free". It should survive GUIs and focus loss.
        // The mixin also blocks vanilla lockCursor() while active, but this corrects
        // the state if another code path already grabbed the cursor.
        if (client.mouse.isCursorLocked()) {
            client.mouse.unlockCursor();
        }
    }
}
