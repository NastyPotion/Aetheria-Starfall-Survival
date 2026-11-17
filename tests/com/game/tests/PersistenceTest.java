package com.game.tests;

import com.game.entities.EnemyType;
import com.game.persistence.FileSaveRepository;
import com.game.persistence.GameSnapshot;
import com.game.persistence.GameSnapshot.EnemyData;
import com.game.persistence.GameSnapshot.PlayerData;
import com.game.persistence.GameSnapshot.SurvivalData;
import com.game.persistence.SaveGameException;
import com.game.survival.CampaignProgress;
import com.game.survival.ChampionType;
import com.game.survival.Difficulty;
import com.game.survival.Inventory;
import com.game.survival.LevelDefinition;
import com.game.survival.Resource;
import com.game.survival.WeaponType;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Dependency-free survival, immutable-data and version-3 persistence checks. */
public final class PersistenceTest {
    private static int checks;

    public static void main(String[] arguments) throws Exception {
        testDefinitions();
        testInventory();
        testCampaign();
        testSnapshotImmutability();
        testSnapshotValidation();
        Path directory = Files.createTempDirectory(Files.createDirectories(Path.of("build")), "survival-save-test-");
        Path file = directory.resolve("nested").resolve("save.properties");
        FileSaveRepository repository = new FileSaveRepository(file);
        GameSnapshot original = sample();
        repository.save(original);
        GameSnapshot loaded = repository.load();
        check(Arrays.deepEquals(original.tiles(), loaded.tiles()), "Map round trip with all 18 tile IDs");
        check(original.player().equals(loaded.player()), "Champion and selected equipment round trip");
        check(original.enemies().equals(loaded.enemies()), "Indexed enemies and individual statistics round trip");
        check(original.survival().equals(loaded.survival()), "Complete survival progress round trip");
        check(CampaignProgress.fromData(loaded.survival()).toData().equals(original.survival()), "Campaign restore round trip");
        check(repository.getPath().equals(file.toAbsolutePath().normalize()), "Normalized slot path");
        Properties valid = read(file);
        check(valid.getProperty("version").equals("3"), "Writes version 3");
        testOldSaves(repository, file, valid);
        for (String[] corrupt : new String[][] {
                {"version", "99"}, {"map", "0,0"}, {"player.x", "-1"}, {"player.health", "0"},
                {"player.health", "13"}, {"player.champion", "UNKNOWN"}, {"player.weapon", "GUN"},
                {"enemies.count", "101"}, {"enemies.count", "-1"}, {"enemy.0.id", ""},
                {"enemy.1.id", "L2-0"}, {"enemy.0.type", "UNKNOWN"}, {"enemy.0.maxHealth", "129"},
                {"enemy.0.health", "17"}, {"enemy.0.speed", "0"}, {"enemy.0.speed", "11"},
                {"enemy.0.maxStamina", "601"}, {"enemy.0.maxStamina", "0"}, {"enemy.0.stamina", "129"},
                {"enemy.0.fastMonster", "yes"}, {"enemy.0.fastMonster", "true"},
                {"survival.level", "0"}, {"survival.level", "4"}, {"survival.difficulty", "IMPOSSIBLE"},
                {"survival.enemyKills", "-1"}, {"survival.monsterKills", "-1"}, {"survival.crafted", "no"},
                {"survival.bankedDiamonds", "-1"}, {"survival.totalBankedDiamonds", "0"},
                {"survival.elapsedTicks", "-1"}, {"survival.score", "10000001"}, {"survival.choiceShown", "yes"},
                {"inventory.DIAMOND", "-1"}, {"gathered.METAL", "-1"}, {"weapons", "FISTS,FISTS"},
                {"weapons", "SWORD"}, {"weapons", "FISTS,UNKNOWN"}}) {
            expectCorrupt(repository, file, valid, corrupt[0], corrupt[1]);
        }
        for (String missing : List.of("player.weapon", "survival.level", "survival.choiceShown", "inventory.ROCK",
                "gathered.MEAT", "weapons", "enemy.0.stamina")) {
            Properties corrupted = cloneProperties(valid);
            corrupted.remove(missing);
            write(file, corrupted);
            expectSaveFailure(repository::load, "Missing " + missing + " rejected");
        }
        Files.write(file, new byte[0]);
        expectSaveFailure(repository::load, "Empty save rejected");
        Files.write(file, new byte[1_048_577]);
        expectSaveFailure(repository::load, "Oversized save rejected");
        expectSaveFailure(new FileSaveRepository(directory.resolve("absent.properties"))::load, "Missing file is checked failure");
        Path blocked = directory.resolve("parent-is-file");
        Files.writeString(blocked, "preserve me");
        expectSaveFailure(() -> new FileSaveRepository(blocked.resolve("save.properties")).save(original), "Unwritable slot fails safely");
        check(Files.readString(blocked).equals("preserve me"), "Failed save preserves existing contents");
        repository.save(original);
        check(repository.load().survival().equals(original.survival()), "Valid save replaces corrupt slot");
        try (var children = Files.list(file.getParent())) {
            check(children.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")), "No temporary files remain");
        }
        testVariableEnemies(repository);
        System.out.println("PersistenceTest: " + checks + " checks passed.");
    }

    private static void testDefinitions() {
        LevelDefinition first = LevelDefinition.of(1);
        check(first.enemyKillTarget() == 1 && first.monsterKillTarget() == 2 && first.diamonds() == 1, "Level 1 targets");
        check(first.weapon() == WeaponType.PICKAXE && first.requiredGathered().get(Resource.MEAT) == 1, "Level 1 food achievement");
        check(!first.recipe().containsKey(Resource.MEAT), "Meat is never a crafting ingredient");
        check(first.recipe().equals(Map.of(Resource.METAL, 2, Resource.STONE, 2)), "Level 1 recipe exact");
        LevelDefinition second = LevelDefinition.of(2);
        check(second.enemyKillTarget() == 2 && second.monsterKillTarget() == 2 && second.diamonds() == 4
                && second.weapon() == WeaponType.SWORD, "Level 2 targets");
        check(second.recipe().equals(Map.of(Resource.METAL, 3, Resource.STONE, 3)), "Level 2 recipe exact");
        LevelDefinition third = LevelDefinition.of(3);
        check(third.enemyKillTarget() == 3 && third.monsterKillTarget() == 10 && third.diamonds() == 10
                && third.weapon() == WeaponType.GUN, "Level 3 targets");
        check(third.recipe().equals(Map.of(Resource.METAL, 4, Resource.STONE, 4, Resource.ROCK, 4)), "Level 3 recipe exact");
        expectUnsupported(() -> first.recipe().clear(), "Recipe immutable");
        expectInvalid(() -> LevelDefinition.of(4), "Unknown level rejected");
        check(Difficulty.EASY.extraSpawns() == 0 && Difficulty.MEDIUM.extraSpawns() == 2 && Difficulty.HARD.extraSpawns() == 4,
                "Difficulty spawn counts");
        check(Difficulty.MEDIUM.healthMultiplier() == 1.25 && Difficulty.HARD.healthMultiplier() == 1.5
                && Difficulty.MEDIUM.speedBonus() == 1 && Difficulty.HARD.speedBonus() == 2
                && Difficulty.HARD.staminaMultiplier() == 1.5, "Difficulty scaling data");
        check(ChampionType.VANGUARD.displayName().equals("Astra") && ChampionType.SCOUT.displayName().equals("Nyx")
                && ChampionType.ENGINEER.displayName().equals("Orion"), "Three original champion identities");
    }

    private static void testInventory() {
        Inventory inventory = new Inventory();
        check(inventory.hasWeapon(WeaponType.FISTS) && inventory.count(Resource.METAL) == 0, "Initial inventory and fists");
        inventory.add(Resource.METAL, 3);
        check(!inventory.spend(Map.of(Resource.METAL, 2, Resource.STONE, 1)), "Unaffordable recipe rejected");
        check(inventory.count(Resource.METAL) == 3, "Failed recipe does not partly consume items");
        inventory.add(Resource.STONE, 2);
        check(inventory.spend(Map.of(Resource.METAL, 2, Resource.STONE, 2)), "Affordable recipe succeeds");
        check(inventory.count(Resource.METAL) == 1 && inventory.count(Resource.STONE) == 0, "Recipe spends exact quantities");
        check(!inventory.remove(Resource.STONE, 1) && inventory.count(Resource.STONE) == 0, "Cannot remove unavailable items");
        inventory.grantWeapon(WeaponType.PICKAXE);
        inventory.add(Resource.DIAMOND, 4);
        inventory.clearCarried();
        check(Arrays.stream(Resource.values()).allMatch(resource -> inventory.count(resource) == 0), "Death clears all loose resources");
        check(inventory.hasWeapon(WeaponType.PICKAXE), "Permanent equipment survives clearing cargo");
        expectUnsupported(() -> inventory.copyItems().clear(), "Inventory view immutable");
        expectUnsupported(() -> inventory.getWeapons().clear(), "Equipment view immutable");
        expectInvalid(() -> inventory.add(Resource.MEAT, -1), "Negative gather rejected");
        expectInvalid(() -> inventory.spend(Map.of(Resource.METAL, -1)), "Negative price rejected");
        inventory.add(Resource.DIAMOND, Integer.MAX_VALUE);
        expectInvalid(() -> inventory.add(Resource.DIAMOND, 1), "Resource overflow rejected");
        check(inventory.count(Resource.DIAMOND) == Integer.MAX_VALUE, "Overflow preserves prior count");
    }

    private static void testCampaign() {
        CampaignProgress campaign = new CampaignProgress(Difficulty.HARD);
        check(!campaign.craft() && !campaign.advance() && campaign.depositDiamonds() == 0, "Progress gates reject incomplete level");
        campaign.recordKill(false);
        campaign.recordKill(true);
        campaign.recordKill(true);
        campaign.gather(Resource.MEAT, 1);
        check(campaign.eat() && campaign.getGathered(Resource.MEAT) == 1, "Eating retains the gathered-food achievement");
        campaign.gather(Resource.METAL, 2);
        campaign.gather(Resource.STONE, 2);
        check(campaign.craft() && campaign.getInventory().hasWeapon(WeaponType.PICKAXE), "Level 1 craft grants pickaxe");
        check(!campaign.craft(), "Recipe cannot be consumed twice");
        campaign.gather(Resource.DIAMOND, 2);
        check(campaign.depositDiamonds() == 2 && campaign.getBankedDiamonds() == 2
                && campaign.getTotalBankedDiamonds() == 2 && campaign.isReady(), "Deposit moves all diamonds and completes level");
        campaign.gather(Resource.METAL, 1);
        campaign.gather(Resource.DIAMOND, 3);
        campaign.setChoiceShown(true);
        campaign.tick();
        campaign.onDeath();
        check(campaign.getInventory().count(Resource.METAL) == 0 && campaign.getInventory().count(Resource.DIAMOND) == 0,
                "Death clears resources and pending diamonds");
        check(campaign.isReady() && campaign.getBankedDiamonds() == 2 && campaign.getEnemyKills() == 1
                && campaign.isCrafted() && campaign.isChoiceShown(), "Death preserves achievements, secured diamonds and equipment");
        check(campaign.advance() && campaign.getLevel() == 2, "Ready level advances");
        check(campaign.getBankedDiamonds() == 0 && campaign.getTotalBankedDiamonds() == 2 && !campaign.isChoiceShown()
                && campaign.getEnemyKills() == 0 && campaign.getMonsterKills() == 0 && !campaign.isCrafted(), "Advance resets only level state");
        check(campaign.getInventory().hasWeapon(WeaponType.PICKAXE) && campaign.getElapsedTicks() == 1, "Advance preserves equipment and campaign time");
        completeLevel(campaign);
        campaign.gather(Resource.DIAMOND, 4);
        campaign.depositDiamonds();
        check(campaign.advance() && campaign.getLevel() == 3, "Level 2 advances with four diamonds");
        completeLevel(campaign);
        campaign.gather(Resource.DIAMOND, 10);
        campaign.depositDiamonds();
        check(campaign.isReady() && !campaign.advance() && campaign.getLevel() == 3, "Level 3 finishes without creating level 4");
        check(campaign.getTotalBankedDiamonds() == 16 && campaign.getInventory().hasWeapon(WeaponType.GUN), "All campaign banking and weapons retained");
        check(CampaignProgress.fromData(campaign.toData()).toData().equals(campaign.toData()), "Full completed campaign restoration");
    }

    private static void testSnapshotImmutability() {
        GameSnapshot sample = sample();
        int[][] map = sample.tiles();
        List<EnemyData> enemies = new ArrayList<>(sample.enemies());
        GameSnapshot snapshot = new GameSnapshot(map, sample.player(), enemies, sample.survival());
        map[0][0] = 17;
        enemies.clear();
        check(snapshot.tiles()[0][0] == 0 && snapshot.enemies().size() == 3, "Snapshot copies incoming mutable values");
        int[][] exposed = snapshot.tiles();
        exposed[0][0] = 17;
        check(snapshot.tiles()[0][0] == 0, "Snapshot copies outgoing map");
        expectUnsupported(() -> snapshot.enemies().clear(), "Snapshot enemy list immutable");
        EnumMap<Resource, Integer> resources = new EnumMap<>(Resource.class);
        resources.put(Resource.METAL, 3);
        EnumSet<WeaponType> equipment = EnumSet.of(WeaponType.FISTS);
        SurvivalData progress = new SurvivalData(1, Difficulty.EASY, resources, equipment, resources, 0, 0, false, 0, 0, 0, 0, false);
        resources.clear(); equipment.add(WeaponType.GUN);
        check(progress.inventory().get(Resource.METAL) == 3 && progress.gathered().get(Resource.METAL) == 3
                && !progress.weapons().contains(WeaponType.GUN), "Survival data copies maps and sets");
        expectUnsupported(() -> progress.inventory().clear(), "Survival inventory immutable");
        expectUnsupported(() -> progress.gathered().clear(), "Achievement map immutable");
        expectUnsupported(() -> progress.weapons().clear(), "Owned weapons immutable");
    }

    private static void testSnapshotValidation() {
        GameSnapshot good = sample();
        expectInvalid(() -> new GameSnapshot(new int[1][1], good.player(), good.enemies(), good.survival()), "Invalid map dimensions");
        int[][] invalid = good.tiles(); invalid[0][0] = 18;
        expectInvalid(() -> new GameSnapshot(invalid, good.player(), good.enemies(), good.survival()), "Unknown tile rejected");
        for (int solid : new int[] {1, 13, 14, 15, 16, 17}) {
            int[][] blocked = good.tiles(); blocked[83][83] = solid;
            expectInvalid(() -> new GameSnapshot(blocked, good.player(), good.enemies(), good.survival()), "Solid tile " + solid + " blocks player");
        }
        List<EnemyData> duplicate = new ArrayList<>(good.enemies()); duplicate.set(1, duplicate.get(0));
        expectInvalid(() -> new GameSnapshot(good.tiles(), good.player(), duplicate, good.survival()), "Duplicate enemy IDs rejected");
        expectInvalid(() -> new SurvivalData(1, Difficulty.EASY, Map.of(Resource.MEAT, -1), Set.of(WeaponType.FISTS), Map.of(),
                0, 0, false, 0, 0, 0, 0, false), "Negative resource counter rejected");
        expectInvalid(() -> new SurvivalData(1, Difficulty.EASY, Map.of(), Set.of(WeaponType.FISTS), Map.of(),
                0, 0, true, 0, 0, 0, 0, false), "Cannot claim crafted equipment without achievements");
        expectInvalid(() -> new SurvivalData(1, Difficulty.EASY, Map.of(), Set.of(WeaponType.FISTS), Map.of(),
                0, 0, false, 1, 1, 0, 0, false), "Cannot bank diamonds before crafting");
        int[][] batBlocked = good.tiles(); batBlocked[91][91] = 17;
        List<EnemyData> bat = List.of(new EnemyData("flyer", EnemyType.BAT, 4_400, 4_400, 1, 2, 2, 40, 20, false));
        check(new GameSnapshot(batBlocked, good.player(), bat, good.survival()).enemies().size() == 1, "Flying enemy may overlap solid terrain");
    }

    private static void testOldSaves(FileSaveRepository repository, Path path, Properties valid) throws Exception {
        for (String version : List.of("1", "2")) {
            Properties old = cloneProperties(valid); old.setProperty("version", version); write(path, old);
            byte[] before = Files.readAllBytes(path);
            try { repository.load(); throw new AssertionError("Old relay campaign must not silently migrate"); }
            catch (SaveGameException expected) {
                check(expected.getMessage().contains("earlier relay-campaign"), "Old version explains incompatible campaign");
            }
            check(Arrays.equals(before, Files.readAllBytes(path)), "Failed old-version load preserves original file");
        }
    }

    private static void testVariableEnemies(FileSaveRepository repository) throws Exception {
        GameSnapshot base = sample();
        repository.save(new GameSnapshot(base.tiles(), base.player(), List.of(), base.survival()));
        check(repository.load().enemies().isEmpty(), "Zero remaining enemies supported");
        List<EnemyData> enemies = new ArrayList<>();
        for (int index = 0; index < 100; index++) enemies.add(new EnemyData("unit-" + index, EnemyType.ALIEN, 3_000, 3_000,
                index % 2 == 0 ? 0 : 128, 128, 10, 600, 600, true));
        repository.save(new GameSnapshot(base.tiles(), base.player(), enemies, base.survival()));
        check(repository.load().enemies().equals(enemies), "100 enemies with repeated types and maximum statistics supported");
        enemies.add(new EnemyData("overflow", EnemyType.BAT, 3_000, 3_000, 0, 1, 1, 0, 0, false));
        expectInvalid(() -> new GameSnapshot(base.tiles(), base.player(), enemies, base.survival()), "More than 100 enemies rejected");
    }

    private static GameSnapshot sample() {
        CampaignProgress campaign = new CampaignProgress(Difficulty.MEDIUM);
        completeLevel(campaign); campaign.gather(Resource.DIAMOND, 2); campaign.depositDiamonds(); campaign.advance();
        completeLevel(campaign); campaign.gather(Resource.DIAMOND, 4); campaign.depositDiamonds();
        campaign.gather(Resource.MEAT, 2); campaign.gather(Resource.ROCK, 3); campaign.setChoiceShown(true);
        for (int tick = 0; tick < 31; tick++) campaign.tick();
        int[][] map = new int[200][200];
        for (int tile = 0; tile < 18; tile++) map[0][tile] = tile;
        return new GameSnapshot(map, new PlayerData(4_000, 4_000, 8, ChampionType.SCOUT, WeaponType.SWORD), List.of(
                new EnemyData("L2-0", EnemyType.ORC, 4_320, 4_320, 9, 16, 3, 128, 73, false),
                new EnemyData("L2-1", EnemyType.ORC, 4_500, 4_500, 0, 16, 3, 128, 0, false),
                new EnemyData("L2-2", EnemyType.BAT, 4_400, 4_400, 4, 8, 4, 128, 128, true)), campaign.toData());
    }

    private static void completeLevel(CampaignProgress campaign) {
        LevelDefinition definition = campaign.getDefinition();
        for (int index = 0; index < definition.enemyKillTarget(); index++) campaign.recordKill(false);
        for (int index = 0; index < definition.monsterKillTarget(); index++) campaign.recordKill(true);
        for (Map.Entry<Resource, Integer> requirement : definition.requiredGathered().entrySet()) campaign.gather(requirement.getKey(), requirement.getValue());
        check(campaign.craft(), "Craft level " + campaign.getLevel() + " equipment");
    }

    private static void expectCorrupt(FileSaveRepository repository, Path file, Properties valid, String key, String value) throws Exception {
        Properties corrupted = cloneProperties(valid); corrupted.setProperty(key, value); write(file, corrupted);
        expectSaveFailure(repository::load, "Invalid " + key + " rejected");
    }
    private static Properties cloneProperties(Properties original) { Properties copy = new Properties(); copy.putAll(original); return copy; }
    private static Properties read(Path file) throws Exception { Properties values = new Properties(); try (InputStream input = Files.newInputStream(file)) { values.load(input); } return values; }
    private static void write(Path file, Properties values) throws Exception { try (OutputStream output = Files.newOutputStream(file)) { values.store(output, "Test fixture"); } }
    private static void expectSaveFailure(CheckedAction action, String message) throws Exception { try { action.run(); throw new AssertionError(message); } catch (SaveGameException expected) { check(!expected.getMessage().isBlank(), message); } }
    private static void expectInvalid(Runnable action, String message) { try { action.run(); throw new AssertionError(message); } catch (IllegalArgumentException expected) { checks++; } }
    private static void expectUnsupported(Runnable action, String message) { try { action.run(); throw new AssertionError(message); } catch (UnsupportedOperationException expected) { checks++; } }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
