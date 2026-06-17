package com.marsbelt.freemouse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class FreeMouseClient implements ClientModInitializer {
    public static final String MOD_ID = "free_mouse_locked_view";

    private static final int TELEPORT_SYNC_GRACE_TICKS = 5;
    private static final double SUDDEN_POSITION_CHANGE_SQUARED = 4.0D;
    private static final float LOOK_CHANGE_EPSILON = 1.0F;

    private static KeyBinding toggleKey;
    private static boolean active = false;
    private static float lockedYaw;
    private static float lockedPitch;
    private static boolean releasedUnsafeInputLastTick = false;
    private static boolean lastGameplayInputAllowed = false;
    private static int lockSyncGraceTicks = 0;
    private static Object lastWorld = null;
    private static Object lastPlayer = null;
    private static boolean hasLastPosition = false;
    private static double lastX;
    private static double lastY;
    private static double lastZ;

    @Override
    public void onInitializeClient() {
        FreeMouseConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                KeyBinding.Category.MISC
        ));

        InventoryViewerOverlay.register();
        ClientTickEvents.END_CLIENT_TICK.register(FreeMouseClient::onEndClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disable(client, false));
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

        if (!focused) {
            // Focus loss can leave vanilla key bindings in a pressed state until a
            // matching release event arrives. Clear gameplay actions immediately so
            // Free Mouse Lock never continues movement, attacking, using, jumping,
            // sneaking, or sprinting while Minecraft is in the background.
            releaseGameplayInput(client);
            releasedUnsafeInputLastTick = true;
            releaseCursorOnly(client);
            return;
        }

        if (client.currentScreen == null && client.player != null && client.world != null
                && !client.mouse.isCursorLocked()) {
            client.mouse.lockCursor();
        }

        releaseCursorOnly(client);
    }

    private static void onEndClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            toggle(client);
        }

        if (!active) {
            resetRuntimeState();
            return;
        }

        if (client.player == null || client.world == null) {
            disable(client, false);
            return;
        }

        boolean gameplayInputAllowed = isGameplayInputAllowed(client);
        updateLockForServerCorrections(client);
        updateCursorState(client, gameplayInputAllowed);

        if (client.isWindowFocused()) {
            releasedUnsafeInputLastTick = false;
        } else {
            // Only clear gameplay keys while unfocused. Focused GUIs should keep
            // vanilla mouse behavior, including button presses used by screens.
            releaseGameplayInput(client);
            releasedUnsafeInputLastTick = true;
        }

        if (lockSyncGraceTicks > 0) {
            syncLockedViewToPlayer(client);
            lockSyncGraceTicks--;
        } else {
            lockView(client);
        }

        recordLastPlayerState(client);
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
        releasedUnsafeInputLastTick = false;
        lockSyncGraceTicks = 0;
        recordLastPlayerState(client);

        if (isGameplayInputAllowed(client) && !client.mouse.isCursorLocked()) {
            client.mouse.lockCursor();
        }

        releaseCursorOnly(client);
        lockView(client);
        toast(client, "Free Mouse Lock: ON");
    }

    private static void disable(MinecraftClient client, boolean announce) {
        if (!active) {
            return;
        }

        active = false;
        resetRuntimeState();

        if (client.player != null && client.world != null && client.currentScreen == null && client.isWindowFocused()) {
            client.mouse.lockCursor();
        }

        if (announce) {
            toast(client, "Free Mouse Lock: OFF");
        }
    }

    private static boolean isGameplayInputAllowed(MinecraftClient client) {
        return active
                && client != null
                && client.player != null
                && client.world != null
                && client.currentScreen == null
                && client.isWindowFocused();
    }

    private static void updateCursorState(MinecraftClient client, boolean gameplayInputAllowed) {
        if (gameplayInputAllowed) {
            if (!client.mouse.isCursorLocked()) {
                client.mouse.lockCursor();
            }

            if (!lastGameplayInputAllowed) {
                releaseCursorOnly(client);
            }
        } else if (lastGameplayInputAllowed) {
            releaseCursorOnly(client);
        }

        lastGameplayInputAllowed = gameplayInputAllowed;
    }

    private static void releaseCursorOnly(MinecraftClient client) {
        InputUtil.setCursorParameters(
                client.getWindow(),
                GLFW.GLFW_CURSOR_NORMAL,
                client.mouse.getX(),
                client.mouse.getY()
        );
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

    private static void updateLockForServerCorrections(MinecraftClient client) {
        boolean changedPlayerContext = client.world != lastWorld || client.player != lastPlayer;
        boolean suddenPositionChange = false;

        if (hasLastPosition && !changedPlayerContext) {
            double deltaX = client.player.getX() - lastX;
            double deltaY = client.player.getY() - lastY;
            double deltaZ = client.player.getZ() - lastZ;
            suddenPositionChange = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
                    > SUDDEN_POSITION_CHANGE_SQUARED;
        }

        boolean serverLookChange =
                Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - lockedYaw)) > LOOK_CHANGE_EPSILON
                        || Math.abs(client.player.getPitch() - lockedPitch) > LOOK_CHANGE_EPSILON;

        if (changedPlayerContext || suddenPositionChange || serverLookChange) {
            lockSyncGraceTicks = TELEPORT_SYNC_GRACE_TICKS;
        }

        if (lockSyncGraceTicks > 0) {
            syncLockedViewToPlayer(client);
        }
    }

    private static void syncLockedViewToPlayer(MinecraftClient client) {
        lockedYaw = client.player.getYaw();
        lockedPitch = client.player.getPitch();
    }

    private static void recordLastPlayerState(MinecraftClient client) {
        lastWorld = client.world;
        lastPlayer = client.player;
        hasLastPosition = true;
        lastX = client.player.getX();
        lastY = client.player.getY();
        lastZ = client.player.getZ();
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
        options.pickItemKey.setPressed(false);
    }

    private static void toast(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private static void resetRuntimeState() {
        releasedUnsafeInputLastTick = false;
        lastGameplayInputAllowed = false;
        lockSyncGraceTicks = 0;
        lastWorld = null;
        lastPlayer = null;
        hasLastPosition = false;
    }
}
