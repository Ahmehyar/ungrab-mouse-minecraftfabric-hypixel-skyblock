package com.marsbelt.freemouse;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class InventoryViewerPositionScreen extends Screen {
    private static final int TEXT_COLOR = 0xE6FFFFFF;
    private static final int HELP_COLOR = 0xB0FFFFFF;

    private int panelX;
    private int panelY;
    private boolean dragging = false;
    private double dragOffsetX;
    private double dragOffsetY;

    public InventoryViewerPositionScreen() {
        super(Text.translatable("screen.free_mouse_locked_view.inventory_viewer_position"));
    }

    @Override
    protected void init() {
        this.panelX = InventoryViewerOverlay.getConfiguredX(this.width);
        this.panelY = InventoryViewerOverlay.getConfiguredY(this.height);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.free_mouse_locked_view.reset_position"),
                button -> resetPosition()
        ).dimensions(this.width / 2 - 105, this.height - 28, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(this.width / 2 + 5, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 0x66000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, TEXT_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, "X: " + this.panelX + "  Y: " + this.panelY,
                this.width / 2, 24, TEXT_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.free_mouse_locked_view.inventory_viewer_position.help"),
                this.width / 2, this.height - 44, HELP_COLOR);

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        InventoryViewerOverlay.renderAt(context, minecraftClient, this.panelX, this.panelY);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isOverPanel(click.x(), click.y())) {
            this.dragging = true;
            this.dragOffsetX = click.x() - this.panelX;
            this.dragOffsetY = click.y() - this.panelY;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.dragging) {
            setPanelPosition((int) Math.round(click.x() - this.dragOffsetX),
                    (int) Math.round(click.y() - this.dragOffsetY), false);
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.dragging) {
            this.dragging = false;
            savePosition();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;

        return switch (input.key()) {
            case GLFW.GLFW_KEY_LEFT -> {
                movePanel(-step, 0);
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                movePanel(step, 0);
                yield true;
            }
            case GLFW.GLFW_KEY_UP -> {
                movePanel(0, -step);
                yield true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                movePanel(0, step);
                yield true;
            }
            default -> super.keyPressed(input);
        };
    }

    @Override
    public void close() {
        savePosition();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void movePanel(int deltaX, int deltaY) {
        setPanelPosition(this.panelX + deltaX, this.panelY + deltaY, true);
    }

    private void resetPosition() {
        FreeMouseConfig.resetInventoryViewerPosition();
        this.panelX = InventoryViewerOverlay.getConfiguredX(this.width);
        this.panelY = InventoryViewerOverlay.getConfiguredY(this.height);
    }

    private void setPanelPosition(int x, int y, boolean save) {
        int maxX = Math.max(0, this.width - InventoryViewerOverlay.PANEL_WIDTH);
        int maxY = Math.max(0, this.height - InventoryViewerOverlay.PANEL_HEIGHT);
        this.panelX = clamp(x, 0, maxX);
        this.panelY = clamp(y, 0, maxY);

        if (save) {
            savePosition();
        }
    }

    private void savePosition() {
        InventoryViewerOverlay.savePosition(this.panelX, this.panelY, this.width, this.height);
    }

    private boolean isOverPanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX
                && mouseX < this.panelX + InventoryViewerOverlay.PANEL_WIDTH
                && mouseY >= this.panelY
                && mouseY < this.panelY + InventoryViewerOverlay.PANEL_HEIGHT;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
