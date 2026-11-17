package com.game.survival;

/** Stable equipment identifiers, independent of input and animation state. */
public enum WeaponType {
    FISTS("Fists"), PICKAXE("Pickaxe"), SWORD("Sword"), GUN("Gun");

    private final String displayName;

    WeaponType(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
