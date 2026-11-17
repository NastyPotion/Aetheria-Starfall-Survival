package com.game.persistence;

import com.game.entities.EnemyType;
import com.game.survival.ChampionType;
import com.game.survival.Difficulty;
import com.game.survival.LevelDefinition;
import com.game.survival.Resource;
import com.game.survival.WeaponType;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable, validated survival campaign data suitable for a background file worker. */
public record GameSnapshot(int[][] tiles, PlayerData player, List<EnemyData> enemies, SurvivalData survival) {
    private static final int WORLD_SIZE = 200;
    private static final int TILE_SIZE = 48;
    private static final int HITBOX_SIZE = 32;

    public static int worldSize() { return WORLD_SIZE; }

    public GameSnapshot {
        tiles = copyAndValidateTiles(tiles);
        Objects.requireNonNull(player, "Player is required");
        Objects.requireNonNull(survival, "Survival progress is required");
        enemies = List.copyOf(Objects.requireNonNull(enemies, "Enemies are required"));
        validatePosition(player.x(), player.y());
        requireGround(tiles, player.x(), player.y());
        Objects.requireNonNull(player.champion(), "Champion is required");
        Objects.requireNonNull(player.weapon(), "Equipped weapon is required");
        if (player.health() < 1 || player.health() > 12) {
            throw new IllegalArgumentException("Player health must be between 1 and 12");
        }
        if (!survival.weapons().contains(player.weapon())) {
            throw new IllegalArgumentException("The equipped weapon must be owned");
        }
        if (enemies.size() > 100) throw new IllegalArgumentException("A save supports at most 100 enemies");
        Set<String> ids = new HashSet<>();
        for (EnemyData enemy : enemies) {
            Objects.requireNonNull(enemy, "Enemy is required");
            Objects.requireNonNull(enemy.type(), "Enemy type is required");
            if (enemy.id() == null || enemy.id().isBlank() || enemy.id().length() > 64 || !ids.add(enemy.id())) {
                throw new IllegalArgumentException("Enemy IDs must be unique nonblank values of at most 64 characters");
            }
            validatePosition(enemy.x(), enemy.y());
            if (enemy.maxHealth() < 1 || enemy.maxHealth() > 128
                    || enemy.health() < 0 || enemy.health() > enemy.maxHealth()) {
                throw new IllegalArgumentException("Enemy health must fit its maximum of at most 128");
            }
            if (enemy.speed() < 1 || enemy.speed() > 10) {
                throw new IllegalArgumentException("Enemy speed must be between 1 and 10");
            }
            if (enemy.fastMonster() && enemy.type() != EnemyType.ALIEN && enemy.type() != EnemyType.BAT) {
                throw new IllegalArgumentException("Only monsters can have the fast-monster trait");
            }
            if (enemy.maxStamina() < 1 || enemy.maxStamina() > 600
                    || enemy.stamina() < 0 || enemy.stamina() > enemy.maxStamina()) {
                throw new IllegalArgumentException("Enemy stamina must fit its maximum of at most 600");
            }
            if (enemy.health() > 0 && enemy.type() != EnemyType.BAT) requireGround(tiles, enemy.x(), enemy.y());
        }
    }

    @Override public int[][] tiles() { return copyTiles(tiles); }

    public record PlayerData(int x, int y, int health, ChampionType champion, WeaponType weapon) { }

    public record EnemyData(String id, EnemyType type, int x, int y, int health,
            int maxHealth, int speed, int maxStamina, int stamina, boolean fastMonster) { }

    public record SurvivalData(int level, Difficulty difficulty, Map<Resource, Integer> inventory,
            Set<WeaponType> weapons, Map<Resource, Integer> gathered, int enemyKills,
            int monsterKills, boolean crafted, int bankedDiamonds, int totalBankedDiamonds,
            int elapsedTicks, int score, boolean choiceShown) {
        public SurvivalData {
            LevelDefinition definition = LevelDefinition.of(level);
            Objects.requireNonNull(difficulty, "Difficulty is required");
            inventory = copyResourceCounts(inventory);
            gathered = copyResourceCounts(gathered);
            weapons = Set.copyOf(Objects.requireNonNull(weapons, "Owned weapons are required"));
            if (!weapons.contains(WeaponType.FISTS)) throw new IllegalArgumentException("Fists must always be available");
            if (enemyKills < 0 || monsterKills < 0 || bankedDiamonds < 0 || totalBankedDiamonds < bankedDiamonds
                    || elapsedTicks < 0 || score < 0 || score > 10_000_000) {
                throw new IllegalArgumentException("Campaign counters must be nonnegative and internally consistent");
            }
            boolean achievements = enemyKills >= definition.enemyKillTarget()
                    && monsterKills >= definition.monsterKillTarget();
            for (Map.Entry<Resource, Integer> requirement : definition.requiredGathered().entrySet()) {
                achievements &= gathered.get(requirement.getKey()) >= requirement.getValue();
            }
            if (crafted && (!achievements || !weapons.contains(definition.weapon()))) {
                throw new IllegalArgumentException("Crafted equipment requires its achievements and owned weapon");
            }
            if (bankedDiamonds > 0 && !crafted) {
                throw new IllegalArgumentException("Current-level diamonds cannot be banked before crafting");
            }
        }
    }

    private static Map<Resource, Integer> copyResourceCounts(Map<Resource, Integer> source) {
        Objects.requireNonNull(source, "Resource counts are required");
        EnumMap<Resource, Integer> copy = new EnumMap<>(Resource.class);
        for (Map.Entry<Resource, Integer> entry : source.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "Resource is required");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Resource counts cannot be negative");
            }
        }
        for (Resource resource : Resource.values()) copy.put(resource, source.getOrDefault(resource, 0));
        return Map.copyOf(copy);
    }

    private static int[][] copyAndValidateTiles(int[][] source) {
        Objects.requireNonNull(source, "Map is required");
        if (source.length != WORLD_SIZE) throw new IllegalArgumentException("Map must have 200 rows");
        int[][] copy = new int[WORLD_SIZE][WORLD_SIZE];
        for (int row = 0; row < WORLD_SIZE; row++) {
            if (source[row] == null || source[row].length != WORLD_SIZE) {
                throw new IllegalArgumentException("Map rows must have 200 columns");
            }
            for (int col = 0; col < WORLD_SIZE; col++) {
                int tile = source[row][col];
                if (tile < 0 || tile > 17) throw new IllegalArgumentException("Unknown tile identifier: " + tile);
                copy[row][col] = tile;
            }
        }
        return copy;
    }

    private static int[][] copyTiles(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) copy[row] = source[row].clone();
        return copy;
    }

    private static void validatePosition(int x, int y) {
        int last = WORLD_SIZE * TILE_SIZE - HITBOX_SIZE;
        if (x < 0 || y < 0 || x > last || y > last) {
            throw new IllegalArgumentException("Entity position lies outside the world");
        }
    }

    private static void requireGround(int[][] tiles, int x, int y) {
        int left = x / TILE_SIZE;
        int right = (x + HITBOX_SIZE - 1) / TILE_SIZE;
        int top = y / TILE_SIZE;
        int bottom = (y + HITBOX_SIZE - 1) / TILE_SIZE;
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                int tile = tiles[row][col];
                if (tile == 1 || tile >= 13 && tile <= 17) {
                    throw new IllegalArgumentException("Ground entity cannot stand inside solid terrain");
                }
            }
        }
    }
}
