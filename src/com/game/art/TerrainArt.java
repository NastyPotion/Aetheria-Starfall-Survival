package com.game.art;

import static com.game.art.PixelArt.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Original, deterministic 48-pixel biomes. Returned sprites are shared read-only assets. */
public final class TerrainArt {
    private static final Map<Integer, BufferedImage> FLOORS = new HashMap<>();
    private static final Map<Integer, BufferedImage> OBSTACLES = new HashMap<>();
    private static final int[][] PALETTES = {
        {0x183C3C, 0x214947, 0x2D5A50, 0x387360, 0x51DDB7},
        {0x252B46, 0x303653, 0x424662, 0x5D577E, 0xB6A2EE},
        {0x3A3439, 0x484047, 0x615049, 0x846750, 0xEABB79},
        {0x173846, 0x214653, 0x2C6170, 0x3D8292, 0x69D7F4}
    };

    private TerrainArt() { }

    /** biome 0=verdant, 1=rift, 2=ruins, 3=tidal; sixteen fixed patterns per biome. */
    public static synchronized BufferedImage floor(int variant, int biome) {
        int key = Math.floorMod(biome, PALETTES.length) * 16 + Math.floorMod(variant, 16);
        return FLOORS.computeIfAbsent(key, TerrainArt::makeFloor);
    }

    /** Native 48x48 transparent luminous tree, crystal outcrop or ruined monolith. */
    public static synchronized BufferedImage obstacle(int variant, int biome) {
        int key = Math.floorMod(biome, PALETTES.length) * 16 + Math.floorMod(variant, 16);
        return OBSTACLES.computeIfAbsent(key, TerrainArt::makeObstacle);
    }

