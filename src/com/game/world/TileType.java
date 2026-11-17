package com.game.world;

/** The map vocabulary. Stable ids are used by the versioned save format. */
public enum TileType {
    GRASS(0, false), TREE(1, true), MUSHROOM(2, false),
    STRAWBERRY(3, false), BANANA(4, false), DRAGON_FRUIT(5, false),
    BLUE_HEART(6, false), SWORD(7, false), METAL(8, false), STONE(9, false),
    ROCK(10, false), DIAMOND(11, false), MEAT(12, false), TREE_METAL(13, true),
    TREE_FOOD(14, true), ROCK_DIAMOND(15, true), BOULDER(16, true), TREE_STONE(17, true);

    private final int id;
    private final boolean solid;

    TileType(int id, boolean solid) { this.id = id; this.solid = solid; }
    public int getId() { return id; }
    public boolean isSolid() { return solid; }
    public boolean isCollectible() { return this != GRASS && !solid; }
    public boolean isHarvestable() { return solid; }

    public static TileType fromId(int id) {
        for (TileType type : values()) if (type.id == id) return type;
        throw new IllegalArgumentException("Unknown tile id: " + id);
    }
}
