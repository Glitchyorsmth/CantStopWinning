package com.cantstopwinning;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CelebrationOverlay implements HudRenderCallback {

    private static final int MAX_LOOPS = 2;

    private final String assetName;
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

    public CelebrationOverlay(String assetName, String idPrefix) {
        this.assetName = assetName;
        this.idPrefix = idPrefix;
    }

    public void start() {
        if (active) return;
        MinecraftClient.getInstance().execute(() -> {
            loadIfNeeded();
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

    private void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            frames = loadFrames();
            if (frames == null || frames.isEmpty()) {
                CantStopWinningClient.LOGGER.warn("No image found for {}", assetName);
                return;
            }

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
            CantStopWinningClient.LOGGER.info("Loaded {}: {} frames", assetName, frames.size());
        } catch (IOException e) {
            CantStopWinningClient.LOGGER.error("Failed to load {}", assetName, e);
        }
    }

    private List<GifDecoder.Frame> loadFrames() throws IOException {
        String baseName = assetName.contains(".") ? assetName.substring(0, assetName.lastIndexOf('.')) : assetName;

        // Try static formats first (PNG > JPG/JPEG), then animated GIF
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            InputStream in = openAsset(baseName + ext);
            if (in != null) {
                try (in) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        argb.getGraphics().drawImage(img, 0, 0, null);
                        List<GifDecoder.Frame> list = new ArrayList<>();
                        list.add(new GifDecoder.Frame(argb, 100));
                        return list;
                    }
                }
            }
        }

        InputStream gifIn = openAsset(baseName + ".gif");
        if (gifIn != null) {
            try (gifIn) {
                return GifDecoder.decode(gifIn);
            }
        }

        return null;
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
        InputStream jar = CantStopWinningClient.class.getResourceAsStream("/assets/cantstopwinning/" + filename);
        return jar;
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

        int fw = frameWidths[currentFrame];
        int fh = frameHeights[currentFrame];

        // Scale to fill screen height, maintain aspect ratio, center horizontally
        float scale = (float) sh / fh;
        int drawW = (int)(fw * scale);
        int drawH = sh;
        int drawX = (sw - drawW) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                frameTextures[currentFrame],
                drawX, 0,
                0.0f, 0.0f,
                drawW, drawH,
                fw, fh);
    }
}
