package com.game.entities;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Objects;
import com.game.assets.Assets;
import com.game.core.KeyHandler;
import com.game.core.GameConfig;
import com.game.world.Chunk;
import com.game.survival.ChampionType;
import com.game.survival.WeaponType;

/** One playable explorer; the selected ChampionType supplies identity and stats. */
public final class Champion extends Entity implements Damageable {
    private static final int DASH_COST = 35;
    private static final int DASH_DURATION = 8;
    private static final int DASH_COOLDOWN = 30;
    private static final int MAX_HEALTH = 12;
    private final KeyHandler keys;
    private final int maxEnergy;
    private final ChampionType championType;
    private int currentHealth;
    private int invulnerabilityTicks;
    private WeaponType weapon = WeaponType.FISTS;
    private int attackTicks;
    private int attackSequence;
    private int animationTicks;
    private int spriteFrame;
    private int energy;
    private int energyRegenTicks;
    private int dashTicks;
    private int dashCooldown;
    private boolean dashHeld;
    private Direction direction = Direction.DOWN;
    private Direction dashDirection = Direction.DOWN;
    private final BufferedImage[][] walkingSprites = new BufferedImage[4][2];
    private final BufferedImage[][] attackSprites = new BufferedImage[4][2];

    private final int screenX;
    private final int screenY;

    public Champion(KeyHandler keys, int tileSize, ChampionType championType) {
        super(Objects.requireNonNull(championType, "championType").speed(), tileSize - 16, tileSize - 16);
        if (tileSize <= 16) {
            throw new IllegalArgumentException("Champion tile size must exceed its sprite margin.");
        }
        this.keys = Objects.requireNonNull(keys, "keys");
        this.maxEnergy = championType.maxEnergy();
        this.championType = championType;
        currentHealth = 4;
        energy = maxEnergy;
        screenX = GameConfig.VIEW_WIDTH / 2 - tileSize / 2;
        screenY = GameConfig.VIEW_HEIGHT / 2 - tileSize / 2;
        setPosition(tileSize * 100, tileSize * 100);
        String spritePrefix = championType.spritePrefix();
        for (Direction facing : Direction.values()) {
            for (int frame = 0; frame < 2; frame++) {
                String suffix = facing.assetName() + "_" + (frame + 1) + ".png";
                walkingSprites[facing.ordinal()][frame] = Assets.load(spritePrefix + "_" + suffix);
                attackSprites[facing.ordinal()][frame] = Assets.load(spritePrefix + "_attack_" + suffix);
            }
        }
    }

