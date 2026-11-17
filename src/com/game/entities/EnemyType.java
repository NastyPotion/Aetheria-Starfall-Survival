package com.game.entities;

/** Stable enemy identifiers used by the game and save files. */
public enum EnemyType {
    ALIEN("Mossling"), BAT("Riftwing"), ORC("Briar Sentinel"), SKELETON("Eclipse Warden");

    private final String displayName;

    EnemyType(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
