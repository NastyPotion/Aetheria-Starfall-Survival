package com.game.entities;

/** A slow, durable melee opponent whose club inflicts two health points. */
public final class Orc extends MeleeEnemy {
    public Orc(int tileSize, Champion target) {
        super(EnemyType.ORC, tileSize, target, 85, 115, 1, 6, "orc");
    }

    @Override protected int aggroTiles() { return 7; }
    @Override protected int meleeDamage() { return 2; }
}
