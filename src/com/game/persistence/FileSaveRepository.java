package com.game.persistence;

import com.game.entities.EnemyType;
import com.game.survival.ChampionType;
import com.game.survival.Difficulty;
import com.game.survival.Resource;
import com.game.survival.WeaponType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Version 3 Properties: complete map, selected champion/equipment, indexed enemies
 * with individual statistics and survival campaign state. Earlier relay-campaign
 * saves are rejected explicitly rather than silently discarding their progress.
 */
public final class FileSaveRepository implements SaveRepository {
    private static final int MAX_SAVE_BYTES = 1_048_576;
    private static final String VERSION = "3";
    private final Path path;

    public FileSaveRepository(Path path) {
        this.path = Objects.requireNonNull(path, "Save path is required").toAbsolutePath().normalize();
    }

    public Path getPath() { return path; }

    @Override public void save(GameSnapshot snapshot) throws SaveGameException {
        Objects.requireNonNull(snapshot, "Snapshot is required");
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "aetheria-", ".tmp");
            Properties properties = encode(snapshot);
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Aetheria survival campaign - save version 3");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | SecurityException exception) {
            throw new SaveGameException("Could not save the campaign: " + exception.getMessage(), exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException exception) {
                    System.err.println("Could not remove temporary save: " + exception.getMessage());
                }
            }
        }
    }

    @Override public GameSnapshot load() throws SaveGameException {
        try {
            long length = Files.size(path);
            if (length <= 0 || length > MAX_SAVE_BYTES) {
                throw new SaveGameException("Save file is empty or larger than the supported limit");
            }
            byte[] bytes;
            try (InputStream input = Files.newInputStream(path)) {
                bytes = input.readNBytes(MAX_SAVE_BYTES + 1);
            }
            if (bytes.length == 0 || bytes.length > MAX_SAVE_BYTES) {
                throw new SaveGameException("Save file is empty or larger than the supported limit");
            }
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(bytes));
            String version = required(properties, "version");
            if ("1".equals(version) || "2".equals(version)) {
                throw new SaveGameException("This is an earlier relay-campaign save (version " + version
                        + "). It cannot be used for the survival campaign. Your file has not been changed; begin a new campaign.");
            }
            if (!VERSION.equals(version)) throw new SaveGameException("This save uses an unsupported version");
            int[][] tiles = decodeMap(required(properties, "map"));
            GameSnapshot.PlayerData player = new GameSnapshot.PlayerData(number(properties, "player.x"),
                    number(properties, "player.y"), number(properties, "player.health"),
                    ChampionType.valueOf(required(properties, "player.champion")),
                    WeaponType.valueOf(required(properties, "player.weapon")));
            int count = number(properties, "enemies.count");
            if (count < 0 || count > 100) throw new IllegalArgumentException("Enemy count must be between 0 and 100");
            List<GameSnapshot.EnemyData> enemies = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String prefix = "enemy." + index + ".";
                enemies.add(new GameSnapshot.EnemyData(required(properties, prefix + "id"),
                        EnemyType.valueOf(required(properties, prefix + "type")),
                        number(properties, prefix + "x"), number(properties, prefix + "y"),
                        number(properties, prefix + "health"), number(properties, prefix + "maxHealth"),
                        number(properties, prefix + "speed"), number(properties, prefix + "maxStamina"),
                        number(properties, prefix + "stamina"), booleanValue(properties, prefix + "fastMonster")));
            }
            GameSnapshot.SurvivalData survival = new GameSnapshot.SurvivalData(
                    number(properties, "survival.level"), Difficulty.valueOf(required(properties, "survival.difficulty")),
                    resourceMap(properties, "inventory."), weaponSet(required(properties, "weapons")),
                    resourceMap(properties, "gathered."), number(properties, "survival.enemyKills"),
                    number(properties, "survival.monsterKills"), booleanValue(properties, "survival.crafted"),
                    number(properties, "survival.bankedDiamonds"), number(properties, "survival.totalBankedDiamonds"),
                    number(properties, "survival.elapsedTicks"), number(properties, "survival.score"),
                    booleanValue(properties, "survival.choiceShown"));
            return new GameSnapshot(tiles, player, enemies, survival);
        } catch (IOException | SecurityException exception) {
            throw new SaveGameException("Could not load the campaign: " + exception.getMessage(), exception);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SaveGameException("Save data is invalid: " + exception.getMessage(), exception);
        }
    }

    private static Properties encode(GameSnapshot snapshot) {
        Properties properties = new Properties();
        properties.setProperty("version", VERSION);
        int size = GameSnapshot.worldSize();
        StringBuilder map = new StringBuilder(size * size * 2);
        for (int[] row : snapshot.tiles()) {
            for (int tile : row) {
                if (map.length() > 0) map.append(',');
                map.append(tile);
            }
        }
        properties.setProperty("map", map.toString());
        GameSnapshot.PlayerData player = snapshot.player();
        properties.setProperty("player.x", Integer.toString(player.x()));
        properties.setProperty("player.y", Integer.toString(player.y()));
        properties.setProperty("player.health", Integer.toString(player.health()));
        properties.setProperty("player.champion", player.champion().name());
        properties.setProperty("player.weapon", player.weapon().name());
        properties.setProperty("enemies.count", Integer.toString(snapshot.enemies().size()));
        for (int index = 0; index < snapshot.enemies().size(); index++) {
            GameSnapshot.EnemyData enemy = snapshot.enemies().get(index);
            String prefix = "enemy." + index + ".";
            properties.setProperty(prefix + "id", enemy.id());
            properties.setProperty(prefix + "type", enemy.type().name());
            properties.setProperty(prefix + "x", Integer.toString(enemy.x()));
            properties.setProperty(prefix + "y", Integer.toString(enemy.y()));
            properties.setProperty(prefix + "health", Integer.toString(enemy.health()));
            properties.setProperty(prefix + "maxHealth", Integer.toString(enemy.maxHealth()));
            properties.setProperty(prefix + "speed", Integer.toString(enemy.speed()));
            properties.setProperty(prefix + "maxStamina", Integer.toString(enemy.maxStamina()));
            properties.setProperty(prefix + "stamina", Integer.toString(enemy.stamina()));
            properties.setProperty(prefix + "fastMonster", Boolean.toString(enemy.fastMonster()));
        }
        GameSnapshot.SurvivalData survival = snapshot.survival();
        properties.setProperty("survival.level", Integer.toString(survival.level()));
        properties.setProperty("survival.difficulty", survival.difficulty().name());
        properties.setProperty("survival.enemyKills", Integer.toString(survival.enemyKills()));
        properties.setProperty("survival.monsterKills", Integer.toString(survival.monsterKills()));
        properties.setProperty("survival.crafted", Boolean.toString(survival.crafted()));
        properties.setProperty("survival.bankedDiamonds", Integer.toString(survival.bankedDiamonds()));
        properties.setProperty("survival.totalBankedDiamonds", Integer.toString(survival.totalBankedDiamonds()));
        properties.setProperty("survival.elapsedTicks", Integer.toString(survival.elapsedTicks()));
        properties.setProperty("survival.score", Integer.toString(survival.score()));
        properties.setProperty("survival.choiceShown", Boolean.toString(survival.choiceShown()));
        for (Resource resource : Resource.values()) {
            properties.setProperty("inventory." + resource.name(), Integer.toString(survival.inventory().get(resource)));
            properties.setProperty("gathered." + resource.name(), Integer.toString(survival.gathered().get(resource)));
        }
        StringJoiner weapons = new StringJoiner(",");
        for (WeaponType weapon : WeaponType.values()) if (survival.weapons().contains(weapon)) weapons.add(weapon.name());
        properties.setProperty("weapons", weapons.toString());
        return properties;
    }

    private static int[][] decodeMap(String encoded) {
        String[] values = encoded.split(",", -1);
        int size = GameSnapshot.worldSize();
        if (values.length != size * size) throw new IllegalArgumentException("Map must contain exactly 40,000 tiles");
        int[][] tiles = new int[size][size];
        for (int index = 0; index < values.length; index++) tiles[index / size][index % size] = Integer.parseInt(values[index]);
        return tiles;
    }

    private static Map<Resource, Integer> resourceMap(Properties properties, String prefix) {
        EnumMap<Resource, Integer> values = new EnumMap<>(Resource.class);
        for (Resource resource : Resource.values()) values.put(resource, number(properties, prefix + resource.name()));
        return values;
    }

    private static Set<WeaponType> weaponSet(String encoded) {
        EnumSet<WeaponType> weapons = EnumSet.noneOf(WeaponType.class);
        for (String value : encoded.split(",", -1)) {
            if (!weapons.add(WeaponType.valueOf(value))) throw new IllegalArgumentException("Duplicate owned weapon");
        }
        return weapons;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing value: " + key);
        return value;
    }

    private static int number(Properties properties, String key) { return Integer.parseInt(required(properties, key)); }

    private static boolean booleanValue(Properties properties, String key) {
        return switch (required(properties, key)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean: " + key);
        };
    }
}
