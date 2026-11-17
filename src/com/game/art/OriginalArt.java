package com.game.art;

import static com.game.art.PixelArt.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The game's original art direction, drawn from code rather than third-party sprites.
 * Every actor uses a common ink outline, stepped highlights and a restrained palette.
 * Old resource identifiers remain aliases so existing saves and animation code stay valid.
 */
public final class OriginalArt {
    private static final Pattern ACTOR = Pattern.compile(
            "(boy|astra|scout|nyx|engineer|orion|orc|briar|briarsentinel|skeletonlord(?:_phase2)?|warden(?:_phase2)?|eclipsewarden(?:_phase2)?)"
            + "_(attack_)?(up|down|left|right)_([12])\\.png");
    private static final Pattern WING = Pattern.compile("(?:bat|riftwing)_(?:up|down|left|right)_([12])\\.png");
    private static final Pattern MOSS = Pattern.compile("mossling_(?:(?:up|down|left|right)_)?([12])\\.png");

    private OriginalArt() { }

    /** Returns a new transparent native sprite, or null for resources outside this art set. */
    public static BufferedImage create(String name) {
        if (name == null) return null;
        String identifier = name.toLowerCase(Locale.ROOT);
        Matcher actor = ACTOR.matcher(identifier);
        if (actor.matches()) {
            String family = actor.group(1);
            int species = family.equals("scout") || family.equals("nyx") ? 3
                    : family.equals("engineer") || family.equals("orion") ? 4
                    : family.equals("boy") || family.equals("astra") ? 0
                    : family.equals("orc") || family.startsWith("briar") ? 1 : 2;
            return actor(species, actor.group(3), Integer.parseInt(actor.group(4)),
                    actor.group(2) != null, family.contains("phase2"));
        }
        Matcher wing = WING.matcher(identifier);
        if (wing.matches()) return riftwing(Integer.parseInt(wing.group(1)));
        Matcher moss = MOSS.matcher(identifier);
        if (moss.matches()) return mossling(Integer.parseInt(moss.group(1)));
        return switch (identifier) {
            case "icons8-minecraft-creeper-16.png", "mossling_1.png" -> mossling(1);
            case "icons8-minecraft-creeper-48.png", "mossling_2.png" -> mossling(2);
            case "002.png", "floor.png" -> TerrainArt.floor(0, 0);
            case "016.png", "tree.png" -> TerrainArt.obstacle(0, 0);
            case "mashroom.png", "mushroom.png", "poison-crystal.png" -> item(0);
            case "strawberry.png", "medpod.png" -> item(1);
            case "banana.png", "batteryfruit.png" -> item(2);
            case "dragonfruit.png", "starbloom.png" -> item(3);
            case "blueheart.png", "surgeorb.png" -> item(4);
            case "sword_normal.png", "starblade.png" -> item(5);
            case "heart_full.png" -> healthHeart(2);
            case "energy-full.png" -> energyCell(2);
            case "heart_half.png" -> healthHeart(1);
            case "energy-half.png" -> energyCell(1);
            case "heart_blank.png" -> healthHeart(0);
            case "energy-empty.png" -> energyCell(0);
            case "spaceship_1.png", "shuttle-flight.png" -> shuttle(true);
            case "spaceship_2.png", "shuttle-landed.png" -> shuttle(false);
            case "relay-dormant.png" -> relay(false);
            case "relay-active.png" -> relay(true);
            case "meat.png" -> item(6);
            case "metal.png" -> item(7);
            case "stone.png" -> item(8);
            case "rock.png" -> item(9);
            case "diamond.png" -> item(10);
            case "pickaxe.png" -> item(11);
            case "gun.png" -> item(12);
            case "metal-ore.png" -> resourceNode(0);
            case "stone-node.png" -> resourceNode(1);
            case "rock-node.png" -> resourceNode(2);
            case "diamond-node.png" -> resourceNode(3);
            default -> null;
        };
    }

    private static BufferedImage actor(int species, String facing, int frame,
            boolean attacking, boolean awakened) {
        boolean extended = attacking && frame == 2;
        boolean vertical = facing.equals("up") || facing.equals("down");
        int width = extended && !vertical ? 96 : 48;
        int height = extended && vertical ? 96 : 48;
        BufferedImage image = image(width, height);
        Graphics2D g = graphics(image);
        try {
            int offsetX = extended && facing.equals("left") ? 48 : 0;
            int offsetY = extended && facing.equals("up") ? 48 : 0;
            g.translate(offsetX, offsetY);
            glow(g, INK, 105, 9, 38, 32, 8);
            boolean mirror = facing.equals("left");
            if (mirror) { g.translate(48, 0); g.scale(-1, 1); }
            String direction = mirror ? "right" : facing;
            if (species == 0) astronaut(g, direction, frame, attacking);
            else if (species == 3) scout(g, direction, frame, attacking);
            else if (species == 4) engineer(g, direction, frame, attacking);
            else if (species == 1) sentinel(g, direction, frame);
            else warden(g, direction, frame, awakened);
            if (mirror) { g.scale(-1, 1); g.translate(-48, 0); }
            if (attacking) weapon(g, species, facing, frame, awakened);
        } finally { g.dispose(); }
        return image;
    }

