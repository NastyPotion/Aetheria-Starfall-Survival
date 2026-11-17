package com.game.survival;

/** Loose materials and the diamonds that can be secured at the ship. */
public enum Resource {
    MEAT("Meat"), METAL("Metal"), STONE("Stone"), ROCK("Rock"), DIAMOND("Diamond");

    private final String displayName;

    Resource(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
