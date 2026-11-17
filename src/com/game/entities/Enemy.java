package com.game.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.IdentityHashMap;
import java.util.Map;
import com.game.assets.Assets;
import com.game.world.Chunk;

/** Reusable health, movement, collision and rendering for every enemy subtype. */
public abstract class Enemy extends Entity implements Damageable {
    private final EnemyType type;
    private int maxHealth;
    private int currentHealth;
    private String id;
    private int maxStamina = 90;
    private int stamina = 90;
    private int exertionTicks;
    private int recoveryTicks;
    private int restTicks;
    private boolean fastMonster;
    private final Champion target;
    private Direction direction = Direction.DOWN;
    private int animationTicks;
    private int flashTicks;
    private int staggerTicks;
    private int knockbackX;
    private int knockbackY;
    private int spriteFrame;
    private final BufferedImage[][] walkingSprites = new BufferedImage[4][2];
    private final Map<BufferedImage, BufferedImage> flashSprites = new IdentityHashMap<>();

    protected Enemy(EnemyType type, int tileSize, Champion target,
            int spawnCol, int spawnRow, int speed, int maxHealth, String spritePrefix) {
        super(speed, tileSize - 16, tileSize - 16,
                spawnCoordinate(spawnCol, tileSize), spawnCoordinate(spawnRow, tileSize));
        if (tileSize <= 16 || speed <= 0 || maxHealth <= 0) {
            throw new IllegalArgumentException("Enemy size, speed and health must be positive.");
        }
        this.type = Objects.requireNonNull(type, "type");
        id = type.name().toLowerCase(java.util.Locale.ROOT) + "-0";
        this.target = Objects.requireNonNull(target, "target");
        this.maxHealth = maxHealth;
        currentHealth = maxHealth;
        if (type == EnemyType.ALIEN) {
            BufferedImage first = Assets.load("icons8-minecraft-creeper-16.png");
            BufferedImage second = Assets.load("icons8-minecraft-creeper-48.png");
            for (Direction facing : Direction.values()) {
                walkingSprites[facing.ordinal()][0] = first;
                walkingSprites[facing.ordinal()][1] = second;
            }
        } else {
            for (Direction facing : Direction.values()) {
                for (int frame = 0; frame < 2; frame++) {
                    String spriteDirection = type == EnemyType.BAT ? "down" : facing.assetName();
                    walkingSprites[facing.ordinal()][frame] = Assets.load(
                            spritePrefix + "_" + spriteDirection + "_" + (frame + 1) + ".png");
                }
            }
        }
    }

    private static int spawnCoordinate(int tile, int tileSize) {
        long coordinate = (long) tile * tileSize;
        if (tileSize <= 16 || coordinate < 0 || coordinate > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Enemy spawn must be inside integer coordinate bounds.");
        }
        return (int) coordinate;
    }

    public final EnemyType getType() { return type; }
    public final String getId() { return id; }
    public final int getStamina() { return stamina; }
    public final int getMaxStamina() { return maxStamina; }
    public final boolean isResting() { return restTicks > 0; }
    public final boolean isMonster() { return type == EnemyType.ALIEN || type == EnemyType.BAT; }
    public final boolean isFastMonster() { return fastMonster; }
    public final String getDisplayName() { return type.displayName(); }
    public final boolean isStaggered() { return staggerTicks > 0; }
    public final boolean isFlashing() { return flashTicks > 0; }
    @Override public final int getCurrentHealth() { return currentHealth; }
    @Override public final int getMaxHealth() { return maxHealth; }
    @Override public final boolean isAlive() { return currentHealth > 0; }

    protected final Champion getTarget() { return target; }
    protected final Direction getDirection() { return direction; }
    protected final int getSpriteFrame() { return spriteFrame; }
    protected final void resetWalkingFrame() { spriteFrame = 0; }

