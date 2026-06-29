package com.cantstopwinning;

import dev.anvil.api.Framework;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages CSW's overlay assets in {@code <configDir>/cantstopwinning/}. On first run the bundled
 * defaults are extracted there so the player can swap in their own GIF/WAV without rebuilding.
 */
public final class CswAssets {

    private static final String[] BUNDLED = {
            "celebration.gif", "celebration.wav", "loss.gif", "loss.png", "loss.wav"
    };
    private static final String[] IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".gif"};

    private CswAssets() {
    }

    /** @return the {@code <configDir>/cantstopwinning/} directory */
    public static Path dir() {
        return Framework.platform().configDir().resolve("cantstopwinning");
    }

    /** Creates the asset folder and extracts any missing bundled defaults. */
    public static void extractDefaults() {
        Path dir = dir();
        try {
            Files.createDirectories(dir);
            for (String name : BUNDLED) {
                Path target = dir.resolve(name);
                if (Files.exists(target)) {
                    continue;
                }
                try (InputStream in = CswAssets.class.getResourceAsStream("/assets/cantstopwinning/" + name)) {
                    if (in != null) {
                        Files.copy(in, target);
                    }
                }
            }
        } catch (Exception e) {
            Framework.log().error("Failed to extract CSW assets", e);
        }
    }

    /**
     * @param baseName e.g. {@code "celebration"} or {@code "loss"}
     * @return the first existing image file (PNG &gt; JPG &gt; JPEG &gt; GIF), or the {@code .gif}
     *         path as a fallback even if absent
     */
    public static Path image(String baseName) {
        Path dir = dir();
        for (String ext : IMAGE_EXTS) {
            Path p = dir.resolve(baseName + ext);
            if (Files.exists(p)) {
                return p;
            }
        }
        return dir.resolve(baseName + ".gif");
    }

    /**
     * @param baseName e.g. {@code "celebration"} or {@code "loss"}
     * @return the {@code .wav} sound path (may not exist)
     */
    public static Path sound(String baseName) {
        return dir().resolve(baseName + ".wav");
    }
}
