package com.game.art;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Small, deliberately aliased drawing vocabulary for the original pixel artwork. */
final class PixelArt {
    static final int INK = 0x0B1427;
    static final int IVORY = 0xF2F0D9;
    static final int LIGHT = 0xCCFFF2;
    static final int TEAL = 0x51DDB7;
    static final int DEEP = 0x194E57;
    static final int AMBER = 0xFFBE68;
    static final int ORANGE = 0xE87548;
    static final int LILAC = 0xC1A4F5;

    private PixelArt() { }

    static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D graphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        return graphics;
    }

    static void rect(Graphics2D g, int color, int x, int y, int width, int height) {
        g.setColor(new Color(color));
        g.fillRect(x, y, width, height);
    }

    static void oval(Graphics2D g, int color, int x, int y, int width, int height) {
        g.setColor(new Color(color));
        g.fillOval(x, y, width, height);
    }

    static void poly(Graphics2D g, int color, int... coordinates) {
        int count = coordinates.length / 2;
        int[] xs = new int[count];
        int[] ys = new int[count];
        for (int index = 0; index < count; index++) {
            xs[index] = coordinates[index * 2];
            ys[index] = coordinates[index * 2 + 1];
        }
        g.setColor(new Color(color));
        g.fillPolygon(xs, ys, count);
    }

    static void line(Graphics2D g, int color, int x1, int y1, int x2, int y2) {
        g.setColor(new Color(color));
        g.drawLine(x1, y1, x2, y2);
    }

    static void glow(Graphics2D g, int color, int alpha, int x, int y, int width, int height) {
        g.setColor(new Color((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha));
        g.fillOval(x, y, width, height);
    }

    static void spark(Graphics2D g, int color, int x, int y, int size) {
        rect(g, color, x - size, y, size * 2 + 1, 1);
        rect(g, color, x, y - size, 1, size * 2 + 1);
    }
}
