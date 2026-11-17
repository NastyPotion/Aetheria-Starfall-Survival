package com.game.survival;

import java.util.Map;

/** Immutable campaign requirements; the food achievement is separate from crafting cost. */
public record LevelDefinition(int level, int enemyKillTarget, int monsterKillTarget,
        WeaponType weapon, int diamonds, Map<Resource, Integer> requiredGathered,
        Map<Resource, Integer> recipe) {

    public LevelDefinition {
        if (level < 1 || level > 3 || enemyKillTarget < 0 || monsterKillTarget < 0 || diamonds < 0) {
            throw new IllegalArgumentException("Invalid level requirements");
        }
        java.util.Objects.requireNonNull(weapon, "Weapon is required");
        requiredGathered = Map.copyOf(requiredGathered);
        recipe = Map.copyOf(recipe);
        for (int count : requiredGathered.values()) {
            if (count <= 0) throw new IllegalArgumentException("Gathering requirements must be positive");
        }
        for (int count : recipe.values()) {
            if (count <= 0) throw new IllegalArgumentException("Recipe amounts must be positive");
        }
    }

    public static LevelDefinition of(int level) {
        return switch (level) {
            case 1 -> new LevelDefinition(1, 1, 2, WeaponType.PICKAXE, 1,
                    Map.of(Resource.MEAT, 1, Resource.METAL, 2, Resource.STONE, 2),
                    Map.of(Resource.METAL, 2, Resource.STONE, 2));
            case 2 -> new LevelDefinition(2, 2, 2, WeaponType.SWORD, 4,
                    Map.of(Resource.METAL, 3, Resource.STONE, 3),
                    Map.of(Resource.METAL, 3, Resource.STONE, 3));
            case 3 -> new LevelDefinition(3, 3, 10, WeaponType.GUN, 10,
                    Map.of(Resource.METAL, 4, Resource.STONE, 4, Resource.ROCK, 4),
                    Map.of(Resource.METAL, 4, Resource.STONE, 4, Resource.ROCK, 4));
            default -> throw new IllegalArgumentException("Level must be 1, 2 or 3");
        };
    }
}
