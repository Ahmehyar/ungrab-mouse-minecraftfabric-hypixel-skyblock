package com.marsbelt.freemouse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FreeMouseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve(FreeMouseClient.MOD_ID + ".json");
    private static final int DEFAULT_AUTO_X = -1;
    private static final int DEFAULT_Y = 30;
    private static ConfigData data = new ConfigData();

    private FreeMouseConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            data = loaded == null ? new ConfigData() : loaded;
        } catch (IOException | RuntimeException ignored) {
            data = new ConfigData();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
            // The overlay can keep working with in-memory defaults if the config
            // file cannot be written.
        }
    }

    public static int getInventoryViewerX(int screenWidth, int panelWidth) {
        int x = data.inventoryViewerX < 0 ? defaultInventoryViewerX(screenWidth, panelWidth) : data.inventoryViewerX;
        return clamp(x, 0, Math.max(0, screenWidth - panelWidth));
    }

    public static int getInventoryViewerY(int screenHeight, int panelHeight) {
        return clamp(data.inventoryViewerY, 0, Math.max(0, screenHeight - panelHeight));
    }

    public static void setInventoryViewerPosition(int x, int y, int screenWidth, int screenHeight,
            int panelWidth, int panelHeight) {
        data.inventoryViewerX = clamp(x, 0, Math.max(0, screenWidth - panelWidth));
        data.inventoryViewerY = clamp(y, 0, Math.max(0, screenHeight - panelHeight));
        save();
    }

    public static void resetInventoryViewerPosition() {
        data.inventoryViewerX = DEFAULT_AUTO_X;
        data.inventoryViewerY = DEFAULT_Y;
        save();
    }

    public static int defaultInventoryViewerX(int screenWidth, int panelWidth) {
        return Math.max(4, screenWidth - panelWidth - 8);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static final class ConfigData {
        int inventoryViewerX = DEFAULT_AUTO_X;
        int inventoryViewerY = DEFAULT_Y;
    }
}
