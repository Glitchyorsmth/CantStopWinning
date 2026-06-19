package com.cantstopwinning;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CelebrationOverlay implements HudRenderCallback {

    private static final int MAX_LOOPS = 2;

    private final String gifName;
    private final String idPrefix;

    private List<GifDecoder.Frame> frames;
    private int currentFrame = 0;
    private long lastFrameTime = 0;
    private volatile boolean active = false;
    private boolean loaded = false;
    private int loopCount = 0;
    private double lastOpacity = -1;

    private Identifier[] frameTextures;
    private int[] frameWidths;
    private int[] frameHeights;

    public CelebrationOverlay(String gifName, String idPrefix) {
        this.gifName = gifName;
        this.idPrefix = idPrefix;
    }

    public void start() {
        if (active) return;
        MinecraftClient.getInstance().execute(() -> {
            loadGifIfNeeded();
            rebuildTexturesIfOpacityChanged();
            if (frames == null || frames.isEmpty()) return;
            currentFrame = 0;
            loopCount = 0;
            lastFrameTime = System.currentTimeMillis();
            active = true;
        });
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    private void loadGifIfNeeded() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = openAsset(gifName)) {
            if (in == null) {
                CantStopWinningClient.LOGGER.warn("{} not found", gifName);
                return;
            }
            frames = GifDecoder.decode(in);
            if (frames.isEmpty()) return;

            frameTextures = new Identifier[frames.size()];
            frameWidths = new int[frames.size()];
            frameHeights = new int[frames.size()];

            for (int i = 0; i < frames.size(); i++) {
                frameTextures[i] = Identifier.of("cantstopwinning", idPrefix + "_" + i);
                BufferedImage img = frames.get(i).image();
                frameWidths[i] = img.getWidth();
                frameHeights[i] = img.getHeight();
                registerFrameTexture(i, img);
            }
            lastOpacity = CantStopWinningClient.CONFIG.overlayOpacity;
            CantStopWinningClient.LOGGER.info("Loaded {}: {} frames", gifName, frames.size());
        } catch (IOException e) {
            CantStopWinningClient.LOGGER.error("Failed to load {}", gifName, e);
        }
    }

    private void rebuildTexturesIfOpacityChanged() {
        if (frames == null || frames.isEmpty()) return;
        double currentOpacity = CantStopWinningClient.CONFIG.overlayOpacity;
        if (Math.abs(currentOpacity - lastOpacity) < 0.01) return;
        lastOpacity = currentOpacity;
        for (int i = 0; i < frames.size(); i++) {
            registerFrameTexture(i, frames.get(i).image());
        }
    }

    private static InputStream openAsset(String filename) throws IOException {
        Path external = ModConfig.ASSETS_DIR.resolve(filename);
        if (Files.exists(external)) {
            return Files.newInputStream(external);
        }
        return CantStopWinningClient.class.getResourceAsStream("/assets/cantstopwinning/" + filename);
    }

    private void registerFrameTexture(int index, BufferedImage img) {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = img.getWidth(), h = img.getHeight();
        float opacity = (float) CantStopWinningClient.CONFIG.overlayOpacity;
        NativeImage ni = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (int)(((argb >>> 24) & 0xFF) * opacity) & 0xFF;
                ni.setColorArgb(x, y, (a << 24) | (argb & 0x00FFFFFF));
            }
        }
        NativeImageBackedTexture tex = new NativeImageBackedTexture(
                () -> "cantstopwinning:" + idPrefix + "_" + index, ni);
        client.getTextureManager().registerTexture(frameTextures[index], tex);
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!active || frames == null || frames.isEmpty()) return;

        long now = System.currentTimeMillis();
        GifDecoder.Frame frame = frames.get(currentFrame);
        if (now - lastFrameTime >= frame.delayMs()) {
            int nextFrame = currentFrame + 1;
            if (nextFrame >= frames.size()) {
                loopCount++;
                // Single-frame overlays stay until stop() is called (by audio callback)
                if (frames.size() > 1 && loopCount >= MAX_LOOPS) {
                    active = false;
                    return;
                }
                nextFrame = 0;
            }
            currentFrame = nextFrame;
            lastFrameTime = now;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                frameTextures[currentFrame],
                0, 0,
                0.0f, 0.0f,
                sw, sh,
                frameWidths[currentFrame],
                frameHeights[currentFrame]);
    }
}