    @Override public final int getCurrentHealth() { return currentHealth; }
    @Override public final int getMaxHealth() { return MAX_HEALTH; }
    @Override public final boolean isAlive() { return currentHealth > 0; }
    public final boolean isAttacking() { return attackTicks > 0; }
    public final int getAttackSequence() { return attackSequence; }
    public final int getAttackDamage() {
        return switch (weapon) { case FISTS -> 1; case PICKAXE -> 2; case SWORD -> 3; case GUN -> 4; };
    }
    public final WeaponType getWeaponType() { return weapon; }
    public final void setWeapon(WeaponType weapon) { this.weapon = Objects.requireNonNull(weapon, "weapon"); }
    public final ChampionType getChampionType() { return championType; }
    public final String getDisplayName() { return championType.displayName(); }
    public final String getSignature() { return championType.description(); }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }
    public final Direction getDirection() { return direction; }
    public final int getEnergy() { return energy; }
    public final int getMaxEnergy() { return maxEnergy; }
    public final boolean isDashing() { return dashTicks > 0; }
    public final int getDashCooldown() { return dashCooldown; }

    /** Fists, the pickaxe and the sword share the same damage contract. */
    public final void strike(Damageable target) {
        Objects.requireNonNull(target, "target");
        if (!getAttackBounds().isEmpty() && target.isAlive()) target.takeDamage(getAttackDamage());
    }

    /** The empty rectangle prevents damage during wind-up and outside an attack. */
    public final Rectangle getAttackBounds() {
        if (!isAlive() || attackTicks < 6 || attackTicks > 25 || weapon == WeaponType.GUN) {
            return new Rectangle();
        }
        Rectangle body = getBounds();
        int reach = switch (weapon) { case FISTS -> 30; case PICKAXE -> 36; case SWORD -> 42; case GUN -> 0; };
        return switch (direction) {
            case UP -> new Rectangle(body.x, body.y - reach, body.width, reach);
            case DOWN -> new Rectangle(body.x, body.y + body.height, body.width, reach);
            case LEFT -> new Rectangle(body.x - reach, body.y, reach, body.height);
            case RIGHT -> new Rectangle(body.x + body.width, body.y, reach, body.height);
        };
    }

    @Override
    public final void takeDamage(int amount) {
        requirePositive(amount);
        if (isAlive() && invulnerabilityTicks == 0 && !isDashing()) {
            currentHealth = Math.max(0, currentHealth - amount);
            invulnerabilityTicks = 60;
            if (!isAlive()) {
                attackTicks = 0;
                dashTicks = 0;
            }
        }
    }

    public final void heal(int amount) {
        requirePositive(amount);
        if (isAlive()) {
            currentHealth = (int) Math.min(MAX_HEALTH, (long) currentHealth + amount);
        }
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Health change must be positive.");
        }
    }

    /** Restores persistent survival state and clears transient animation/ability state. */
    public final void restoreState(int worldX, int worldY, int health, WeaponType weapon) {
        if (health < 0 || health > MAX_HEALTH) throw new IllegalArgumentException("Invalid champion health.");
        Objects.requireNonNull(weapon, "weapon");
        setPosition(worldX, worldY);
        currentHealth = health;
        this.weapon = weapon;
        invulnerabilityTicks = 0;
        attackTicks = 0;
        attackSequence = 0;
        animationTicks = 0;
        spriteFrame = 0;
        direction = Direction.DOWN;
        dashDirection = Direction.DOWN;
        dashTicks = 0;
        dashCooldown = 0;
        dashHeld = false;
        energy = maxEnergy;
        energyRegenTicks = 0;
    }

    @Override
    public void update(Chunk chunk, int tileSize) {
        if (!isAlive()) {
            return;
        }
        if (invulnerabilityTicks > 0) invulnerabilityTicks--;
        if (dashCooldown > 0) dashCooldown--;
        if (dashTicks > 0) dashTicks--;
        boolean dashPressed = keys.isDashPressed();
        boolean dashRequested = dashPressed && !dashHeld;
        dashHeld = dashPressed;
        if (!isDashing() && ++energyRegenTicks >= 3) {
            energy = Math.min(maxEnergy, energy + 1);
            energyRegenTicks = 0;
        }
        if (dashRequested && !isDashing() && !isAttacking()
                && dashCooldown == 0 && energy >= DASH_COST) {
            direction = movementDirection();
            dashDirection = direction;
            dashTicks = DASH_DURATION;
            dashCooldown = DASH_COOLDOWN;
            energy -= DASH_COST;
            energyRegenTicks = 0;
        }
        if (isDashing()) {
            updateDash(chunk, tileSize);
        } else if (attackTicks == 0 && keys.isAttackPressed()) {
            attackTicks = 1;
            attackSequence++;
            spriteFrame = 0;
        } else if (attackTicks > 0) {
            attackTicks++;
            if (attackTicks > 25) {
                attackTicks = 0;
                spriteFrame = 0;
            }
        } else {
            updateMovement(chunk, tileSize);
        }
    }

    private void updateMovement(Chunk chunk, int tileSize) {
        int x = getWorldX();
        int y = getWorldY();
        int speed = getSpeed();
        int nextX = x;
        int nextY = y;
        if (keys.isUpPressed()) {
            direction = Direction.UP;
            nextY -= speed;
        } else if (keys.isDownPressed()) {
            direction = Direction.DOWN;
            nextY += speed;
        } else if (keys.isLeftPressed()) {
            direction = Direction.LEFT;
            nextX -= speed;
        } else if (keys.isRightPressed()) {
            direction = Direction.RIGHT;
            nextX += speed;
        }
        boolean moving = nextX != x || nextY != y;
        Rectangle destination = boundsAt(nextX, nextY);
        if (moving && chunk.canOccupy(destination, tileSize)) {
            setPosition(nextX, nextY);
            animationTicks++;
            if (animationTicks >= 12) {
                spriteFrame = 1 - spriteFrame;
                animationTicks = 0;
            }
        } else {
            spriteFrame = 0;
            animationTicks = 0;
        }
    }

    private Direction movementDirection() {
        if (keys.isUpPressed()) return Direction.UP;
        if (keys.isDownPressed()) return Direction.DOWN;
        if (keys.isLeftPressed()) return Direction.LEFT;
        if (keys.isRightPressed()) return Direction.RIGHT;
        return direction;
    }

    /** Three collision-checked steps provide a burst without crossing solid tiles. */
    private void updateDash(Chunk chunk, int tileSize) {
        int speed = getSpeed();
        int stepX = switch (dashDirection) {
            case LEFT -> -speed;
            case RIGHT -> speed;
            default -> 0;
        };
        int stepY = switch (dashDirection) {
            case UP -> -speed;
            case DOWN -> speed;
            default -> 0;
        };
        for (int step = 0; step < 3; step++) {
            int nextX = getWorldX() + stepX;
            int nextY = getWorldY() + stepY;
            Rectangle destination = boundsAt(nextX, nextY);
            if (!chunk.canOccupy(destination, tileSize)) break;
            setPosition(nextX, nextY);
        }
        spriteFrame = (dashTicks / 2) % 2;
    }

    @Override
    public void draw(Graphics graphics, int tileSize) {
        if (!isAlive()) return;
        // Short flashes make damage and the invulnerability interval visible.
        if (invulnerabilityTicks > 0 && invulnerabilityTicks % 8 < 4) return;
        int drawX = screenX - (tileSize - getBodyWidth()) / 2;
        int drawY = screenY - (tileSize - getBodyHeight()) / 2;
        int drawWidth = tileSize;
        int drawHeight = tileSize;
        BufferedImage image;
        if (isAttacking() && weapon == WeaponType.SWORD) {
            int frame = attackTicks < 6 ? 0 : 1;
            image = attackSprites[direction.ordinal()][frame];
            if (frame == 1) {
                if (direction == Direction.UP || direction == Direction.DOWN) {
                    drawHeight *= 2;
                    if (direction == Direction.UP) drawY -= tileSize;
                } else {
                    drawWidth *= 2;
                    if (direction == Direction.LEFT) drawX -= tileSize;
                }
            }
        } else {
            image = walkingSprites[direction.ordinal()][spriteFrame];
        }
        if (isDashing() && graphics instanceof Graphics2D original) {
            Graphics2D echo = (Graphics2D) original.create();
            try {
                int trailX = dashDirection == Direction.RIGHT ? -1 : dashDirection == Direction.LEFT ? 1 : 0;
                int trailY = dashDirection == Direction.DOWN ? -1 : dashDirection == Direction.UP ? 1 : 0;
                for (int layer = 3; layer >= 1; layer--) {
                    echo.setComposite(AlphaComposite.SrcOver.derive(0.12f + (3 - layer) * 0.07f));
                    echo.drawImage(image, drawX + trailX * layer * 11,
                            drawY + trailY * layer * 11, drawWidth, drawHeight, null);
                }
            } finally {
                echo.dispose();
            }
        }
        graphics.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        drawHeldTool(graphics);
        graphics.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        int nameWidth = graphics.getFontMetrics().stringWidth(getDisplayName());
        graphics.setColor(new Color(7, 22, 31, 215));
        graphics.fillRoundRect(screenX + getBodyWidth() / 2 - nameWidth / 2 - 5,
                screenY + getBodyHeight() + 7, nameWidth + 10, 17, 6, 6);
        graphics.setColor(new Color(237, 240, 221));
        graphics.drawString(getDisplayName(), screenX + getBodyWidth() / 2 - nameWidth / 2,
                screenY + getBodyHeight() + 19);
    }

    private void drawHeldTool(Graphics graphics) {
        if (weapon == WeaponType.SWORD || !(graphics instanceof Graphics2D original)) return;
        Graphics2D tool = (Graphics2D) original.create();
        try {
            tool.translate(screenX + getBodyWidth() / 2, screenY + getBodyHeight() / 2);
            tool.rotate(switch (direction) { case RIGHT -> 0; case DOWN -> Math.PI / 2;
                case LEFT -> Math.PI; case UP -> -Math.PI / 2; });
            int extension = isAttacking() && attackTicks >= 6 ? 8 : 0;
            if (weapon == WeaponType.FISTS) {
                if (isAttacking()) {
                    tool.setColor(new Color(22, 49, 57));
                    tool.fillRoundRect(10 + extension, -3, 12, 11, 3, 3);
                    tool.setColor(new Color(187, 225, 209));
                    tool.fillRoundRect(12 + extension, -2, 9, 8, 3, 3);
                }
            } else if (weapon == WeaponType.PICKAXE) {
                tool.setColor(new Color(47, 32, 40));
                tool.setStroke(new java.awt.BasicStroke(5));
                tool.drawLine(11, 8, 25 + extension, -8);
                tool.setColor(new Color(186, 126, 75));
                tool.setStroke(new java.awt.BasicStroke(3));
                tool.drawLine(11, 8, 25 + extension, -8);
                tool.setColor(new Color(148, 213, 214));
                tool.drawLine(17 + extension, -13, 33 + extension, -4);
            } else {
                tool.setColor(new Color(20, 35, 49));
                tool.fillRect(11, -6, 22, 11);
                tool.fillRect(14, 4, 7, 8);
                tool.setColor(new Color(91, 169, 181));
                tool.fillRect(14, -4, 19, 4);
                tool.setColor(new Color(178, 247, 226));
                tool.fillRect(29, -3, 5, 3);
                if (isAttacking() && attackTicks < 6) {
                    tool.setColor(new Color(255, 223, 137, 210));
                    tool.fillOval(33, -7, 13, 13);
                }
            }
        } finally { tool.dispose(); }
    }
}
