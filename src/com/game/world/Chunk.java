package com.game.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import com.game.assets.Assets;
import com.game.art.TerrainArt;
import com.game.core.GameConfig;
import com.game.entities.Champion;

/** Owns the map; callers interact with tiles without access to its mutable array. */
public final class Chunk {
    public static final int WORLD_SIZE = 200;
    private final TileType[][] tiles = new TileType[WORLD_SIZE][WORLD_SIZE];
    private final Map<TileType, BufferedImage> images = new EnumMap<>(TileType.class);

    public Chunk() { this(new Random().nextLong()); }
    public Chunk(long seed) { generateMap(new Random(seed)); loadImages(); }

    public Chunk(int[][] savedTiles) {
        if (savedTiles == null || savedTiles.length != WORLD_SIZE) {
            throw new IllegalArgumentException("The world must have 200 rows.");
        }
        for (int row = 0; row < WORLD_SIZE; row++) {
            if (savedTiles[row] == null || savedTiles[row].length != WORLD_SIZE) {
                throw new IllegalArgumentException("The world must have 200 columns.");
            }
            for (int col = 0; col < WORLD_SIZE; col++) tiles[row][col] = TileType.fromId(savedTiles[row][col]);
        }
        loadImages();
    }

    private void generateMap(Random random) {
        for (int row = 0; row < WORLD_SIZE; row++) {
            for (int col = 0; col < WORLD_SIZE; col++) {
                int chance = random.nextInt(1000);
                tiles[row][col] = chance < 85 ? TileType.TREE
                    : chance < 105 ? TileType.TREE_METAL : chance < 125 ? TileType.TREE_STONE
                    : chance < 140 ? TileType.TREE_FOOD : chance < 155 ? TileType.BOULDER
                    : chance < 170 ? TileType.ROCK_DIAMOND : chance < 175 ? TileType.MUSHROOM
                    : chance < 188 ? TileType.STRAWBERRY : chance < 194 ? TileType.BANANA
                    : chance < 199 ? TileType.BLUE_HEART : TileType.GRASS;
            }
        }
        clearArea(97, 96, 106, 103);
        // Open routes make every enemy and the introductory fruit reachable.
        clearRoute(100, 100, 90, 90);
        clearRoute(100, 100, 110, 110);
        clearRoute(100, 100, 85, 115);
        clearRoute(100, 100, 115, 85);
        clearArena(90, 90);
        clearArena(110, 110);
        clearArena(85, 115);
        clearArena(115, 85);
        // A safe initial food supply and reachable resource field on every seed.
        tiles[100][102] = TileType.MEAT;
        tiles[101][102] = TileType.STRAWBERRY;
        tiles[99][102] = TileType.BANANA;
        for (int i = 0; i < 12; i++) {
            int col = 93 + i;
            tiles[103][col] = i % 2 == 0 ? TileType.METAL : TileType.STONE;
            tiles[107][col] = i % 3 == 0 ? TileType.TREE_METAL : i % 3 == 1 ? TileType.TREE_STONE : TileType.BOULDER;
            tiles[109][col] = TileType.ROCK_DIAMOND;
            tiles[110][col] = TileType.GRASS;
        }
    }

    private void clearArena(int col, int row) {
        for (int r = row - 4; r <= row + 4; r++) {
            for (int c = col - 4; c <= col + 4; c++) {
                if (Math.hypot(c - col, r - row) <= 4.5) tiles[r][c] = TileType.GRASS;
            }
        }
    }

    public Biome getBiomeAt(int col, int row) { return Biome.at(col, row); }

