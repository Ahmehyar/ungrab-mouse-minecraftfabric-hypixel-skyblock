package com.marsbelt.freemouse;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class InventoryViewerOverlay {
    private static final Identifier HUD_ID = Identifier.of(FreeMouseClient.MOD_ID, "inventory_viewer");
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_OFFSET = 1;
    private static final int GRID_COLUMNS = 9;
    private static final int MAIN_ROWS = 3;
    private static final int SIDE_SLOTS = 5;
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 12;
    private static final int GRID_GAP = 4;
    private static final int SIDE_GAP = 6;
    static final int PANEL_WIDTH = PADDING * 2 + GRID_COLUMNS * SLOT_SIZE + SIDE_GAP + SLOT_SIZE;
    static final int PANEL_HEIGHT = PADDING * 2 + HEADER_HEIGHT + SIDE_SLOTS * SLOT_SIZE;
    private static final int PANEL_BACKGROUND = 0xB0101010;
    private static final int PANEL_BORDER = 0xAA606060;
    private static final int SLOT_BACKGROUND = 0x88303030;
    private static final int SLOT_BORDER = 0xAA202020;
    private static final int SLOT_HIGHLIGHT = 0xD0E0E0E0;
    private static final int TEXT_COLOR = 0xE6FFFFFF;

    private static KeyBinding toggleKey;
    private static KeyBinding editPositionKey;
    private static boolean visible = false;

    private InventoryViewerOverlay() {
    }

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + FreeMouseClient.MOD_ID + ".inventory_viewer",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                KeyBinding.Category.MISC
        ));
        editPositionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + FreeMouseClient.MOD_ID + ".inventory_viewer_position",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(InventoryViewerOverlay::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, HUD_ID, InventoryViewerOverlay::render);
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            visible = !visible;
            toast(client, visible ? "Inventory Viewer: ON" : "Inventory Viewer: OFF");
        }

        while (editPositionKey.wasPressed()) {
            if (!(client.currentScreen instanceof InventoryViewerPositionScreen)) {
                client.setScreen(new InventoryViewerPositionScreen());
            }
        }
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!visible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        int panelX = getConfiguredX(context.getScaledWindowWidth());
        int panelY = getConfiguredY(context.getScaledWindowHeight());

        renderAt(context, client, panelX, panelY);
    }

    static int getConfiguredX(int screenWidth) {
        return FreeMouseConfig.getInventoryViewerX(screenWidth, PANEL_WIDTH);
    }

    static int getConfiguredY(int screenHeight) {
        return FreeMouseConfig.getInventoryViewerY(screenHeight, PANEL_HEIGHT);
    }

    static void savePosition(int x, int y, int screenWidth, int screenHeight) {
        FreeMouseConfig.setInventoryViewerPosition(x, y, screenWidth, screenHeight, PANEL_WIDTH, PANEL_HEIGHT);
    }

    static void renderAt(DrawContext context, MinecraftClient client, int panelX, int panelY) {
        if (client.player == null) {
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        drawPanel(context, client, panelX, panelY);
        drawMainInventory(context, client, inventory, panelX + PADDING, panelY + PADDING + HEADER_HEIGHT);
        drawEquipment(context, client, panelX + PADDING + GRID_COLUMNS * SLOT_SIZE + SIDE_GAP,
                panelY + PADDING + HEADER_HEIGHT);
    }

    private static void drawPanel(DrawContext context, MinecraftClient client, int x, int y) {
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, PANEL_BACKGROUND);
        drawOutline(context, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER);
        context.drawTextWithShadow(client.textRenderer, "Inventory", x + PADDING, y + 4, TEXT_COLOR);
    }

    private static void drawMainInventory(DrawContext context, MinecraftClient client, PlayerInventory inventory,
            int x, int y) {
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int slot = PlayerInventory.getHotbarSize() + row * GRID_COLUMNS + column;
                drawSlot(context, client, stackFromMain(inventory, slot), x + column * SLOT_SIZE,
                        y + row * SLOT_SIZE, false);
            }
        }

        int hotbarY = y + MAIN_ROWS * SLOT_SIZE + GRID_GAP;
        int selectedSlot = inventory.getSelectedSlot();
        for (int column = 0; column < GRID_COLUMNS; column++) {
            drawSlot(context, client, stackFromMain(inventory, column), x + column * SLOT_SIZE, hotbarY,
                    column == selectedSlot);
        }
    }

    private static void drawEquipment(DrawContext context, MinecraftClient client, int x, int y) {
        drawSlot(context, client, client.player.getEquippedStack(EquipmentSlot.HEAD), x, y, false);
        drawSlot(context, client, client.player.getEquippedStack(EquipmentSlot.CHEST), x, y + SLOT_SIZE, false);
        drawSlot(context, client, client.player.getEquippedStack(EquipmentSlot.LEGS), x, y + SLOT_SIZE * 2, false);
        drawSlot(context, client, client.player.getEquippedStack(EquipmentSlot.FEET), x, y + SLOT_SIZE * 3, false);
        drawSlot(context, client, client.player.getEquippedStack(EquipmentSlot.OFFHAND), x, y + SLOT_SIZE * 4, false);
    }

    private static ItemStack stackFromMain(PlayerInventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getMainStacks().size()) {
            return ItemStack.EMPTY;
        }

        return inventory.getMainStacks().get(slot);
    }

    private static void drawSlot(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y,
            boolean selected) {
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER);
        context.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_BACKGROUND);

        if (selected) {
            drawOutline(context, x, y, SLOT_SIZE, SLOT_SIZE, SLOT_HIGHLIGHT);
        }

        if (!stack.isEmpty()) {
            context.drawItem(stack, x + ITEM_OFFSET, y + ITEM_OFFSET);
            context.drawStackOverlay(client.textRenderer, stack, x + ITEM_OFFSET, y + ITEM_OFFSET);
        }
    }

    private static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void toast(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }
}
