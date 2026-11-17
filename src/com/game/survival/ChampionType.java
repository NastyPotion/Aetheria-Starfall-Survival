package com.game.survival;

/** Three playable choices share one Champion class and differ through simple data. */
public enum ChampionType {
    VANGUARD("Astra", "A resilient vanguard built for close combat and survival.", 4, 100, "astra"),
    SCOUT("Nyx", "An agile scout with quick movement and a larger dash reserve.", 5, 120, "scout"),
    ENGINEER("Orion", "A resourceful engineer ready to adapt and build better equipment.", 4, 110, "engineer");

    private final String displayName;
    private final String description;
    private final int speed;
    private final int maxEnergy;
    private final String spritePrefix;

    ChampionType(String displayName, String description, int speed, int maxEnergy, String spritePrefix) {
        this.displayName = displayName;
        this.description = description;
        this.speed = speed;
        this.maxEnergy = maxEnergy;
        this.spritePrefix = spritePrefix;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public int speed() { return speed; }
    public int maxEnergy() { return maxEnergy; }
    public String spritePrefix() { return spritePrefix; }
}
