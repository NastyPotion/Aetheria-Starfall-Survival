package com.game.core;

import com.game.entities.*;
import com.game.survival.Difficulty;

/** Difficulty changes encounters, never the resource recipes or diamond objectives. */
public final class EncounterFactory {
    private EncounterFactory() {
    }
    public static Enemy create(EnemyType type, String id, int level, Difficulty difficulty, Champion player) {
        Enemy enemy = switch (type) {
            case ALIEN -> new Alien(48, player);
            case BAT -> new Bat(48, player);
            case ORC -> new Orc(48, player);
            case SKELETON -> new Skeleton(48, player);
        };
        boolean monster = type == EnemyType.ALIEN || type == EnemyType.BAT;
        int baseHealth = monster ? level == 1 ? 2 : level == 2 ? 3 : 6 : level == 1 ? 3 : level == 2 ? 6 : 12;
        int baseStamina = monster ? level == 1 ? 45 : level == 2 ? 60 : 120 : level == 1 ? 45 : level == 2 ? 90 : 180;
        int speed = (monster ? level == 3 ? 4 : 2 : level == 1 ? 1 : 2) + difficulty.speedBonus();
        int health = level == 3 ? 2 * (int) Math.ceil((baseHealth / 2) * difficulty.healthMultiplier())
        : (int) Math.ceil(baseHealth * difficulty.healthMultiplier());
        int stamina = level == 3 ? 2 * (int) Math.ceil((baseStamina / 2) * difficulty.staminaMultiplier())
        : (int) Math.ceil(baseStamina * difficulty.staminaMultiplier());
        enemy.configure(id, health, speed, stamina, monster && level == 3);
        return enemy;
    }
}
