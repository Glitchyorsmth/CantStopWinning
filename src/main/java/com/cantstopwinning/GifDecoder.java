package com.cantstopwinning;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifDecoder {

    public record Frame(BufferedImage image, int delayMs) {}

    public static List<Frame> decode(InputStream in) throws IOException {
        List<Frame> frames = new ArrayList<>();

        ImageInputStream iis = ImageIO.createImageInputStream(in);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) throw new IOException("No GIF reader available");

        ImageReader reader = readers.next();
        reader.setInput(iis);

        int count = reader.getNumImages(true);
        for (int i = 0; i < count; i++) {
            BufferedImage img = reader.read(i);
            int delayMs = 100;
            try {
                javax.imageio.metadata.IIOMetadata meta = reader.getImageMetadata(i);
                String formatName = meta.getNativeMetadataFormatName();
                org.w3c.dom.Node root = meta.getAsTree(formatName);
                delayMs = extractDelay(root);
            } catch (Exception ignored) {}
            frames.add(new Frame(img, delayMs));
        }
        reader.dispose();
        return frames;
    }

    private static int extractDelay(org.w3c.dom.Node root) {
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            org.w3c.dom.Node child = root.getChildNodes().item(i);
            if ("GraphicControlExtension".equals(child.getNodeName())) {
                org.w3c.dom.NamedNodeMap attrs = child.getAttributes();
                if (attrs != null) {
                    org.w3c.dom.Node delay = attrs.getNamedItem("delayTime");
                    if (delay != null) {
                        int hundredths = Integer.parseInt(delay.getNodeValue());
                        return Math.max(hundredths * 10, 20);
                    }
                }
            }
            int found = extractDelay(child);
            if (found != 100) return found;
        }
        return 100;
    }
}