    private static void scout(Graphics2D g, String direction, int frame, boolean attacking) {
        int step = attacking || frame == 1 ? 0 : 2;
        boolean back = direction.equals("up");
        boolean side = direction.equals("right");
        // Nyx carries a split lilac field cloak over a deliberately narrow ranger suit.
        poly(g, INK, 15, 20, 32, 20, 36, 37, 30, 35, 27, 40, 20, 36, 12, 39);
        poly(g, 0x665377, 17, 22, 30, 22, 33, 35, 28, 33, 26, 37, 20, 34, 15, 36);
        line(g, LILAC, 17, 24, 14, 35);
        rect(g, INK, 17, 32 + step, 7, 13 - step);
        rect(g, INK, 25, 34 - step, 7, 11 + step);
        rect(g, 0x315C68, 18, 34 + step, 5, 8 - step);
        rect(g, 0x537A81, 26, 34 - step, 5, 8);
        rect(g, 0xA5D1C0, 19, 34 + step, 2, 4);
        rect(g, INK, 16, 42 + step / 2, 9, 3);
        rect(g, INK, 25, 42 - step / 2, 9, 3);
        rect(g, TEAL, 18, 43 + step / 2, 5, 1);
        rect(g, LILAC, 27, 43 - step / 2, 5, 1);
        poly(g, INK, 17, 21, 30, 21, 33, 28, 29, 36, 18, 36, 15, 29);
        poly(g, 0x335B64, 18, 23, 28, 23, 30, 28, 27, 34, 19, 34, 17, 29);
        poly(g, 0x719D9B, 19, 23, 28, 23, 27, 27, 20, 27);
        rect(g, 0xBDDACA, 19, 24, 4, 2);
        line(g, LILAC, 19, 28, 28, 31);
        rect(g, INK, 18, 32, 12, 3);
        rect(g, AMBER, 22, 32, 3, 2);
        rect(g, INK, 11, 24 + step / 2, 6, 12);
        rect(g, 0x547F80, 12, 25 + step / 2, 4, 8);
        rect(g, 0x9AC6B5, 12, 25 + step / 2, 1, 5);
        rect(g, INK, 31, 24 - step / 2, 6, 12);
        rect(g, 0x72958D, 32, 25 - step / 2, 4, 8);
        rect(g, LILAC, 32, 31 - step / 2, 4, 2);
        // The angular hood and long visor distinguish Nyx from Astra's round helmet.
        poly(g, INK, 12, 12, 17, 5, 26, 2, 33, 7, 36, 15, 32, 23, 17, 24, 12, 19);
        poly(g, 0x594D6F, 14, 12, 18, 7, 26, 5, 31, 9, 33, 15, 30, 21, 18, 21, 14, 18);
        poly(g, 0x87769A, 15, 12, 19, 8, 25, 6, 29, 8, 23, 10, 19, 15);
        line(g, LILAC, 20, 7, 25, 6);
        if (back) {
            poly(g, 0x6E6387, 18, 11, 25, 8, 30, 12, 28, 19, 20, 20, 17, 16);
            line(g, LILAC, 25, 10, 26, 18);
            rect(g, INK, 20, 24, 9, 10);
            rect(g, 0x315B67, 21, 25, 7, 8);
            rect(g, TEAL, 23, 26, 2, 5);
        } else {
            poly(g, INK, side ? 23 : 17, 11, 32, 10, 34, 14, 31, 19, 18, 19, 16, 15);
            poly(g, 0x287C87, side ? 25 : 18, 12, 31, 12, 32, 14, 29, 17, 19, 17, 18, 15);
            line(g, TEAL, side ? 25 : 19, 12, 30, 12);
            line(g, LIGHT, side ? 28 : 20, 12, side ? 30 : 24, 12);
            rect(g, 0x123E52, 20, 16, 9, 1);
        }
        poly(g, 0xA48AB9, 17, 22, 30, 21, 32, 24, 24, 26, 17, 24);
        poly(g, 0xA48AB9, 29, 24, 34, 25, 39, 32 + step, 35, 32 + step, 31, 28);
        line(g, 0xE2CDF2, 19, 22, 27, 22);
        rect(g, TEAL, 12, 15, 1, 3);
    }

    private static void engineer(Graphics2D g, String direction, int frame, boolean attacking) {
        int step = attacking || frame == 1 ? 0 : 2;
        boolean back = direction.equals("up");
        boolean side = direction.equals("right");
        // Orion's broad hard-shell pack and industrial bracers are visibly heavier.
        rect(g, INK, 9, 18, 30, 19);
        rect(g, 0x6F675D, 11, 20, 26, 15);
        rect(g, 0xC09564, 12, 21, 4, 12);
        rect(g, INK, 14, 33 + step, 10, 12 - step);
        rect(g, INK, 25, 34 - step, 10, 11 + step);
        rect(g, 0xA78C6D, 16, 34 + step, 7, 8 - step);
        rect(g, 0xC5B397, 26, 34 - step, 7, 8);
        rect(g, 0x49575E, 14, 41 + step / 2, 11, 4);
        rect(g, 0x49575E, 25, 41 - step / 2, 11, 4);
        rect(g, AMBER, 16, 42 + step / 2, 6, 1);
        rect(g, AMBER, 28, 42 - step / 2, 6, 1);
        poly(g, INK, 13, 21, 33, 21, 37, 27, 35, 36, 27, 39, 16, 37, 11, 30);
        poly(g, 0xBE925C, 15, 23, 31, 23, 34, 28, 32, 34, 26, 36, 17, 34, 14, 29);
        poly(g, 0xF1CF91, 16, 23, 30, 23, 32, 27, 26, 30, 18, 28);
        rect(g, 0xFFF0CD, 17, 24, 6, 2);
        rect(g, INK, 17, 31, 16, 4);
        rect(g, 0x817568, 18, 32, 14, 2);
        rect(g, TEAL, 23, 32, 4, 2);
        rect(g, INK, 5, 23 + step / 2, 10, 13);
        rect(g, 0xA88253, 7, 25 + step / 2, 7, 8);
        rect(g, 0xE2C38C, 7, 25 + step / 2, 3, 6);
        rect(g, 0x425D66, 7, 32 + step / 2, 7, 4);
        rect(g, INK, 34, 23 - step / 2, 10, 13);
        rect(g, 0xD8B276, 35, 25 - step / 2, 7, 8);
        rect(g, 0xFFF0CD, 35, 25 - step / 2, 2, 5);
        rect(g, 0x425D66, 35, 32 - step / 2, 7, 4);
        rect(g, AMBER, 38, 33 - step / 2, 3, 1);
        poly(g, INK, 11, 9, 16, 3, 31, 3, 37, 9, 37, 20, 32, 25, 16, 25, 11, 20);
        poly(g, 0xB28C5C, 13, 10, 17, 6, 30, 6, 35, 10, 35, 19, 31, 22, 17, 22, 13, 19);
        poly(g, 0xF1D39A, 14, 10, 18, 6, 29, 6, 32, 9, 31, 12, 15, 13);
        rect(g, IVORY, 19, 6, 8, 2);
        rect(g, INK, 21, 3, 7, 5);
        rect(g, AMBER, 22, 4, 5, 2);
        if (back) {
            rect(g, 0x849586, 17, 12, 14, 6);
            rect(g, 0xCDD5B9, 18, 12, 11, 2);
            rect(g, INK, 17, 24, 17, 13);
            rect(g, 0x526F71, 18, 25, 15, 10);
            rect(g, 0xA7B29A, 19, 25, 3, 8);
            rect(g, AMBER, 25, 27, 5, 2);
            rect(g, TEAL, 25, 31, 5, 2);
        } else {
            poly(g, INK, side ? 23 : 16, 12, 34, 11, 35, 15, 32, 20, 17, 20, 15, 16);
            poly(g, 0x255E6C, side ? 25 : 17, 13, 32, 13, 33, 15, 30, 18, 18, 18, 17, 16);
            rect(g, 0x63C1BA, side ? 26 : 18, 13, side ? 6 : 12, 2);
            rect(g, LIGHT, side ? 29 : 19, 13, 3, 1);
            rect(g, 0xD5B17E, 17, 21, 14, 1);
        }
        rect(g, INK, 9, 13, 4, 8);
        rect(g, 0x7C9288, 10, 14, 2, 5);
        rect(g, AMBER, 10, 15, 1, 2);
        poly(g, IVORY, 14, 24, 18, 22, 20, 25, 17, 28, 13, 27);
        rect(g, 0x6B7C72, 15, 25, 2, 1);
    }