    private static BufferedImage makeFloor(int key) {
        int biome = key / 16;
        int variant = key % 16;
        int[] palette = PALETTES[biome];
        BufferedImage image = image(48, 48);
        Graphics2D g = graphics(image);
        Random random = new Random(9317L + key * 7349L);
        try {
            rect(g, palette[0], 0, 0, 48, 48);
            // Broken clusters, rather than an obvious border, let tiles join naturally.
            for (int cluster = 0; cluster < 11; cluster++) {
                int x = random.nextInt(46);
                int y = random.nextInt(46);
                int length = 3 + random.nextInt(8);
                rect(g, palette[1], x, y, Math.min(length, 48 - x), 1 + random.nextInt(3));
            }
            for (int tuft = 0; tuft < 7; tuft++) {
                int x = 3 + random.nextInt(41);
                int y = 4 + random.nextInt(39);
                if (biome == 2) {
                    line(g, palette[2], x, y, x + 3, y + 1);
                    rect(g, palette[1], x + 1, y + 2, 4, 1);
                } else if (biome == 3) {
                    line(g, palette[2], x - 2, y, x + 2, y);
                    line(g, palette[3], x, y - 1, x + 3, y - 1);
                    rect(g, palette[1], x + 3, y - 2, 2, 1);
                } else {
                    line(g, palette[2], x, y, x - 2, y - 3);
                    line(g, palette[3], x, y, x + 1, y - 4);
                    rect(g, palette[1], x - 2, y + 1, 5, 1);
                }
            }
            if (variant % 4 == 0) {
                int x = 12 + random.nextInt(24);
                int y = 12 + random.nextInt(24);
                glow(g, palette[4], 18, x - 6, y - 5, 13, 11);
                rect(g, palette[4], x, y, 1, 1);
                rect(g, palette[3], x + 4, y - 3, 1, 1);
            }
            if (biome == 2 && variant % 3 == 0) {
                line(g, palette[1], 0, 34, 15, 34);
                line(g, palette[1], 15, 34, 23, 26);
                line(g, palette[2], 2, 35, 14, 35);
            }
        } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage makeObstacle(int key) {
        int biome = key / 16;
        int variant = key % 16;
        BufferedImage image = image(48, 48);
        Graphics2D g = graphics(image);
        try {
            glow(g, INK, 100, 5, 36, 39, 11);
            if (biome == 1) crystal(g, variant);
            else if (biome == 2) monolith(g, variant);
            else if (biome == 3) tidalCoral(g, variant);
            else tree(g, variant);
        } finally { g.dispose(); }
        return image;
    }

    private static void tree(Graphics2D g, int variant) {
        poly(g, INK, 19, 28, 31, 27, 31, 38, 38, 43, 10, 43, 18, 37);
        poly(g, 0x3F5A60, 23, 23, 29, 26, 27, 38, 33, 41, 16, 41, 22, 35);
        rect(g, 0x729A88, 24, 28, 2, 11);
        line(g, 0x24424B, 22, 29, 17, 24);
        poly(g, INK, 4, 19, 9, 10, 16, 10, 20, 4, 33, 4, 38, 11, 44, 14,
                46, 25, 39, 32, 10, 32, 2, 27);
        poly(g, 0x1B5554, 5, 19, 12, 12, 18, 12, 22, 7, 31, 7, 37, 14,
                41, 15, 43, 25, 37, 29, 12, 29, 5, 25);
        poly(g, 0x2A7968, 9, 17, 15, 12, 22, 13, 25, 8, 31, 9, 34, 14,
                39, 17, 37, 23, 29, 25, 21, 22, 14, 25, 8, 22);
        poly(g, 0x43A47F, 12, 15, 20, 14, 24, 10, 30, 10, 31, 14,
                25, 16, 22, 20, 15, 19);
        line(g, 0x75CE9D, 16, 13, 21, 13);
        line(g, 0x75CE9D, 24, 10, 28, 10);
        // Hanging bioluminescent seeds give the forest a distinct silhouette.
        for (int index = 0; index < 3; index++) {
            int x = 12 + index * 11;
            int y = 25 + Math.floorMod(variant + index * 3, 5);
            line(g, 0x5AA18B, x, 22, x, y + 3);
            glow(g, TEAL, 35, x - 4, y - 1, 9, 10);
            rect(g, 0x58CFA2, x - 1, y, 3, 5);
            rect(g, LIGHT, x, y + 1, 1, 2);
        }
        rect(g, 0x367C66, 7, 28, 4, 2);
        rect(g, 0x367C66, 36, 29, 5, 2);
    }

    private static void crystal(Graphics2D g, int variant) {
        poly(g, INK, 8, 38, 6, 22, 16, 15, 20, 22, 25, 3, 35, 12, 37, 28,
                41, 21, 46, 33, 39, 42, 16, 43);
        poly(g, 0x514274, 21, 35, 25, 6, 33, 13, 34, 36, 28, 41);
        poly(g, 0x8471AD, 25, 6, 28, 16, 27, 36, 21, 35);
        poly(g, 0xC4B3EE, 25, 7, 27, 16, 24, 29, 24, 15);
        poly(g, 0x3C3B65, 8, 24, 15, 18, 21, 37, 16, 41, 10, 36);
        poly(g, 0x796493, 9, 25, 14, 20, 15, 34, 12, 36);
        poly(g, 0x6C588C, 32, 36, 40, 25, 43, 34, 38, 39);
        line(g, LILAC, 37, 33, 40, 28);
        glow(g, LILAC, 45, 12, 30, 29, 13);
        spark(g, LIGHT, 27, 12, 1);
        rect(g, LILAC, 13 + variant % 7, 40, 3, 2);
    }

    private static void monolith(Graphics2D g, int variant) {
        poly(g, INK, 10, 39, 12, 11, 20, 5, 33, 8, 36, 39, 41, 43, 5, 43);
        poly(g, 0x5B5258, 13, 13, 20, 8, 30, 10, 33, 39, 12, 39);
        poly(g, 0x796C66, 13, 13, 20, 8, 20, 38, 12, 39);
        poly(g, 0x363D4B, 21, 10, 30, 12, 32, 37, 22, 38);
        rect(g, 0xA28A70, 15, 16, 3, 13);
        line(g, 0xC1A180, 15, 14, 18, 12);
        line(g, INK, 15, 32, 22, 28);
        line(g, INK, 22, 28, 28, 30);
        glow(g, AMBER, 35, 15, 17, 20, 18);
        rect(g, AMBER, 24, 19, 2, 9);
        rect(g, AMBER, 21, 22, 8, 2);
        rect(g, IVORY, 24, 22, 2, 2);
        poly(g, 0x3B6657, 6, 40, 13, 34, 19, 37, 25, 39, 35, 36, 41, 41);
        rect(g, 0x60967A, 10 + variant % 5, 37, 5, 2);
    }

    private static void tidalCoral(Graphics2D g, int variant) {
        glow(g, 0x65D7E7, 24, 3, 2, 43, 43);
        // A branching, asymmetric sea fan gives tidal obstacles their own silhouette.
        poly(g, INK, 18, 39, 20, 30, 13, 27, 6, 21, 3, 12, 7, 6, 12, 6,
                15, 10, 12, 16, 18, 21, 22, 23, 21, 13, 18, 7, 22, 2, 27, 3,
                30, 8, 27, 16, 29, 23, 34, 18, 33, 10, 36, 4, 42, 4, 46, 10,
                43, 18, 39, 24, 31, 30, 29, 39, 37, 42, 34, 46, 12, 46, 10, 42);
        poly(g, 0x27596F, 21, 39, 23, 29, 15, 24, 9, 20, 6, 12, 8, 9, 11, 9,
                12, 11, 10, 16, 16, 23, 25, 26, 25, 16, 24, 10, 21, 7, 23, 5,
                26, 6, 27, 9, 25, 16, 27, 25, 30, 26, 37, 21, 39, 16, 36, 11,
                38, 7, 41, 7, 43, 11, 41, 17, 36, 24, 28, 29, 27, 39, 32, 42,
                30, 44, 17, 44, 14, 42);
        poly(g, 0x388DAB, 22, 39, 24, 29, 17, 24, 11, 20, 8, 13, 9, 10, 11, 11,
                10, 16, 16, 23, 26, 27, 26, 17, 25, 10, 23, 7, 24, 6, 26, 8,
                24, 16, 27, 27, 30, 28, 38, 22, 41, 15, 39, 10, 40, 8, 42, 11,
                40, 17, 36, 23, 27, 30, 26, 40, 29, 42, 20, 43);
        line(g, 0x74CBCC, 24, 37, 26, 29);
        line(g, 0x74CBCC, 25, 29, 15, 23);
        line(g, 0x74CBCC, 14, 22, 10, 17);
        line(g, 0x74CBCC, 27, 28, 38, 20);
        line(g, 0x74CBCC, 27, 25, 25, 16);
        line(g, 0xB0EEDD, 10, 12, 10, 14);
        line(g, 0xB0EEDD, 24, 7, 25, 9);
        line(g, 0xB0EEDD, 40, 9, 41, 11);
        // Featherlike offshoots and glowing polyps break the mechanical branch lines.
        poly(g, 0x3C91A0, 14, 24, 9, 27, 9, 31, 15, 29, 18, 27);
        poly(g, 0x4BA7AE, 29, 32, 35, 30, 39, 31, 35, 35, 29, 36);
        line(g, 0x82D8CF, 11, 29, 15, 27);
        line(g, 0x82D8CF, 31, 34, 36, 32);
        int shift = variant % 3;
        glow(g, 0x7BE9E2, 38, 4, 5, 11, 11);
        rect(g, 0x6ACDD4, 7, 8, 5, 4);
        rect(g, LIGHT, 8, 8, 3, 1);
        glow(g, 0x7BE9E2, 38, 20, 1, 10, 11);
        rect(g, 0x6ACDD4, 23, 4, 4, 4);
        rect(g, LIGHT, 24, 4, 2, 1);
        glow(g, 0x7BE9E2, 38, 34, 3, 11, 11);
        rect(g, 0x6ACDD4, 37, 6, 5, 4);
        rect(g, LIGHT, 38, 6, 3, 1);
        rect(g, 0x8AE7E2, 14, 25 + shift, 2, 2);
        rect(g, 0x8AE7E2, 34, 22 - shift, 2, 2);
        poly(g, 0x1D3B50, 13, 43, 19, 40, 27, 41, 33, 40, 38, 44, 33, 47, 16, 47, 11, 45);
        line(g, 0x4F7B85, 17, 43, 23, 42);
        line(g, 0x4F7B85, 27, 43, 32, 42);
        rect(g, 0x72D2DB, 16 + shift * 3, 45, 3, 1);
    }
}
