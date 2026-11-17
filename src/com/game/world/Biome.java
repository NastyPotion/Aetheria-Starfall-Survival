package com.game.world;

/** Deterministic regions keep saved terrain compatible with the visual world. */
public enum Biome {
    STARFALL_GLADE("Starfall Glade", 0), VERDANT_GROVE("Verdant Grove", 0),
    TIDAL_GARDENS("Tidal Gardens", 3), EMBER_RUINS("Ember Ruins", 2),
    ECLIPSE_REACH("Eclipse Reach", 1);
    private final String title;
    private final int artIndex;
    Biome(String title, int artIndex) { this.title = title; this.artIndex = artIndex; }
    public String getTitle() { return title; }
    public int getArtIndex() { return artIndex; }
    public static Biome at(int col, int row) {
        // Curved, slightly irregular borders avoid rectangular patches of terrain.
        double border = Math.sin(col * .7) * .55 + Math.cos(row * .6) * .45;
        if (Math.hypot(col - 115, row - 85) < 9 + border) return ECLIPSE_REACH;
        if (Math.hypot(col - 85, row - 115) < 10 + border) return EMBER_RUINS;
        if (Math.hypot(col - 110, row - 110) < 9 + border) return TIDAL_GARDENS;
        if (Math.hypot(col - 90, row - 90) < 9 + border) return VERDANT_GROVE;
        return STARFALL_GLADE;
    }
}