    private static void astronaut(Graphics2D g, String direction, int frame, boolean attacking) {
        int step = attacking ? 0 : frame == 1 ? 0 : 2;
        boolean back = direction.equals("up");
        boolean side = direction.equals("right");
        // A compact life-support unit and two independently shaded boots.
        rect(g, INK, 11, 21, 26, 15);
        rect(g, 0x487E83, 12, 22, 5, 12);
        rect(g, 0xA0B7AB, 15, 22, 3, 9);
        rect(g, INK, 16, 33 + step, 8, 11 - step);
        rect(g, INK, 25, 35 - step, 8, 9 + step);
        rect(g, 0xA8C1B5, 17, 34 + step, 6, 7 - step);
        rect(g, IVORY, 26, 35 - step, 6, 7);
        rect(g, 0x2E535F, 16, 41 + step / 2, 9, 4);
        rect(g, 0x2E535F, 25, 41 - step / 2, 9, 4);
        rect(g, TEAL, 18, 42 + step / 2, 5, 1);
        rect(g, LIGHT, 27, 42 - step / 2, 5, 1);
        poly(g, INK, 15, 23, 32, 23, 35, 30, 32, 37, 16, 37, 13, 30);
        poly(g, 0x9EBCAF, 17, 23, 30, 23, 32, 29, 30, 35, 17, 35, 15, 29);
        rect(g, IVORY, 17, 24, 12, 6);
        rect(g, 0x678E8A, 17, 32, 14, 3);
        rect(g, 0x294856, 19, 29, 10, 3);
        rect(g, TEAL, 20, 29, 4, 2);
        rect(g, AMBER, 26, 29, 2, 2);
        if (back) {
            rect(g, INK, 18, 23, 14, 14);
            rect(g, 0x39666E, 19, 24, 12, 12);
            rect(g, 0x749D97, 20, 24, 3, 10);
            rect(g, 0x1F414E, 24, 25, 6, 8);
            rect(g, TEAL, 25, 27, 4, 1);
            rect(g, TEAL, 25, 30, 4, 1);
        }
        // Arms stay separated from the body even at a one-tile display size.
        rect(g, INK, 10, 24 + step / 2, 7, 12);
        rect(g, 0xA0BCAD, 11, 25 + step / 2, 5, 8);
        rect(g, IVORY, 11, 25 + step / 2, 2, 6);
        rect(g, 0x325C66, 11, 33 + step / 2, 5, 3);
        rect(g, INK, 32, 24 - step / 2, 7, 12);
        rect(g, IVORY, 33, 25 - step / 2, 5, 8);
        rect(g, 0x325C66, 33, 33 - step / 2, 5, 3);
        // Helmet: stepped octagonal rim, side lamps and a glazed visor.
        poly(g, INK, 13, 8, 17, 4, 31, 4, 36, 9, 36, 20, 31, 24, 17, 24, 12, 19, 12, 10);
        poly(g, 0xA0BCB2, 14, 9, 18, 6, 30, 6, 34, 10, 34, 19, 30, 22, 18, 22, 14, 18);
        poly(g, IVORY, 15, 10, 19, 6, 29, 6, 32, 9, 31, 12, 16, 13);
        rect(g, 0x779E98, 12, 11, 3, 7);
        rect(g, AMBER, 12, 12, 1, 3);
        if (back) {
            rect(g, 0xCAD6C4, 17, 11, 14, 8);
            rect(g, 0x8CAEA3, 18, 17, 12, 3);
            rect(g, 0x4E8580, 23, 8, 3, 11);
            rect(g, LIGHT, 24, 8, 1, 6);
        } else if (side) {
            poly(g, INK, 24, 10, 34, 10, 36, 13, 34, 19, 25, 19, 22, 15);
            poly(g, 0x218D91, 25, 11, 33, 11, 34, 14, 32, 17, 25, 17, 24, 14);
            rect(g, 0xA1F3DA, 27, 11, 5, 2);
            rect(g, LIGHT, 32, 12, 1, 3);
            rect(g, 0x416F77, 16, 12, 5, 6);
            rect(g, TEAL, 17, 13, 2, 3);
        } else {
            poly(g, INK, 17, 11, 31, 11, 33, 14, 31, 20, 18, 20, 16, 16);
            poly(g, 0x217B83, 18, 12, 30, 12, 31, 14, 29, 18, 19, 18, 18, 16);
            rect(g, 0x63D1BA, 19, 12, 10, 2);
            rect(g, LIGHT, 20, 12, 5, 1);
            rect(g, 0x113B50, 20, 16, 9, 2);
            rect(g, TEAL, 29, 14, 1, 3);
        }
        // A warm scarf is Astra's identifying color against the cool forest.
        poly(g, INK, 16, 21, 32, 21, 35, 25, 28, 27, 16, 25);
        poly(g, ORANGE, 17, 22, 31, 22, 33, 24, 27, 25, 17, 24);
        rect(g, AMBER, 19, 22, 9, 1);
        poly(g, ORANGE, 30, 24, 36, 24, 42, 27 + step, 38, 30 + step, 33, 28);
        line(g, AMBER, 33, 25, 39, 28 + step);
    }

