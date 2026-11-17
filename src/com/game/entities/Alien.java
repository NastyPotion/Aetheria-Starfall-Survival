package com.game.entities;

/** A ground enemy that chases nearby survivors and damages them on contact. */
public final class Alien extends Enemy {
    public Alien(int tileSize, Champion target) {
        super(EnemyType.ALIEN, tileSize, target, 90, 90, 2, 4, "");
    }

    @Override protected int aggroTiles() { return 8; }
}