    /** Encounter construction supplies level/difficulty stats before play begins. */
    public final void configure(String id, int maxHealth, int speed, int maxStamina, boolean fastMonster) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank() || id.length() > 64 || maxHealth < 1 || maxHealth > 128
                || speed < 1 || speed > 10 || maxStamina < 1 || maxStamina > 600
                || fastMonster && !isMonster()) {
            throw new IllegalArgumentException("Invalid encounter identity or combat stats.");
        }
        this.id = id;
        this.maxHealth = maxHealth;
        setSpeed(speed);
        this.maxStamina = maxStamina;
        this.fastMonster = fastMonster;
        restoreState(getWorldX(), getWorldY(), maxHealth, maxStamina);
    }

    @Override
    public final void takeDamage(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Damage must be positive.");
        if (!isAlive()) return;
        currentHealth = Math.max(0, currentHealth - amount);
        flashTicks = 8;
        staggerTicks = 10;
        knockbackX = Integer.compare(getWorldX(), target.getWorldX()) * 2;
        knockbackY = Integer.compare(getWorldY(), target.getWorldY()) * 2;
        if (knockbackX == 0 && knockbackY == 0) knockbackY = 2;
        resetBehavior();
    }

    public final void restoreState(int worldX, int worldY, int health, int stamina) {
        if (health < 0 || health > maxHealth || stamina < 0 || stamina > maxStamina) {
            throw new IllegalArgumentException("Enemy health or stamina is outside its valid range.");
        }
        setPosition(worldX, worldY);
        currentHealth = health;
        this.stamina = stamina;
        exertionTicks = 0;
        recoveryTicks = 0;
        restTicks = stamina == 0 ? 48 : 0;
        animationTicks = 0;
        spriteFrame = 0;
        flashTicks = 0;
        staggerTicks = 0;
        knockbackX = 0;
        knockbackY = 0;
        direction = Direction.DOWN;
        resetBehavior();
    }

    /** Subclasses can reset additional behavior without duplicating validation. */
    protected void resetBehavior() { }

    protected abstract int aggroTiles();
    protected boolean canFly() { return false; }
    protected int contactDamage() { return 1; }
    protected int animationPeriod() { return 12; }

    protected final double distanceToTarget() {
        return Math.hypot((double) getWorldX() - target.getWorldX(),
                (double) getWorldY() - target.getWorldY());
    }

    @Override
    public void update(Chunk chunk, int tileSize) {
        if (!isAlive() || !target.isAlive()) return;
        if (updateReaction(chunk, tileSize)) return;
        boolean chasing = distanceToTarget() <= aggroTiles() * tileSize;
        if (chasing) {
            chase(chunk, tileSize);
        } else if (canFly()) {
            animate();
        } else {
            spriteFrame = 0;
        }
        if (getBounds().intersects(target.getBounds())) {
            target.takeDamage(contactDamage());
        }
    }

    /** All enemy subtypes share the same interruption and wall-safe knockback. */
    protected final boolean updateReaction(Chunk chunk, int tileSize) {
        if (flashTicks > 0) flashTicks--;
        if (staggerTicks > 0) {
            staggerTicks--;
            tryMove(chunk, tileSize, getWorldX() + knockbackX, getWorldY());
            tryMove(chunk, tileSize, getWorldX(), getWorldY() + knockbackY);
            return true;
        }
        if (restTicks > 0) {
            restTicks--;
            stamina = Math.min(maxStamina, stamina + Math.max(1, (maxStamina + 47) / 48));
            if (restTicks == 0) stamina = maxStamina;
            return true;
        }
        boolean exerting = attackFrame() >= 0 || distanceToTarget() <= aggroTiles() * tileSize;
        if (exerting) {
            recoveryTicks = 0;
            if (++exertionTicks >= 3) {
                exertionTicks = 0;
                stamina = Math.max(0, stamina - 1);
                if (stamina == 0) {
                    restTicks = 48;
                    resetBehavior();
                    return true;
                }
            }
        } else {
            exertionTicks = 0;
            if (++recoveryTicks >= 2) {
                stamina = Math.min(maxStamina, stamina + 1);
                recoveryTicks = 0;
            }
        }
        return false;
    }

    /** Resolve axes separately so a blocked diagonal can slide along a wall. */
    protected final void chase(Chunk chunk, int tileSize) {
        int x = getWorldX();
        int y = getWorldY();
        int speed = getSpeed();
        int stepX = Integer.compare(target.getWorldX(), x) * speed;
        int stepY = Integer.compare(target.getWorldY(), y) * speed;
        // Avoid oscillating around positions that are not multiples of our speed.
        if (Math.abs((long) target.getWorldX() - x) < speed) stepX = target.getWorldX() - x;
        if (Math.abs((long) target.getWorldY() - y) < speed) stepY = target.getWorldY() - y;
        faceTarget();
        boolean moved = tryMove(chunk, tileSize, x + stepX, y);
        moved |= tryMove(chunk, tileSize, getWorldX(), y + stepY);
        if (moved || canFly()) animate();
        else spriteFrame = 0;
    }

    private boolean tryMove(Chunk chunk, int tileSize, int nextX, int nextY) {
        if (nextX == getWorldX() && nextY == getWorldY()) return false;
        Rectangle destination = boundsAt(nextX, nextY);
        boolean withinWorld = destination.x >= 0 && destination.y >= 0
                && (long) destination.x + destination.width <= (long) chunk.getWidth() * tileSize
                && (long) destination.y + destination.height <= (long) chunk.getHeight() * tileSize;
        if (withinWorld && (canFly() || chunk.canOccupy(destination, tileSize))) {
            setPosition(nextX, nextY);
            return true;
        }
        return false;
    }

    protected final void faceTarget() {
        long deltaX = (long) target.getWorldX() - getWorldX();
        long deltaY = (long) target.getWorldY() - getWorldY();
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            direction = deltaX < 0 ? Direction.LEFT : Direction.RIGHT;
        } else if (deltaX != 0 || deltaY != 0) {
            direction = deltaY < 0 ? Direction.UP : Direction.DOWN;
        }
    }

    private void animate() {
        animationTicks++;
        if (animationTicks >= animationPeriod()) {
            spriteFrame = 1 - spriteFrame;
            animationTicks = 0;
        }
    }

    protected BufferedImage currentSprite() {
        return walkingSprites[direction.ordinal()][spriteFrame];
    }

    protected int attackFrame() { return -1; }

    @Override
    public void draw(Graphics graphics, int tileSize) {
        if (!isAlive()) return;
        int screenX = getWorldX() - target.getWorldX() + target.getScreenX();
        int screenY = getWorldY() - target.getWorldY() + target.getScreenY();
        int drawX = screenX - (tileSize - getBodyWidth()) / 2;
        int drawY = screenY - (tileSize - getBodyHeight()) / 2;
        int drawWidth = tileSize;
        int drawHeight = tileSize;
        if (attackFrame() == 1) {
            if (direction == Direction.UP || direction == Direction.DOWN) {
                drawHeight *= 2;
                if (direction == Direction.UP) drawY -= tileSize;
            } else {
                drawWidth *= 2;
                if (direction == Direction.LEFT) drawX -= tileSize;
            }
        }
        BufferedImage image = currentSprite();
        if (isFlashing()) image = flashSprites.computeIfAbsent(image, Enemy::flashSprite);
        graphics.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        if (currentHealth < maxHealth) {
            graphics.setColor(new Color(30, 20, 25));
            graphics.fillRect(screenX, screenY - 9, getBodyWidth(), 4);
            graphics.setColor(new Color(235, 75, 65));
            graphics.fillRect(screenX, screenY - 9,
                    getBodyWidth() * currentHealth / maxHealth, 4);
        }
        if (stamina < maxStamina || isResting()) {
            graphics.setColor(new Color(21, 42, 53));
            graphics.fillRect(screenX, screenY - 4, getBodyWidth(), 3);
            graphics.setColor(isResting() ? new Color(236, 190, 111) : new Color(117, 218, 208));
            graphics.fillRect(screenX, screenY - 4, getBodyWidth() * stamina / maxStamina, 3);
        }
    }

    private static BufferedImage flashSprite(BufferedImage source) {
        BufferedImage flashed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < source.getHeight(); row++) {
            for (int col = 0; col < source.getWidth(); col++) {
                int alpha = source.getRGB(col, row) & 0xff000000;
                flashed.setRGB(col, row, alpha | 0x00fff6dc);
            }
        }
        return flashed;
    }
}
