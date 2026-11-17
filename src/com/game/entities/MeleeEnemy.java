package com.game.entities;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import com.game.assets.Assets;
import com.game.world.Chunk;

/** A shared attack animation and wind-up for ground-based melee enemies. */
public abstract class MeleeEnemy extends Enemy {
    private final BufferedImage[][] attackSprites = new BufferedImage[4][2];
    private int attackTicks;

    protected MeleeEnemy(EnemyType type, int tileSize, Champion target,
            int spawnCol, int spawnRow, int speed, int maxHealth, String prefix) {
        super(type, tileSize, target, spawnCol, spawnRow, speed, maxHealth, prefix);
        for (Direction facing : Direction.values()) {
            for (int frame = 0; frame < 2; frame++) {
                attackSprites[facing.ordinal()][frame] = Assets.load(
                        prefix + "_attack_" + facing.assetName() + "_" + (frame + 1) + ".png");
            }
        }
    }

    protected abstract int meleeDamage();
    protected int windupTicks() { return 15; }
    protected int strikeTick() { return 25; }
    protected int cycleTicks() { return 35; }
    public final boolean isAttacking() { return attackTicks > 0; }
    public final boolean isWindingUp() { return attackTicks > 0 && attackTicks <= windupTicks(); }

    @Override
    public final void update(Chunk chunk, int tileSize) {
        Champion target = getTarget();
        if (!isAlive() || !target.isAlive()) return;
        if (updateReaction(chunk, tileSize)) return;
        if (attackTicks > 0) {
            attackTicks++;
            if (attackTicks == strikeTick() && getStrikeBounds(tileSize).intersects(target.getBounds())) {
                target.takeDamage(meleeDamage());
            }
            if (attackTicks > cycleTicks()) attackTicks = 0;
        } else if (distanceToTarget() <= tileSize + 10) {
            faceTarget();
            attackTicks = 1;
        } else if (distanceToTarget() <= aggroTiles() * tileSize) {
            chase(chunk, tileSize);
        } else {
            resetWalkingFrame();
        }
    }

    /** A fresh world-space footprint for attack telegraphs and collision checks. */
    public final Rectangle getStrikeBounds(int tileSize) {
        if (tileSize <= 0) throw new IllegalArgumentException("Tile size must be positive.");
        Rectangle body = getBounds();
        Rectangle reach = switch (getDirection()) {
            case UP -> new Rectangle(body.x - 8, body.y - tileSize, body.width + 16, tileSize);
            case DOWN -> new Rectangle(body.x - 8, body.y + body.height, body.width + 16, tileSize);
            case LEFT -> new Rectangle(body.x - tileSize, body.y - 8, tileSize, body.height + 16);
            case RIGHT -> new Rectangle(body.x + body.width, body.y - 8, tileSize, body.height + 16);
        };
        // Overlapping bodies must still receive the strike at point-blank range.
        return body.union(reach);
    }

    @Override protected final void resetBehavior() { attackTicks = 0; }

    @Override protected final int attackFrame() {
        if (attackTicks == 0) return -1;
        return attackTicks <= windupTicks() ? 0 : 1;
    }

    /** A phase-aware subtype can supply art without duplicating animation rules. */
    protected BufferedImage alternateSprite(int frame) { return null; }

    @Override protected final BufferedImage currentSprite() {
        int frame = attackFrame();
        BufferedImage alternate = alternateSprite(frame);
        if (alternate != null) return alternate;
        return frame < 0 ? super.currentSprite() : attackSprites[getDirection().ordinal()][frame];
    }
}