    private static void sentinel(Graphics2D g, String direction, int frame) {
        int step = frame == 2 ? 2 : 0;
        boolean back = direction.equals("up");
        boolean side = direction.equals("right");
        rect(g, INK, 12, 32, 10, 13);
        rect(g, INK, 27, 32, 10, 13);
        rect(g, 0x45665A, 14, 33 + step, 7, 10 - step);
        rect(g, 0x687967, 28, 33, 7, 10 - step);
        rect(g, 0x9EAC80, 29, 34, 2, 4);
        rect(g, 0x233F43, 12, 41 + step / 2, 11, 4);
        rect(g, 0x233F43, 26, 41 - step / 2, 12, 4);
        poly(g, INK, 9, 17, 18, 13, 32, 13, 39, 18, 38, 34, 31, 39, 17, 39, 9, 33);
        poly(g, 0x3F6057, 12, 19, 18, 15, 30, 15, 36, 20, 35, 32, 30, 36, 18, 36, 12, 31);
        poly(g, 0x6D8670, 14, 20, 18, 16, 29, 16, 33, 20, 29, 24, 16, 24);
        line(g, 0xA5B48C, 17, 18, 27, 18);
        rect(g, 0x233F43, 17, 30, 16, 5);
        rect(g, 0x78927C, 19, 33, 10, 1);
        poly(g, INK, 4, 20, 10, 17, 16, 21, 15, 35, 9, 39, 4, 34);
        poly(g, 0x567763, 6, 22, 10, 20, 13, 23, 12, 33, 9, 36, 6, 33);
        rect(g, 0x91A17A, 7, 23, 2, 6);
        poly(g, INK, 34, 20, 41, 18, 45, 22, 44, 35, 39, 39, 34, 35);
        poly(g, 0x6B836B, 36, 23, 40, 21, 42, 23, 41, 34, 38, 36, 36, 33);
        rect(g, 0x9CA980, 37, 23, 2, 6);
        // A lantern-like ancient machine face, framed by a living briar crown.
        poly(g, INK, 13, 8, 19, 3, 31, 4, 36, 10, 34, 22, 28, 26, 18, 23, 13, 17);
        poly(g, 0x6F7A65, 16, 9, 20, 6, 29, 7, 33, 11, 31, 21, 26, 23, 19, 21, 16, 17);
        if (back) {
            rect(g, 0x2C4B48, 20, 10, 9, 10);
            line(g, 0xA0AA7E, 22, 10, 22, 18);
        } else {
            poly(g, 0x293F42, 18, 12, 31, 11, 31, 20, 25, 22, 19, 19);
            rect(g, AMBER, side ? 27 : 20, 14, side ? 4 : 3, 2);
            if (!side) rect(g, AMBER, 27, 14, 3, 2);
            rect(g, 0xA28258, 22, 18, 5, 2);
            rect(g, IVORY, side ? 29 : 21, 14, 1, 1);
        }
        glow(g, AMBER, 55, 17, 23, 17, 10);
        poly(g, INK, 24, 23, 30, 27, 26, 33, 20, 28);
        poly(g, AMBER, 24, 25, 28, 27, 25, 30, 22, 28);
        rect(g, IVORY, 24, 26, 2, 2);
        poly(g, 0x2C6A50, 9, 19, 5, 13, 12, 14, 15, 8, 20, 11, 25, 4, 31, 8, 37, 6, 36, 16, 42, 19, 32, 20, 27, 15, 21, 17, 15, 16);
        line(g, 0x71B176, 10, 15, 16, 15);
        line(g, 0x71B176, 24, 8, 28, 12);
        rect(g, 0xA3D38F, 16, 11, 2, 2);
        poly(g, 0x397B58, 8, 28, 13, 30, 11, 34, 16, 36, 12, 39, 7, 34);
        rect(g, TEAL, 9, 29, 1, 2);
    }

    private static void warden(Graphics2D g, String direction, int frame, boolean awakened) {
        int step = frame == 2 ? 2 : 0;
        int edge = awakened ? 0xF18AA8 : LILAC;
        int middle = awakened ? 0x9B487B : 0x685588;
        boolean back = direction.equals("up");
        boolean side = direction.equals("right");
        glow(g, edge, 27, 2, 8, 44, 38);
        // The split cape gives the knight a silhouette unlike the other guardians.
        poly(g, INK, 12, 18, 35, 18, 43, 41, 34, 39, 31, 45, 24, 41, 18, 45, 7, 42);
        poly(g, 0x322C51, 14, 20, 33, 20, 39, 40, 33, 37, 30, 42, 24, 38, 18, 41, 11, 40);
        line(g, middle, 14, 24, 11, 39);
        line(g, middle, 33, 23, 38, 38);
        rect(g, INK, 16, 33 + step, 8, 11 - step);
        rect(g, INK, 26, 34 - step, 8, 10 + step);
        rect(g, 0x555471, 17, 35 + step, 6, 7 - step);
        rect(g, 0x77718B, 27, 34 - step, 6, 8);
        rect(g, edge, 18, 37 + step, 2, 2);
        rect(g, edge, 28, 36 - step, 2, 2);
        poly(g, INK, 10, 20, 17, 15, 31, 15, 38, 20, 36, 34, 29, 39, 18, 38, 11, 32);
        poly(g, 0x494562, 13, 21, 18, 17, 29, 17, 35, 22, 33, 32, 28, 36, 19, 35, 14, 31);
        poly(g, 0x77708E, 15, 21, 19, 18, 27, 18, 30, 21, 24, 26, 18, 24);
        line(g, edge, 17, 21, 23, 24);
        poly(g, INK, 5, 19, 12, 15, 19, 19, 15, 25, 11, 35, 5, 33);
        poly(g, middle, 7, 20, 12, 17, 16, 20, 12, 24, 9, 32, 7, 31);
        line(g, edge, 8, 20, 12, 18);
        poly(g, INK, 30, 19, 37, 15, 44, 20, 43, 34, 37, 36, 33, 27);
        poly(g, middle, 33, 21, 37, 18, 41, 21, 40, 32, 38, 33, 36, 25);
        line(g, edge, 36, 20, 39, 21);
        // Obsidian helmet, crescent antlers and a narrow star-lit face slit.
        poly(g, INK, 14, 5, 20, 7, 28, 7, 35, 4, 34, 17, 29, 24, 20, 24, 14, 17);
        poly(g, 0x504660, 16, 8, 21, 9, 28, 9, 32, 7, 32, 16, 28, 21, 21, 21, 16, 16);
        poly(g, 0x8C7AA6, 16, 8, 19, 9, 23, 14, 21, 20, 18, 16);
        poly(g, middle, 29, 9, 31, 8, 30, 16, 27, 20, 26, 14);
        if (!back) {
            rect(g, INK, side ? 25 : 19, 13, side ? 8 : 11, 4);
            rect(g, edge, side ? 27 : 20, 14, side ? 5 : 9, 1);
            rect(g, LIGHT, side ? 29 : 24, 14, 1, 1);
            rect(g, INK, 24, 17, 2, 4);
        }
        poly(g, INK, 14, 11, 9, 7, 8, 1, 13, 3, 18, 10);
        poly(g, middle, 14, 9, 11, 6, 10, 3, 13, 5, 16, 10);
        poly(g, INK, 31, 9, 36, 2, 40, 0, 39, 7, 34, 13);
        poly(g, middle, 33, 9, 37, 4, 38, 3, 37, 7, 34, 11);
        spark(g, edge, 24, 28, 3);
        rect(g, LIGHT, 24, 27, 1, 2);
    }

