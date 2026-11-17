package com.game.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;
import com.game.world.Chunk;

/** A short-lived gun pulse; the session resolves its impact through Damageable. */
public final class Projectile extends Entity {
    private final Direction direction;
    private final int damage;
    private int remainingTicks = 60;
    private boolean active = true;

    public Projectile(int x, int y, Direction direction, int damage) {
        super(10, 10, 10);
        if (damage <= 0) throw new IllegalArgumentException("Projectile damage must be positive.");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.damage = damage;
        setPosition(x, y);
    }

    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public int getDamage() { return damage; }

    @Override public void update(Chunk chunk, int tileSize) {
        if (!active) return;
        if (remainingTicks-- <= 0) { active = false; return; }
        int deltaX = switch (direction) { case LEFT -> -1; case RIGHT -> 1; default -> 0; };
        int deltaY = switch (direction) { case UP -> -1; case DOWN -> 1; default -> 0; };
        // Single-pixel checks protect the pulse even at tile edges and thin hitboxes.
        for (int step = 0; step < getSpeed(); step++) {
            int nextX = getWorldX() + deltaX;
            int nextY = getWorldY() + deltaY;
            Rectangle destination = boundsAt(nextX, nextY);
            if (!chunk.canOccupy(destination, tileSize)) { active = false; return; }
            setPosition(nextX, nextY);
        }
        if (remainingTicks == 0) active = false;
    }

    @Override public void draw(Graphics graphics, int tileSize) {
        drawPulse(graphics, getWorldX(), getWorldY());
    }

    public void draw(Graphics graphics, int tileSize, Champion viewer) {
        Objects.requireNonNull(viewer, "viewer");
        drawPulse(graphics, getWorldX() - viewer.getWorldX() + viewer.getScreenX(),
                getWorldY() - viewer.getWorldY() + viewer.getScreenY());
    }

    private void drawPulse(Graphics graphics, int screenX, int screenY) {
        if (!active) return;
        graphics.setColor(new Color(98, 234, 221, 45));
        graphics.fillOval(screenX - 6, screenY - 6, 22, 22);
        graphics.setColor(new Color(124, 242, 218, 180));
        graphics.fillOval(screenX - 2, screenY - 2, 14, 14);
        graphics.setColor(new Color(240, 255, 222));
        graphics.fillOval(screenX + 1, screenY + 1, 8, 8);
    }
}
