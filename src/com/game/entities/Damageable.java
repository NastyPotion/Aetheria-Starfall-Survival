package com.game.entities;

/** A small interface shared by the player and enemies in the combat system. */
public interface Damageable {
    void takeDamage(int amount);
    int getCurrentHealth();
    int getMaxHealth();
    boolean isAlive();
}
