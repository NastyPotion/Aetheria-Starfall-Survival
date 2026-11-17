package com.game.survival;

/** Difficulty data used by spawning/stat factories, rather than copied conditions. */
public enum Difficulty {
    EASY("Easy", 0, 1.0, 0, 1.0),
    MEDIUM("Medium", 2, 1.25, 1, 1.25),
    HARD("Hard", 4, 1.5, 2, 1.5);

    private final String displayName;
    private final int extraSpawns;
    private final double healthMultiplier;
    private final int speedBonus;
    private final double staminaMultiplier;

    Difficulty(String displayName, int extraSpawns, double healthMultiplier,
            int speedBonus, double staminaMultiplier) {
        this.displayName = displayName;
        this.extraSpawns = extraSpawns;
        this.healthMultiplier = healthMultiplier;
        this.speedBonus = speedBonus;
        this.staminaMultiplier = staminaMultiplier;
    }

    public String displayName() { return displayName; }
    public int extraSpawns() { return extraSpawns; }
    public double healthMultiplier() { return healthMultiplier; }
    public int speedBonus() { return speedBonus; }
    public double staminaMultiplier() { return staminaMultiplier; }
}