    private void clearArea(int left, int top, int right, int bottom) {
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) tiles[row][col] = TileType.GRASS;
        }
    }

    private void clearRoute(int startCol, int startRow, int endCol, int endRow) {
        int col = startCol;
        int row = startRow;
        while (col != endCol || row != endRow) {
            clearArea(col - 1, row - 1, col + 1, row + 1);
            col += Integer.compare(endCol, col);
            row += Integer.compare(endRow, row);
        }
        clearArea(col - 1, row - 1, col + 1, row + 1);
    }

    private void loadImages() {
        images.put(TileType.GRASS, Assets.load("002.png"));
        images.put(TileType.TREE, Assets.load("016.png"));
        images.put(TileType.MUSHROOM, Assets.load("mashroom.png"));
        images.put(TileType.STRAWBERRY, Assets.load("strawberry.png"));
        images.put(TileType.BANANA, Assets.load("banana.png"));
        images.put(TileType.DRAGON_FRUIT, Assets.load("dragonfruit.png"));
        images.put(TileType.BLUE_HEART, Assets.load("blueheart.png"));
        images.put(TileType.SWORD, Assets.load("sword_normal.png"));
        images.put(TileType.METAL, Assets.load("metal.png"));
        images.put(TileType.STONE, Assets.load("stone.png"));
        images.put(TileType.ROCK, Assets.load("rock.png"));
        images.put(TileType.DIAMOND, Assets.load("diamond.png"));
        images.put(TileType.MEAT, Assets.load("meat.png"));
    }

    public int getWidth() { return WORLD_SIZE; }
    public int getHeight() { return WORLD_SIZE; }
    public boolean contains(int col, int row) { return col >= 0 && col < WORLD_SIZE && row >= 0 && row < WORLD_SIZE; }
    public boolean isSolid(int col, int row) { return !contains(col, row) || tiles[row][col].isSolid(); }

    public TileType getTileAt(int col, int row) {
        if (!contains(col, row)) throw new IllegalArgumentException("Tile is outside the world.");
        return tiles[row][col];
    }

    public boolean canOccupy(Rectangle bounds, int tileSize) {
        if (tileSize <= 0 || bounds.width <= 0 || bounds.height <= 0) return false;
        if (bounds.x < 0 || bounds.y < 0
                || (long) bounds.x + bounds.width > (long) WORLD_SIZE * tileSize
                || (long) bounds.y + bounds.height > (long) WORLD_SIZE * tileSize) return false;
        int left = Math.floorDiv(bounds.x, tileSize);
        int right = (int) Math.floorDiv((long) bounds.x + bounds.width - 1, tileSize);
        int top = Math.floorDiv(bounds.y, tileSize);
        int bottom = (int) Math.floorDiv((long) bounds.y + bounds.height - 1, tileSize);
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) if (isSolid(col, row)) return false;
        }
        return true;
    }

    public TileType collectAt(int col, int row) {
        if (!contains(col, row)) return TileType.GRASS;
        TileType type = tiles[row][col];
        if (type.isCollectible()) tiles[row][col] = TileType.GRASS;
        return type;
    }

    /** Reveals and collects a hidden resource without overwriting adjacent tiles. */
    public TileType harvestAt(int col, int row, boolean hasPickaxe) {
        if (!contains(col, row)) return TileType.GRASS;
        TileType before = tiles[row][col];
        if ((before == TileType.ROCK_DIAMOND || before == TileType.BOULDER) && !hasPickaxe) return TileType.GRASS;
        TileType loot = switch (before) {
            case TREE_METAL -> TileType.METAL;
            case TREE_STONE -> TileType.STONE;
            case TREE_FOOD -> TileType.MEAT;
            case ROCK_DIAMOND -> TileType.DIAMOND;
            case BOULDER -> TileType.ROCK;
            case TREE -> TileType.STRAWBERRY;
            default -> TileType.GRASS;
        };
        if (loot != TileType.GRASS) tiles[row][col] = TileType.GRASS;
        return loot;
    }

    public void dropAt(int col, int row, TileType item) {
        if (!item.isCollectible()) throw new IllegalArgumentException("Only loose items can be dropped.");
        if (contains(col, row) && !tiles[row][col].isSolid()) tiles[row][col] = item;
    }

    /** Clears an enemy's destination so generated obstacles never trap a spawn. */
    public void clearSpawn(int col, int row) {
        if (col < 2 || row < 2 || col >= WORLD_SIZE - 2 || row >= WORLD_SIZE - 2) throw new IllegalArgumentException("Spawn outside safe map bounds.");
        clearArea(col - 1, row - 1, col + 1, row + 1);
    }

    /** Returns a fresh {column,row} coordinate, or null if no matching tile exists. */
    public int[] findNearest(TileType[] types, int worldX, int worldY) {
        int[] nearest = null;
        long bestDistance = Long.MAX_VALUE;
        for (int r = 0; r < WORLD_SIZE; r++) {
            for (int c = 0; c < WORLD_SIZE; c++) {
                boolean matches = false;
                for (TileType type : types) {
                    if (tiles[r][c] == type) matches = true;
                }
                if (!matches) continue;
                long dx = (long) c * GameConfig.TILE_SIZE - worldX;
                long dy = (long) r * GameConfig.TILE_SIZE - worldY;
                long distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    nearest = new int[] {c, r};
                    bestDistance = distance;
                }
            }
        }
        return nearest;
    }

    public int[][] copyTiles() {
        int[][] copy = new int[WORLD_SIZE][WORLD_SIZE];
        for (int row = 0; row < WORLD_SIZE; row++) {
            for (int col = 0; col < WORLD_SIZE; col++) copy[row][col] = tiles[row][col].getId();
        }
        return copy;
    }

    public void draw(Graphics g, int tileSize, Champion player) {
        Rectangle clip = g.getClipBounds();
        int clipX = clip == null ? 0 : clip.x;
        int clipY = clip == null ? 0 : clip.y;
        int width = clip == null ? GameConfig.VIEW_WIDTH : clip.width;
        int height = clip == null ? GameConfig.VIEW_HEIGHT : clip.height;
        int cameraX = player.getWorldX() - player.getScreenX();
        int cameraY = player.getWorldY() - player.getScreenY();
        int firstCol = Math.max(0, Math.floorDiv(cameraX + clipX, tileSize));
        int lastCol = Math.min(WORLD_SIZE - 1, Math.floorDiv(cameraX + clipX + width, tileSize));
        int firstRow = Math.max(0, Math.floorDiv(cameraY + clipY, tileSize));
        int lastRow = Math.min(WORLD_SIZE - 1, Math.floorDiv(cameraY + clipY + height, tileSize));
        g.setColor(new Color(14, 36, 41));
        g.fillRect(clipX, clipY, width, height);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                int screenX = col * tileSize - cameraX;
                int screenY = row * tileSize - cameraY;
                TileType type = tiles[row][col];
                int variant = Math.floorMod(col * 7349 ^ row * 193, 16);
                int biome = Biome.at(col, row).getArtIndex();
                g.drawImage(TerrainArt.floor(variant, biome), screenX, screenY, tileSize, tileSize, null);
                if (Math.abs(Math.abs(col - 100) - Math.abs(row - 100)) <= 1
                        && Math.max(Math.abs(col - 100), Math.abs(row - 100)) < 21) {
                    g.setColor(new Color(146, 184, 158, 23));
                    g.fillRect(screenX, screenY, tileSize, tileSize);
                    g.setColor(new Color(184, 214, 185, 40));
                    g.fillRect(screenX + 8 + variant, screenY + 18, 12, 2);
                }
                if (type.isSolid()) {
                    boolean tree = type == TileType.TREE || type == TileType.TREE_METAL || type == TileType.TREE_FOOD || type == TileType.TREE_STONE;
                    BufferedImage obstacle = tree ? TerrainArt.obstacle(variant, biome)
                            : Assets.load(type == TileType.ROCK_DIAMOND ? "diamond-node.png" : "rock-node.png");
                    g.drawImage(obstacle, screenX, screenY, tileSize, tileSize, null);
                    if (type != TileType.TREE) {
                        g.setColor(type == TileType.ROCK_DIAMOND ? new Color(120, 230, 245) : new Color(225, 195, 120));
                        g.fillRect(screenX + 35, screenY + 33, 3, 3);
                    }
                } else if (type.isCollectible()) {
                    int size = type == TileType.SWORD ? 38 : 28;
                    int offset = (tileSize - size) / 2;
                    g.drawImage(images.get(type), screenX + offset, screenY + offset, size, size, null);
                }
            }
        }
    }
}
