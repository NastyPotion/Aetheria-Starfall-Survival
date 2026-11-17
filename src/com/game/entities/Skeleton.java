package com.game.entities;

import java.awt.image.BufferedImage;
import com.game.assets.Assets;

/** A faster melee guardian with more health and a lighter strike than the orc. */
public final class Skeleton extends MeleeEnemy {
    private final BufferedImage[][] enragedWalking = new BufferedImage[4][2];
    private final BufferedImage[][] enragedAttacks = new BufferedImage[4][2];

    public Skeleton(int tileSize, Champion target) {
        super(EnemyType.SKELETON, tileSize, target, 115, 85, 2, 8, "skeletonlord");
        for (Direction facing : Direction.values()) {
            for (int frame = 0; frame < 2; frame++) {
                String suffix = facing.assetName() + "_" + (frame + 1) + ".png";
                enragedWalking[facing.ordinal()][frame] = Assets.load("skeletonlord_phase2_" + suffix);
                enragedAttacks[facing.ordinal()][frame] = Assets.load("skeletonlord_phase2_attack_" + suffix);
            }
        }
    }

    @Override protected int aggroTiles() { return 8; }
    @Override protected int meleeDamage() { return 1; }
    public boolean isEnraged() { return getCurrentHealth() <= getMaxHealth() / 2 && isAlive(); }
    @Override protected int windupTicks() { return isEnraged() ? 10 : 15; }
    @Override protected int strikeTick() { return isEnraged() ? 18 : 25; }
    @Override protected int cycleTicks() { return isEnraged() ? 28 : 35; }

    @Override protected BufferedImage alternateSprite(int frame) {
        if (!isEnraged()) return null;
        return frame < 0 ? enragedWalking[getDirection().ordinal()][getSpriteFrame()]
                : enragedAttacks[getDirection().ordinal()][frame];
    }
}
