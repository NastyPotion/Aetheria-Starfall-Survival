package com.game.entities;

/** The declaration order matches rows in the walking and attack sprite arrays. */
public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    String assetName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
