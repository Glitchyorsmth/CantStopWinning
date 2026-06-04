package com.cantstopwinning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path ASSETS_DIR = FabricLoader.getInstance().getConfigDir().resolve("cantstopwinning");
    private static final Path CONFIG_PATH = ASSETS_DIR.resolve("cantstopwinning.json");

    public boolean enabled = true;
    public double volume = 0.5;  // 0.0 to 1.0
    public List<String> triggerMessages = new ArrayList<>();

    private static final String DEFAULT_TRIGGER = "Wow! You dug out 1,000,000 coins!";

    public static ModConfig load() {
        // Create the config/cantstopwinning/ folder and extract default assets
        try {
            Files.createDirectories(ASSETS_DIR);
            extractDefault("celebration.gif");
            extractDefault("celebration.wav");
        } catch (IOException e) {
            CantStopWinningClient.LOGGER.error("Failed to create assets dir", e);
        }

        if (!CONFIG_PATH.toFile().exists()) {
            ModConfig def = new ModConfig();
            def.triggerMessages.add(DEFAULT_TRIGGER);
            def.save();
            return def;
        }
        try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
            ModConfig cfg = GSON.fromJson(r, ModConfig.class);
            if (cfg == null) cfg = new ModConfig();
            if (cfg.triggerMessages == null) cfg.triggerMessages = new ArrayList<>();
            // Clamp volume
            cfg.volume = Math.max(0.0, Math.min(1.0, cfg.volume));
            return cfg;
        } catch (IOException e) {
            return new ModConfig();
        }
    }

    /**
     * Extracts a default asset from the JAR to the config folder if it doesn't already exist.
     * Users can then replace these files without rebuilding.
     */
    private static void extractDefault(String filename) throws IOException {
        Path target = ASSETS_DIR.resolve(filename);
        if (Files.exists(target)) return;  // Don't overwrite user's custom files

        try (InputStream in = ModConfig.class.getResourceAsStream("/assets/cantstopwinning/" + filename)) {
            if (in != null) {
                Files.copy(in, target);
            }
        }
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            CantStopWinningClient.LOGGER.error("Failed to save config", e);
        }
    }
}