    private static void weapon(Graphics2D g, int species, String facing, int frame, boolean awakened) {
        int color = species == 0 ? TEAL : species == 1 || species == 4 ? AMBER
                : species == 3 ? LILAC : awakened ? 0xF18AA8 : LILAC;
        if (frame == 1) {
            // Wind-up pose fits the normal frame and signals the coming strike.
            poly(g, INK, 35, 12, 39, 11, 42, 28, 37, 29);
            poly(g, color, 37, 13, 38, 13, 40, 26, 38, 27);
            rect(g, AMBER, 34, 27, 9, 3);
            rect(g, INK, 37, 30, 3, 5);
            return;
        }
        Graphics2D blade = (Graphics2D) g.create();
        try {
            // Draw one carefully shaded right-facing blade, then rotate it per direction.
            blade.translate(24, 24);
            switch (facing) {
                case "down" -> blade.rotate(Math.PI / 2);
                case "left" -> blade.rotate(Math.PI);
                case "up" -> blade.rotate(-Math.PI / 2);
                default -> { }
            }
            blade.translate(-24, -24);
            glow(blade, color, 26, 41, 4, 47, 39);
            // Three stepped bands describe a curved energy wake without blurring pixels.
            poly(blade, color, 52, 5, 67, 9, 81, 19, 88, 31, 90, 44, 86, 39, 82, 28, 74, 18, 62, 11);
            poly(blade, LIGHT, 63, 10, 74, 15, 83, 24, 86, 32, 81, 25, 72, 18);
            poly(blade, INK, 37, 21, 74, 20, 87, 24, 74, 29, 37, 28);
            poly(blade, color, 40, 23, 73, 22, 82, 24, 73, 27, 40, 26);
            poly(blade, LIGHT, 42, 23, 73, 22, 80, 24, 45, 24);
            rect(blade, 0x3C7073, 43, 26, 30, 1);
            rect(blade, INK, 34, 17, 6, 16);
            rect(blade, AMBER, 35, 19, 4, 12);
            rect(blade, IVORY, 36, 20, 1, 7);
            rect(blade, INK, 28, 22, 8, 6);
            rect(blade, 0x7B6A59, 29, 23, 6, 3);
            spark(blade, color, 75, 36, 1);
            spark(blade, LIGHT, 83, 12, 2);
            rect(blade, color, 62, 3, 2, 2);
        } finally { blade.dispose(); }
    }

