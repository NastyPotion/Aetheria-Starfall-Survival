package com.game.entities;

import java.awt.Graphics;
import java.awt.Rectangle;
import com.game.world.Chunk;

/** Common position, speed and collision rules for objects in the world. */
public abstract class Entity {
    private int x;
    private int y;
    private int speed;
    private final int bodyWidth;
    private final int bodyHeight;

    protected Entity(int speed, int bodyWidth, int bodyHeight) {
        this(speed, bodyWidth, bodyHeight, 0, 0);
    }

    protected Entity(int speed, int bodyWidth, int bodyHeight, int worldX, int worldY) {
        if (speed <= 0 || bodyWidth <= 0 || bodyHeight <= 0) {
            throw new IllegalArgumentException("Entity speed and body dimensions must be positive.");
        }
        validatePosition(worldX, worldY, bodyWidth, bodyHeight);
        this.speed = speed;
        this.bodyWidth = bodyWidth;
        this.bodyHeight = bodyHeight;
        x = worldX;
        y = worldY;
    }

    public final int getWorldX() { return x; }
    public final int getWorldY() { return y; }
    public final int getSpeed() { return speed; }

    protected final int getBodyWidth() { return bodyWidth; }
    protected final int getBodyHeight() { return bodyHeight; }

    /** Subclasses request a valid speed without accessing the stored field. */
    protected final void setSpeed(int speed) {
        if (speed <= 0) {
            throw new IllegalArgumentException("Entity speed must be positive.");
        }
        this.speed = speed;
    }

    /** Return a copy so callers cannot mutate an entity's collision area. */
    public final Rectangle getBounds() {
        return boundsAt(x, y);
    }

    /** A candidate footprint can be checked before the entity moves. */
    protected final Rectangle boundsAt(int worldX, int worldY) {
        return new Rectangle(worldX, worldY, bodyWidth, bodyHeight);
    }

    public final void setPosition(int worldX, int worldY) {
        validatePosition(worldX, worldY, bodyWidth, bodyHeight);
        x = worldX;
        y = worldY;
    }

    private static void validatePosition(int worldX, int worldY, int bodyWidth, int bodyHeight) {
        if (worldX < 0 || worldY < 0
                || worldX > Integer.MAX_VALUE - bodyWidth
                || worldY > Integer.MAX_VALUE - bodyHeight) {
            throw new IllegalArgumentException("Entity position must be nonnegative and within integer bounds.");
        }
    }
    
    public abstract void update(Chunk chunk, int tileSize);
    public abstract void draw(Graphics g, int tileSize);
}
