package com.game.assets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import com.game.art.OriginalArt;

/** Loads each asset once; packaged resources and the Eclipse source layout both work. */
public final class Assets {
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    private Assets() { }

    public static synchronized BufferedImage load(String name) {
        if (name == null || name.isBlank() || name.contains("..") || name.contains("\\")
                || name.startsWith("/") || name.contains(":")) {
            throw new IllegalArgumentException("Asset name must be a relative resource name.");
        }
        return IMAGES.computeIfAbsent(name, Assets::read);
    }

    private static BufferedImage read(String name) {
        BufferedImage original = OriginalArt.create(name);
        if (original != null) return original;
        try {
            InputStream resource = Assets.class.getResourceAsStream("/" + name);
            if (resource != null) {
                try (InputStream stream = resource) {
                    return decode(stream, name);
                }
            }
            Path sourceAsset = Path.of("src", name);
            if (Files.isRegularFile(sourceAsset)) {
                try (InputStream stream = Files.newInputStream(sourceAsset)) {
                    return decode(stream, name);
                }
            }
            throw new IOException("Resource was not found on the classpath or at " + sourceAsset);
        } catch (IOException exception) {
            System.err.println("Could not load asset '" + name + "': " + exception.getMessage());
            return placeholder();
        }
    }

    private static BufferedImage decode(InputStream stream, String name) throws IOException {
        BufferedImage image = ImageIO.read(stream);
        if (image == null) {
            throw new IOException("Unsupported or damaged image: " + name);
        }
        return image;
    }

    private static BufferedImage placeholder() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.MAGENTA);
            graphics.fillRect(0, 0, 32, 32);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, 16, 16);
            graphics.fillRect(16, 16, 16, 16);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