    private static BufferedImage mossling(int frame) {
        BufferedImage image = image(48, 48);
        Graphics2D g = graphics(image);
        int bounce = frame == 2 ? 2 : 0;
        try {
            glow(g, INK, 95, 7, 38, 35, 8);
            glow(g, TEAL, 24, 5, 20, 39, 25);
            poly(g, INK, 7, 29 + bounce, 10, 20 + bounce, 17, 16 + bounce, 31, 16 + bounce,
                    38, 22 + bounce, 42, 33, 38, 41, 10, 41, 5, 36);
            poly(g, 0x2B7660, 9, 30 + bounce, 12, 22 + bounce, 18, 19 + bounce, 30, 19 + bounce,
                    36, 24 + bounce, 39, 33, 36, 38, 11, 38, 8, 35);
            poly(g, 0x58AA77, 12, 27 + bounce, 17, 21 + bounce, 29, 21 + bounce,
                    34, 25 + bounce, 31, 29 + bounce, 18, 30 + bounce);
            rect(g, 0x8AD59A, 16, 22 + bounce, 6, 2);
            poly(g, 0x174E4E, 10, 35, 16, 36, 22, 34, 29, 36, 36, 34, 36, 38, 11, 38);
            // Sprouting leaves and a tiny amber seed are the Mossling's crest.
            poly(g, INK, 23, 20 + bounce, 15, 14 + bounce, 15, 8 + bounce, 21, 10 + bounce,
                    25, 15 + bounce, 28, 8 + bounce, 36, 5 + bounce, 35, 13 + bounce, 29, 20 + bounce);
            poly(g, 0x69B783, 23, 17 + bounce, 17, 13 + bounce, 17, 10 + bounce, 21, 12 + bounce,
                    25, 18 + bounce, 29, 10 + bounce, 33, 8 + bounce, 32, 13 + bounce, 28, 19 + bounce);
            line(g, 0xB3DEA2, 28, 16 + bounce, 32, 10 + bounce);
            rect(g, INK, 14, 29 + bounce, 6, 6);
            rect(g, INK, 29, 29 + bounce, 6, 6);
            rect(g, LIGHT, 16, 30 + bounce, 3, 3);
            rect(g, LIGHT, 30, 30 + bounce, 3, 3);
            rect(g, TEAL, 16, 33 + bounce, 3, 1);
            rect(g, TEAL, 30, 33 + bounce, 3, 1);
            line(g, 0x8BC39B, 23, 35 + bounce, 26, 35 + bounce);
            rect(g, AMBER, 25, 14 + bounce, 2, 3);
        } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage riftwing(int frame) {
        BufferedImage image = image(48, 48);
        Graphics2D g = graphics(image);
        boolean spread = frame == 1;
        try {
            glow(g, INK, 75, 10, 38, 28, 7);
            glow(g, LILAC, 22, 4, 8, 40, 34);
            int wingY = spread ? 8 : 19;
            poly(g, INK, 22, 18, 12, wingY, 1, wingY - 2, 4, 25, 11, 31, 20, 28,
                    24, 33, 29, 28, 38, 31, 45, 25, 47, wingY - 2, 36, wingY, 26, 18);
            poly(g, 0x3B446E, 21, 20, 11, wingY + 3, 4, wingY + 1, 7, 24, 12, 28, 20, 25,
                    24, 30, 29, 25, 37, 28, 42, 24, 44, wingY + 1, 37, wingY + 3, 27, 20);
            poly(g, 0x716496, 19, 22, 10, wingY + 5, 6, wingY + 4, 10, 24, 14, 25);
            poly(g, 0x9881B8, 29, 22, 38, wingY + 5, 42, wingY + 4, 38, 24, 34, 25);
            line(g, TEAL, 4, wingY + 1, 11, wingY + 3);
            line(g, TEAL, 11, wingY + 3, 20, 21);
            line(g, 0x93E5D3, 28, 21, 37, wingY + 3);
            line(g, 0x93E5D3, 37, wingY + 3, 44, wingY + 1);
            poly(g, INK, 19, 13, 25, 9, 30, 15, 29, 26, 25, 34, 21, 29, 19, 21);
            poly(g, 0x3D717A, 21, 15, 25, 12, 28, 16, 27, 25, 25, 30, 23, 26);
            poly(g, TEAL, 24, 12, 26, 15, 25, 25, 23, 20);
            rect(g, INK, 19, 19, 4, 4);
            rect(g, INK, 27, 19, 4, 4);
            rect(g, AMBER, 20, 20, 2, 1);
            rect(g, AMBER, 28, 20, 2, 1);
            line(g, 0x8D7BBA, 25, 31, 29, 38);
            line(g, LILAC, 29, 38, 27, 42);
            poly(g, LILAC, 25, 40, 28, 38, 30, 41, 27, 44);
            rect(g, LIGHT, 27, 40, 1, 2);
        } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage item(int kind) {
        BufferedImage image = image(32, 32);
        Graphics2D g = graphics(image);
        try {
            glow(g, INK, 100, 4, 24, 25, 6);
            switch (kind) {
                case 0 -> poisonCrystal(g);
                case 1 -> medpod(g);
                case 2 -> batteryfruit(g);
                case 3 -> starbloom(g);
                case 4 -> surgeorb(g);
                case 5 -> starblade(g);
                case 6 -> meat(g);
                case 7 -> metal(g);
                case 8 -> stone(g, false);
                case 9 -> stone(g, true);
                case 10 -> diamond(g);
                case 11 -> pickaxe(g);
                case 12 -> gun(g);
                default -> throw new IllegalArgumentException("Unknown item kind");
            }
        } finally { g.dispose(); }
        return image;
    }

    private static void poisonCrystal(Graphics2D g) {
        glow(g, 0xC66DA0, 35, 3, 4, 26, 25);
        poly(g, INK, 5, 22, 5, 15, 11, 11, 15, 15, 17, 3, 23, 8, 26, 22, 21, 28, 10, 28);
        poly(g, 0x7A447F, 16, 20, 18, 6, 21, 9, 23, 22, 19, 25);
        poly(g, 0xD491B5, 18, 7, 19, 11, 18, 20, 17, 21);
        poly(g, 0x9A568A, 7, 16, 11, 13, 16, 24, 11, 25, 7, 21);
        line(g, 0xE5A7C3, 9, 16, 12, 20);
        rect(g, 0x57365E, 10, 26, 13, 2);
        spark(g, 0xE4A1BA, 25, 5, 1);
    }

    private static void medpod(Graphics2D g) {
        glow(g, TEAL, 30, 2, 4, 28, 25);
        poly(g, INK, 10, 5, 21, 5, 26, 11, 24, 23, 19, 28, 11, 26, 6, 18, 7, 10);
        poly(g, 0xB7D6BE, 11, 7, 20, 7, 24, 12, 22, 22, 18, 25, 12, 24, 8, 18, 9, 11);
        poly(g, IVORY, 11, 8, 19, 8, 21, 11, 10, 13);
        rect(g, 0x3E8071, 10, 13, 12, 8);
        rect(g, TEAL, 14, 13, 4, 8);
        rect(g, TEAL, 11, 15, 10, 3);
        rect(g, LIGHT, 15, 14, 1, 5);
        poly(g, 0x3C7959, 13, 7, 10, 2, 17, 3, 20, 1, 20, 6);
        line(g, 0x8EC893, 14, 4, 17, 6);
        rect(g, 0x678F7D, 12, 23, 7, 2);
    }

    private static void batteryfruit(Graphics2D g) {
        glow(g, AMBER, 30, 3, 5, 26, 24);
        poly(g, INK, 13, 4, 21, 5, 25, 14, 23, 24, 18, 28, 9, 24, 6, 17, 8, 8);
        poly(g, 0xB97844, 13, 6, 20, 7, 23, 15, 21, 23, 17, 25, 10, 22, 8, 16, 10, 9);
        poly(g, AMBER, 13, 7, 17, 7, 16, 14, 20, 14, 13, 24, 14, 17, 10, 17);
        line(g, IVORY, 13, 9, 15, 9);
        poly(g, 0x427B61, 15, 6, 10, 3, 13, 0, 17, 3, 22, 2, 22, 6);
        rect(g, 0x83BC83, 15, 3, 2, 3);
    }

    private static void starbloom(Graphics2D g) {
        glow(g, AMBER, 25, 1, 1, 30, 30);
        poly(g, INK, 16, 1, 20, 9, 29, 7, 26, 16, 30, 23, 21, 24,
                16, 31, 12, 24, 3, 24, 7, 16, 3, 8, 12, 10);
        poly(g, 0xDF875F, 16, 4, 19, 12, 26, 10, 23, 17, 26, 21, 19, 22,
                16, 27, 13, 21, 7, 22, 10, 16, 7, 11, 13, 13);
        poly(g, AMBER, 16, 7, 18, 14, 23, 13, 21, 17, 22, 20, 18, 20,
                16, 24, 14, 20, 10, 20, 12, 16, 11, 14, 15, 15);
        oval(g, INK, 12, 12, 9, 9);
        oval(g, TEAL, 13, 13, 7, 7);
        rect(g, LIGHT, 15, 14, 2, 2);
        line(g, 0x9BD7A0, 8, 25, 12, 23);
        rect(g, AMBER, 27, 3, 1, 2);
    }

    private static void surgeorb(Graphics2D g) {
        glow(g, TEAL, 30, 1, 1, 30, 30);
        poly(g, INK, 10, 5, 21, 4, 28, 12, 27, 22, 20, 28, 10, 27, 4, 19, 4, 11);
        poly(g, 0x267A88, 11, 7, 20, 6, 26, 13, 25, 21, 19, 26, 11, 25, 6, 18, 6, 12);
        poly(g, 0x5ABDB4, 11, 8, 19, 7, 23, 11, 21, 16, 10, 20, 7, 16, 8, 11);
        poly(g, LIGHT, 15, 8, 18, 12, 23, 15, 18, 18, 15, 24, 13, 18, 9, 15, 13, 12);
        rect(g, 0xE8FFF1, 15, 12, 2, 7);
        line(g, TEAL, 22, 20, 25, 18);
        spark(g, LIGHT, 26, 5, 1);
    }

    private static void starblade(Graphics2D g) {
        glow(g, TEAL, 28, 2, 1, 28, 29);
        poly(g, INK, 8, 24, 10, 18, 22, 5, 28, 2, 29, 8, 16, 21, 11, 25);
        poly(g, TEAL, 11, 19, 23, 6, 26, 5, 26, 8, 15, 20);
        line(g, LIGHT, 12, 19, 25, 6);
        poly(g, INK, 6, 16, 16, 25, 13, 28, 3, 19);
        line(g, AMBER, 6, 18, 14, 25);
        line(g, IVORY, 7, 18, 12, 22);
        poly(g, INK, 6, 21, 10, 25, 6, 30, 2, 27);
        line(g, 0xB0805C, 7, 24, 4, 27);
        rect(g, AMBER, 3, 27, 2, 2);
        spark(g, LIGHT, 28, 13, 1);
    }

    private static void meat(Graphics2D g) {
        line(g, 0xE8E4C5, 7, 25, 23, 10); oval(g, IVORY, 3, 22, 8, 7); oval(g, IVORY, 20, 5, 8, 8);
        poly(g, INK, 7, 8, 18, 5, 26, 12, 23, 22, 14, 26, 6, 19);
        poly(g, 0xAA4D61, 9, 10, 18, 7, 23, 13, 20, 21, 14, 23, 8, 18);
        line(g, 0xEF9C93, 11, 12, 18, 10); line(g, 0xEF9C93, 12, 18, 20, 15);
    }
    private static void metal(Graphics2D g) {
        poly(g, INK, 3, 16, 10, 7, 25, 7, 29, 17, 24, 25, 6, 25);
        poly(g, 0x6E9CA8, 5, 16, 11, 9, 23, 9, 26, 17, 22, 22, 7, 22);
        poly(g, 0xC3D8D1, 6, 15, 12, 10, 23, 10, 24, 15);
        line(g, 0x355A70, 8, 18, 22, 18); line(g, LIGHT, 12, 11, 20, 11);
    }
    private static void stone(Graphics2D g, boolean rough) {
        poly(g, INK, 3, 20, 7, 10, 18, 5, 27, 12, 29, 23, 21, 28, 8, 27);
        poly(g, rough ? 0x79615D : 0x8A9CA4, 5, 20, 9, 12, 18, 8, 24, 13, 26, 22, 20, 25, 9, 24);
        poly(g, rough ? 0xB59075 : 0xC7D2CF, 9, 13, 18, 9, 23, 14, 16, 18, 7, 19);
        line(g, 0x435764, 16, 19, 23, 21);
    }
    private static void diamond(Graphics2D g) {
        glow(g, TEAL, 40, 0, 0, 32, 32);
        poly(g, INK, 5, 10, 11, 4, 22, 4, 29, 11, 16, 29);
        poly(g, 0x56BCCD, 8, 11, 12, 7, 21, 7, 25, 11, 16, 25);
        poly(g, LIGHT, 12, 8, 20, 8, 18, 13, 13, 13);
        poly(g, 0xA8F9EC, 13, 14, 18, 14, 16, 24);
        line(g, TEAL, 9, 12, 24, 12); spark(g, IVORY, 27, 3, 1);
    }
    private static void pickaxe(Graphics2D g) {
        poly(g, INK, 5, 28, 3, 25, 21, 7, 25, 10);
        line(g, 0xD6A274, 6, 25, 22, 9);
        poly(g, INK, 6, 4, 18, 3, 27, 12, 29, 22, 24, 16, 17, 9, 7, 8);
        poly(g, 0xAACED0, 8, 5, 17, 5, 25, 13, 26, 17, 17, 7, 8, 7);
        line(g, LIGHT, 10, 5, 16, 5);
    }
    private static void gun(Graphics2D g) {
        poly(g, INK, 3, 9, 27, 9, 29, 13, 26, 17, 17, 17, 14, 27, 8, 27, 9, 17, 3, 17);
        rect(g, 0x6E94A2, 5, 11, 22, 4); rect(g, 0xD2DDD2, 5, 10, 18, 2);
        poly(g, 0xA07253, 10, 17, 15, 17, 12, 25, 9, 25);
        rect(g, TEAL, 20, 12, 7, 2); rect(g, AMBER, 7, 12, 3, 2);
    }
    private static BufferedImage resourceNode(int kind) {
        BufferedImage image = image(48, 48); Graphics2D g = graphics(image);
        try {
            g.drawImage(item(kind == 0 ? 7 : kind == 1 ? 8 : kind == 2 ? 9 : 10), 2, 5, 44, 40, null);
            if (kind == 3) { g.setColor(new java.awt.Color(50, 65, 78)); g.fillPolygon(new int[]{3,12,25,40,44,35,11},new int[]{36,18,14,23,39,46,46},7); g.drawImage(item(10), 14, 4, 25, 25, null); }
        } finally { g.dispose(); } return image;
    }

    private static BufferedImage healthHeart(int filled) {
        BufferedImage image = image(24, 24); Graphics2D g = graphics(image);
        int[] x = {2,5,9,12,15,19,22,22,12,2};
        int[] y = {8,4,4,7,4,4,8,13,23,13};
        try {
            g.setColor(new java.awt.Color(0x142735));g.fillPolygon(x,y,x.length);
            g.setColor(new java.awt.Color(0x658080));g.drawPolygon(x,y,x.length);
            if(filled>0) {
                if(filled==1)g.setClip(0,0,12,24);
                g.setColor(new java.awt.Color(0xEA947E));g.fillPolygon(x,y,x.length);
                g.setColor(new java.awt.Color(0xFFE0B8));g.fillRect(5,6,4,2);
                g.setColor(new java.awt.Color(0xAD5A69));g.drawLine(5,14,12,21);
            }
        } finally { g.dispose(); }return image;
    }

    private static BufferedImage energyCell(int charge) {
        BufferedImage image = image(24, 24);
        Graphics2D g = graphics(image);
        try {
            rect(g, INK, 3, 4, 18, 17);
            rect(g, 0x597D81, 4, 5, 16, 15);
            rect(g, 0xAAC3B5, 5, 5, 13, 1);
            rect(g, INK, 6, 8, 12, 9);
            rect(g, 0x173A46, 7, 9, 10, 7);
            if (charge > 0) {
                rect(g, charge == 1 ? AMBER : TEAL, 7, 9, charge == 1 ? 4 : 10, 7);
                rect(g, charge == 1 ? IVORY : LIGHT, 7, 9, charge == 1 ? 4 : 9, 1);
                rect(g, charge == 1 ? 0xB77F4E : 0x2D9B8A, 7, 15, charge == 1 ? 4 : 10, 1);
            }
            rect(g, 0x73948C, 21, 10, 2, 6);
            rect(g, 0x254653, 4, 18, 16, 2);
            rect(g, charge == 0 ? 0x4A6970 : AMBER, 7, 18, 3, 1);
            rect(g, 0xA2B8AA, 2, 9, 1, 7);
        } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage shuttle(boolean flying) {
        BufferedImage image = image(128, 96);
        Graphics2D g = graphics(image);
        try {
            glow(g, INK, 105, 9, 65, 111, 22);
            if (flying) {
                glow(g, ORANGE, 40, 24, 55, 80, 40);
                poly(g, 0xAA5742, 29, 62, 43, 62, 39, 88, 34, 95, 31, 84);
                poly(g, ORANGE, 31, 63, 41, 63, 37, 86, 35, 90, 33, 82);
                poly(g, AMBER, 33, 63, 39, 63, 36, 82);
                poly(g, IVORY, 34, 63, 38, 63, 36, 74);
                poly(g, 0xAA5742, 85, 62, 99, 62, 97, 85, 92, 95, 88, 88);
                poly(g, ORANGE, 87, 63, 97, 63, 95, 84, 92, 90, 89, 82);
                poly(g, AMBER, 89, 63, 95, 63, 92, 82);
                poly(g, IVORY, 90, 63, 94, 63, 92, 74);
            } else {
                poly(g, INK, 28, 57, 38, 57, 35, 76, 21, 80, 20, 75, 29, 71);
                poly(g, 0x6B8E8B, 30, 61, 35, 61, 32, 73, 24, 76, 24, 74, 31, 70);
                poly(g, INK, 91, 57, 100, 57, 99, 71, 108, 75, 107, 80, 94, 76);
                poly(g, 0x6B8E8B, 93, 61, 98, 61, 97, 70, 104, 74, 104, 76, 96, 73);
            }
            // A crescent swept wing folds around a narrow explorer cockpit.
            poly(g, INK, 4, 34, 15, 20, 27, 23, 38, 36, 49, 39, 54, 18,
                    64, 8, 74, 18, 79, 39, 90, 36, 101, 23, 113, 20, 124, 34,
                    118, 62, 105, 71, 83, 68, 72, 61, 56, 61, 45, 68, 23, 71, 10, 62);
            poly(g, 0x5D898C, 7, 35, 16, 24, 26, 26, 36, 39, 51, 43, 56, 20,
                    64, 12, 72, 20, 77, 43, 92, 39, 102, 26, 112, 24, 121, 35,
                    115, 59, 104, 67, 85, 65, 72, 57, 56, 57, 43, 65, 24, 67, 13, 59);
            poly(g, 0xC8D9C7, 8, 35, 16, 25, 24, 28, 34, 41, 50, 46, 49, 51,
                    32, 48, 21, 37, 15, 37, 14, 51, 11, 52);
            poly(g, IVORY, 16, 26, 24, 29, 32, 40, 22, 34, 17, 34, 14, 38, 11, 38);
            poly(g, 0xA4C0B4, 79, 46, 94, 41, 103, 28, 112, 25, 120, 35,
                    117, 49, 114, 50, 111, 37, 106, 37, 96, 49, 81, 52);
            poly(g, IVORY, 104, 29, 111, 27, 117, 35, 113, 35, 108, 33, 100, 42);
            poly(g, 0x2C555F, 16, 49, 25, 58, 44, 57, 43, 63, 25, 64, 16, 58);
            poly(g, 0x2C555F, 112, 49, 104, 58, 84, 57, 85, 63, 103, 64, 112, 58);
            poly(g, 0xD8E3CC, 57, 21, 64, 14, 70, 21, 74, 45, 70, 57, 58, 57, 53, 45);
            poly(g, 0xF4F0D8, 59, 22, 64, 17, 67, 22, 63, 48, 57, 50, 55, 44);
            poly(g, INK, 58, 25, 64, 20, 69, 25, 71, 39, 66, 46, 59, 44, 56, 38);
            poly(g, 0x20777E, 60, 26, 64, 23, 67, 26, 68, 37, 65, 42, 60, 40, 59, 36);
            poly(g, TEAL, 61, 26, 64, 24, 66, 27, 64, 36, 60, 37);
            line(g, LIGHT, 62, 27, 64, 26);
            line(g, LIGHT, 60, 29, 60, 34);
            rect(g, 0x304D59, 29, 54, 15, 11);
            rect(g, 0x304D59, 85, 54, 14, 11);
            rect(g, flying ? AMBER : 0x3AA68F, 32, 59, 9, 5);
            rect(g, flying ? AMBER : 0x3AA68F, 88, 59, 8, 5);
            rect(g, flying ? IVORY : TEAL, 33, 59, 7, 1);
            rect(g, flying ? IVORY : TEAL, 89, 59, 6, 1);
            line(g, TEAL, 18, 46, 25, 53);
            line(g, TEAL, 110, 46, 103, 53);
            rect(g, ORANGE, 48, 49, 6, 3);
            rect(g, ORANGE, 74, 49, 6, 3);
            rect(g, INK, 59, 51, 10, 5);
            rect(g, AMBER, 61, 52, 6, 2);
            // Small registration stripes and corner lamps finish the hull.
            line(g, 0x477879, 27, 40, 34, 45);
            line(g, 0x477879, 101, 40, 94, 45);
            rect(g, LIGHT, 11, 34, 2, 3);
            rect(g, LIGHT, 115, 34, 2, 3);
            rect(g, AMBER, 23, 60, 2, 2);
            rect(g, AMBER, 103, 60, 2, 2);
        } finally { g.dispose(); }
        return image;
    }

    private static BufferedImage relay(boolean active) {
        BufferedImage image = image(64, 64);
        Graphics2D g = graphics(image);
        int energy = active ? TEAL : AMBER;
        try {
            glow(g, energy, active ? 55 : 15, 1, 5, 62, 57);
            poly(g, INK, 10, 49, 18, 43, 44, 43, 54, 49, 54, 56, 42, 61, 21, 61, 10, 56);
            poly(g, 0x395862, 13, 50, 20, 46, 43, 46, 51, 50, 50, 54, 41, 58, 22, 58, 13, 54);
            poly(g, 0x8BA59D, 17, 48, 23, 45, 41, 45, 47, 49, 41, 53, 22, 53);
            poly(g, INK, 23, 19, 32, 9, 41, 19, 41, 47, 32, 53, 23, 47);
            poly(g, 0x536D75, 26, 20, 32, 13, 38, 20, 38, 45, 32, 49, 26, 45);
            poly(g, 0xA8BCB0, 27, 21, 32, 15, 32, 47, 27, 44);
            rect(g, INK, 29, 24, 7, 18);
            rect(g, energy, 31, 25, 3, 15);
            rect(g, LIGHT, 31, 25, 1, active ? 13 : 5);
            poly(g, INK, 11, 16, 17, 11, 23, 19, 23, 38, 17, 43, 11, 38);
            poly(g, 0x3C6570, 14, 17, 17, 15, 20, 20, 20, 36, 17, 39, 14, 36);
            rect(g, energy, 16, 21, 2, 13);
            poly(g, INK, 41, 19, 47, 11, 53, 16, 53, 38, 47, 43, 41, 38);
            poly(g, 0x3C6570, 44, 20, 47, 15, 50, 17, 50, 36, 47, 39, 44, 36);
            rect(g, energy, 46, 21, 2, 13);
            if (active) {
                poly(g, energy, 32, 1, 39, 8, 32, 15, 25, 8);
                poly(g, LIGHT, 32, 4, 35, 8, 32, 12, 29, 8);
                spark(g, TEAL, 8, 8, 2);
                spark(g, TEAL, 55, 7, 1);
            } else {
                rect(g, energy, 29, 6, 6, 3);
            }
            rect(g, energy, 19, 54, 4, 2);
            rect(g, energy, 41, 54, 4, 2);
        } finally { g.dispose(); }
        return image;
    }
}
