package com.game.entities;

/** Flying enemies ignore trees while retaining world boundary checks. */
public final class Bat extends Enemy {
    public Bat(int tileSize, Champion target) {
        super(EnemyType.BAT, tileSize, target, 110, 110, 2, 2, "bat");
    }

    @Override protected int aggroTiles() { return 6; }
    @Override protected boolean canFly() { return true; }
    @Override protected int animationPeriod() { return 8; }
}
