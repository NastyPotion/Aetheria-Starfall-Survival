package com.game.art;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Optional headless exporter for reviewing the original art independently of gameplay. */
public final class ArtGallery {
    private record Cell(String label, BufferedImage sprite) { }

    private ArtGallery() { }

    public static void main(String[] arguments) throws IOException {
        List<Cell> cells = new ArrayList<>();
        add(cells, "ASTRA / FRONT", "boy_down_1.png");
        add(cells, "ASTRA / STEP", "boy_down_2.png");
        add(cells, "ASTRA / PROFILE", "boy_right_1.png");
        add(cells, "ASTRA / REVERSE", "boy_up_1.png");
        add(cells, "STARBLADE / SWING", "boy_attack_right_2.png");
        add(cells, "STARBLADE / UP", "boy_attack_up_2.png");
        add(cells, "NYX / SCOUT", "scout_down_1.png");
        add(cells, "NYX / PROFILE", "scout_right_1.png");
        add(cells, "NYX / REVERSE", "scout_up_1.png");
        add(cells, "ORION / ENGINEER", "engineer_down_1.png");
        add(cells, "ORION / PROFILE", "engineer_right_1.png");
        add(cells, "ORION / REVERSE", "engineer_up_1.png");
        add(cells, "MOSSLING", "mossling_1.png");
        add(cells, "RIFTWING / GLIDE", "bat_down_1.png");
        add(cells, "RIFTWING / BEAT", "bat_down_2.png");
        add(cells, "BRIAR SENTINEL", "orc_down_1.png");
        add(cells, "SENTINEL / SWING", "orc_attack_right_2.png");
        add(cells, "ECLIPSE WARDEN", "skeletonlord_down_1.png");
        add(cells, "WARDEN / PROFILE", "skeletonlord_right_1.png");
        add(cells, "WARDEN / SWING", "skeletonlord_attack_right_2.png");
        add(cells, "WARDEN / AWAKENED", "skeletonlord_phase2_down_1.png");
        add(cells, "SHUTTLE / LANDED", "spaceship_2.png");
        add(cells, "SHUTTLE / THRUST", "spaceship_1.png");
        add(cells, "POISON CRYSTAL", "mashroom.png");
        add(cells, "MEDPOD", "strawberry.png");
        add(cells, "BATTERYFRUIT", "banana.png");
        add(cells, "STARBLOOM", "dragonfruit.png");
        add(cells, "SURGE ORB", "blueheart.png");
        add(cells, "STARBLADE", "sword_normal.png");
        add(cells, "MEAT", "meat.png");
        add(cells, "METAL", "metal.png");
        add(cells, "STONE", "stone.png");
        add(cells, "ROCK", "rock.png");
        add(cells, "DIAMOND", "diamond.png");
        add(cells, "PICKAXE", "pickaxe.png");
        add(cells, "PULSE GUN", "gun.png");
        add(cells, "METAL ORE", "metal-ore.png");
        add(cells, "STONE NODE", "stone-node.png");
        add(cells, "ROCK NODE", "rock-node.png");
        add(cells, "DIAMOND NODE", "diamond-node.png");
        for (int biome = 0; biome < 4; biome++) {
            for (int variant = 0; variant < 2; variant++) {
                BufferedImage landscape = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = landscape.createGraphics();
                try {
                    g.drawImage(TerrainArt.floor(variant, biome), 0, 0, null);
                    g.drawImage(TerrainArt.obstacle(variant, biome), 0, 0, null);
                } finally { g.dispose(); }
                cells.add(new Cell(new String[] {"VERDANT", "RIFT", "RUINS", "TIDAL"}[biome]
                        + " / " + (variant + 1), landscape));
            }
        }
        add(cells, "RELAY / DORMANT", "relay-dormant.png");
        add(cells, "RELAY / ONLINE", "relay-active.png");
        add(cells, "ENERGY / FULL", "heart_full.png");
        add(cells, "ENERGY / HALF", "heart_half.png");
        add(cells, "ENERGY / EMPTY", "heart_blank.png");
        int columns = 6;
        int cellWidth = 184;
        int cellHeight = 160;
        int rows = (cells.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * cellWidth + 32, rows * cellHeight + 126,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setColor(new Color(0x0B1427));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0xF2F0D9));
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 24));
            g.drawString("STARFALL / ORIGINAL SPRITE STUDIO", 28, 43);
            g.setColor(new Color(0x72ADA7));
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            g.drawString("Code-drawn pixel art  /  Ivory, teal, amber & obsidian  /  No borrowed sprites", 28, 69);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (int index = 0; index < cells.size(); index++) {
                Cell cell = cells.get(index);
                int x = 16 + (index % columns) * cellWidth;
                int y = 92 + (index / columns) * cellHeight;
                g.setColor(new Color(0x142538));
                g.fillRoundRect(x + 6, y + 5, cellWidth - 12, cellHeight - 12, 12, 12);
                g.setColor(new Color(0x20394A));
                g.drawLine(x + 18, y + 127, x + cellWidth - 18, y + 127);
                BufferedImage sprite = cell.sprite();
                double scale = Math.min(2.0, Math.min(156.0 / sprite.getWidth(), 112.0 / sprite.getHeight()));
                int width = (int) (sprite.getWidth() * scale);
                int height = (int) (sprite.getHeight() * scale);
                g.drawImage(sprite, x + (cellWidth - width) / 2,
                        y + 10 + (112 - height) / 2, width, height, null);
                g.setColor(new Color(0xB6CDC8));
                g.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
                int textWidth = g.getFontMetrics().stringWidth(cell.label());
                g.drawString(cell.label(), x + (cellWidth - textWidth) / 2, y + 143);
            }
            g.setColor(new Color(0x567E84));
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            g.drawString("All imagery authored in com.game.art / Characters 48px / Terrain 48px / Shuttle 128px", 28,
                    sheet.getHeight() - 16);
        } finally { g.dispose(); }
        Path output = arguments.length == 0 ? Path.of("build", "previews", "original-art.png")
                : Path.of(arguments[0]);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        if (!ImageIO.write(sheet, "png", output.toFile())) throw new IOException("PNG writer unavailable");
        System.out.println("Original art contact sheet: " + output.toAbsolutePath());
    }

    private static void add(List<Cell> cells, String label, String resource) {
        BufferedImage sprite = OriginalArt.create(resource);
        if (sprite == null) throw new IllegalArgumentException("Unknown original resource " + resource);
        cells.add(new Cell(label, sprite));
    }
}
