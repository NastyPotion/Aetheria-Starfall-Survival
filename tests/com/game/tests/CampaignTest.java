package com.game.tests;

import com.game.core.EncounterFactory;
import com.game.core.GameConfig;
import com.game.core.GamePanel;
import com.game.core.GameSession;
import com.game.core.GameState;
import com.game.core.KeyHandler;
import com.game.entities.Champion;
import com.game.entities.Enemy;
import com.game.entities.EnemyType;
import com.game.persistence.FileSaveRepository;
import com.game.persistence.GameSnapshot;
import com.game.persistence.GameSnapshot.EnemyData;
import com.game.persistence.GameSnapshot.PlayerData;
import com.game.persistence.SaveGameException;
import com.game.persistence.SaveRepository;
import com.game.survival.CampaignProgress;
import com.game.survival.ChampionType;
import com.game.survival.Difficulty;
import com.game.survival.LevelDefinition;
import com.game.survival.Resource;
import com.game.survival.WeaponType;
import com.game.world.TileType;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Session integration: actual combat, harvesting, ship banking and all three level choices. */
public final class CampaignTest {
    private static final int TILE = GameConfig.TILE_SIZE;
    private static final JPanel SOURCE = new JPanel();
    private static int checks;

    public static void main(String[] arguments) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                testSelectionsAndPause();
                testHarvestingAndFood();
                testGunInTheSession();
                testBankingAndDeath();
                testFullFirstLevelAndCampaignChoices();
                testSnapshotAndFileRoundTrip();
            } catch (Exception exception) {
                throw new IllegalStateException("Campaign integration check failed", exception);
            }
        });
        testAsyncPanelTransfers();
        testMainMenuSaves();
        System.out.println("CampaignTest: " + checks + " checks passed.");
    }

    private static void testSelectionsAndPause() {
        for (ChampionType type : ChampionType.values()) for (Difficulty difficulty : Difficulty.values()) {
            KeyHandler keys = new KeyHandler();
            GameSession game = new GameSession(keys, type, difficulty, 2026);
            check(game.getPlayer().getChampionType() == type && game.getCampaign().getDifficulty() == difficulty,
                    "Selected champion and difficulty reach the actual session");
            check(game.getPlayer().getCurrentHealth() == 4, "Every selection begins with two hearts");
            check(game.getEnemies().size() == 3 + difficulty.extraSpawns(), "Difficulty changes the actual number of hostiles");
            for (Enemy enemy : game.getEnemies()) {
                check(game.getChunk().canOccupy(enemy.getBounds(), TILE), "Every generated hostile starts on clear terrain");
            }
            game.start();
            game.update();
            int x = game.getPlayer().getWorldX();
            int elapsed = game.getElapsedTicks();
            game.pause();
            press(keys, KeyEvent.VK_D);
            for (int tick = 0; tick < 10; tick++) game.update();
            check(game.getPlayer().getWorldX() == x && game.getElapsedTicks() == elapsed, "Pause freezes both the explorer and campaign clock");
        }
    }

    private static void testHarvestingAndFood() {
        KeyHandler keys = new KeyHandler();
        int[][] tiles = new int[200][200];
        tiles[83][83] = TileType.TREE_METAL.getId();
        GameSession game = fixture(keys, new CampaignProgress(Difficulty.EASY), tiles, List.of(), WeaponType.FISTS);
        game.getPlayer().setPosition(83 * TILE, 84 * TILE);
        game.start();
        check(game.interact(), "F interaction harvests metal hidden in a tree");
        check(game.getCampaign().getInventory().count(Resource.METAL) == 1
                && game.getCampaign().getGathered(Resource.METAL) == 1, "Harvesting updates carried cargo and persistent achievements");
        check(game.getChunk().getTileAt(83, 83) == TileType.GRASS, "Harvested tree is consumed exactly once");
        check(!game.interact(), "An exhausted node cannot duplicate its resource");

        tiles = new int[200][200];
        tiles[83][83] = TileType.ROCK_DIAMOND.getId();
        game = fixture(keys = new KeyHandler(), new CampaignProgress(Difficulty.EASY), tiles, List.of(), WeaponType.FISTS);
        game.getPlayer().setPosition(83 * TILE, 84 * TILE);
        game.start();
        check(!game.interact() && game.getCampaign().getInventory().count(Resource.DIAMOND) == 0,
                "An uncrafted explorer cannot mine diamonds");
        game.getCampaign().getInventory().grantWeapon(WeaponType.PICKAXE);
        check(game.equip(WeaponType.PICKAXE) && game.interact(), "Owned crafted pickaxe enables rock mining");
        check(game.getCampaign().getInventory().count(Resource.DIAMOND) == 1 && game.getCampaign().getBankedDiamonds() == 0,
                "Mined diamonds stay carried until safely returned");
        game.getCampaign().gather(Resource.MEAT, 1);
        check(game.eat() && game.getPlayer().getCurrentHealth() == 8, "Eating one meat restores exactly two hearts");
        check(game.getCampaign().getInventory().count(Resource.MEAT) == 0, "Eating consumes the carried food");
        check(!game.eat(), "Eating cannot create food that is not owned");
    }

    private static void testGunInTheSession() {
        KeyHandler keys = new KeyHandler();
        CampaignProgress progress = new CampaignProgress(3, Difficulty.EASY);
        progress.getInventory().grantWeapon(WeaponType.GUN);
        EnemyData target = new EnemyData("gun-target", EnemyType.ALIEN, 4_000, 4_100, 6, 6, 4, 120, 120, true);
        GameSession game = fixture(keys, progress, new int[200][200], List.of(target), WeaponType.GUN);
        game.getPlayer().setPosition(4_000, 4_000);
        game.start();
        press(keys, KeyEvent.VK_E);
        game.update();
        check(!game.getProjectiles().isEmpty() && game.getPlayer().getAttackBounds().isEmpty(), "Gun input launches a projectile instead of a melee hitbox");
        check(game.getEnemies().get(0).getCurrentHealth() == 6, "A distant target is not damaged before the pulse reaches it");
        for (int tick = 0; tick < 55 && game.getEnemies().get(0).isAlive(); tick++) game.update();
        release(keys, KeyEvent.VK_E);
        check(!game.getEnemies().get(0).isAlive() && game.getCampaign().getMonsterKills() == 1,
                "Real gun pulses kill the monster and credit the campaign achievement");
        check(game.getState() == GameState.PLAYING && game.getPlayer().isAlive(), "Ranged combat keeps the two-heart explorer alive");
    }

    private static void testBankingAndDeath() {
        KeyHandler keys = new KeyHandler();
        CampaignProgress progress = new CampaignProgress(Difficulty.EASY);
        GameSession game = fixture(keys, progress, new int[200][200], List.of(), WeaponType.FISTS);
        progress = game.getCampaign();
        game.start();
        arrangeAchievements(progress);
        check(game.craft(), "Completed achievements and materials craft the first pickaxe");
        progress.gather(Resource.DIAMOND, 1);
        game.getPlayer().setPosition(4_000, 4_000);
        check(!game.interact() && progress.getBankedDiamonds() == 0 && !progress.isReady(), "Diamonds cannot be banked away from the ship");
        game.getPlayer().setPosition(GameSession.SHIP_X, GameSession.SHIP_Y);
        check(game.interact() && progress.getBankedDiamonds() == 1 && progress.isReady(), "Returning alive and interacting with the ship secures diamonds");
        check(game.getState() == GameState.LEVEL_COMPLETE, "Completed banked objectives offer a level choice");
        game.stayOnLevel();
        progress.gather(Resource.DIAMOND, 3);
        progress.gather(Resource.METAL, 2);
        int kills = progress.getEnemyKills();
        game.getPlayer().takeDamage(4);
        game.update();
        check(game.getState() == GameState.LOST && progress.getInventory().count(Resource.DIAMOND) == 0,
                "Dying drops every carried diamond");
        check(progress.getInventory().count(Resource.METAL) == 0 && progress.getBankedDiamonds() == 1,
                "Loose supplies are lost while the ship bank remains safe");
        check(progress.getEnemyKills() == kills && progress.isCrafted()
                && progress.getInventory().hasWeapon(WeaponType.PICKAXE), "Achievements and earned equipment survive a lost life");
        try { game.snapshot(); throw new AssertionError("A defeated life cannot be saved"); }
        catch (IllegalStateException expected) { checks++; }
        game.retry();
        check(game.getState() == GameState.PLAYING && game.getPlayer().getCurrentHealth() == 4,
                "Retry starts a fresh two-heart life");
        check(progress.getBankedDiamonds() == 1 && game.getPlayer().getWeaponType() == WeaponType.PICKAXE,
                "Retry preserves banked progress and the equipped crafted tool");
    }

    private static void testFullFirstLevelAndCampaignChoices() {
        KeyHandler keys = new KeyHandler();
        GameSession game = new GameSession(keys, ChampionType.VANGUARD, Difficulty.EASY, 2026);
        game.start();
        for (Enemy enemy : game.getEnemies()) {
            int foodBefore = game.getCampaign().getGathered(Resource.MEAT);
            defeatUsingInput(game, keys, enemy);
            check(game.getPlayer().getWeaponType() == WeaponType.FISTS, "First-level kills are possible before any weapon is granted");
            int col = enemy.getWorldX() / TILE;
            int row = enemy.getWorldY() / TILE;
            if (game.getChunk().getTileAt(col, row) == TileType.MEAT) {
                game.getPlayer().setPosition(col * TILE, row * TILE);
                game.update();
            }
            check(game.getCampaign().getGathered(Resource.MEAT) > foodBefore,
                    "Real combat drops collectable food: " + enemy.getType() + " tile=" + game.getChunk().getTileAt(col, row));
            if (game.getPlayer().getCurrentHealth() < game.getPlayer().getMaxHealth()) game.eat();
        }
        check(game.getCampaign().getEnemyKills() == 1 && game.getCampaign().getMonsterKills() == 2,
                "Actual unarmed combat completes the first enemy and monster achievements");
        for (int col = 93; col <= 96; col++) {
            check(game.getChunk().canOccupy(new Rectangle(col * TILE, 103 * TILE, 32, 32), TILE), "Guaranteed supplies are reachable");
            game.getPlayer().setPosition(col * TILE, 103 * TILE);
            game.update();
        }
        check(game.getCampaign().getGathered(Resource.METAL) == 2 && game.getCampaign().getGathered(Resource.STONE) == 2,
                "World pickups complete the first gathering achievements");
        check(game.craft() && game.getPlayer().getWeaponType() == WeaponType.PICKAXE,
                "Kills, food and gathered materials unlock the first crafted tool");
        game.getPlayer().setPosition(93 * TILE, 110 * TILE);
        check(game.interact() && game.getCampaign().getInventory().count(Resource.DIAMOND) == 1,
                "Real interaction mines the guaranteed diamond rock with the crafted pickaxe");
        check(game.getCampaign().getBankedDiamonds() == 0 && !game.getCampaign().isReady(), "A carried diamond does not complete the level prematurely");
        game.getPlayer().setPosition(GameSession.SHIP_X, GameSession.SHIP_Y);
        check(game.interact() && game.getState() == GameState.LEVEL_COMPLETE, "Returning the mined diamond completes the first expedition");
        game.stayOnLevel();
        for (int tick = 0; tick < 10; tick++) game.update();
        check(game.getState() == GameState.PLAYING && game.getCampaign().getLevel() == 1, "Choosing Stay permits continued play without a forced transition");
        check(game.requestLevelChoice() && game.advanceLevel(), "The player can later return and choose to advance");
        check(game.getCampaign().getLevel() == 2 && game.getCampaign().getTotalBankedDiamonds() == 1
                && game.getCampaign().getBankedDiamonds() == 0, "Advance starts the next checklist and retains the lifetime diamond bank");
        check(game.getCampaign().getInventory().hasWeapon(WeaponType.PICKAXE), "The next expedition keeps previously crafted tools");

        completeArrangedLevel(game);
        check(game.getPlayer().getWeaponType() == WeaponType.SWORD && game.getCampaign().getBankedDiamonds() == 4,
                "Level two crafts its sword and banks its four-diamond target");
        check(game.advanceLevel() && game.getCampaign().getLevel() == 3, "The second completed level offers the third expedition");
        check(game.getEnemies().stream().filter(Enemy::isMonster).count() == 10
                && game.getEnemies().stream().filter(enemy -> !enemy.isMonster()).count() == 3,
                "Level three actually populates its three enemy and ten monster objectives");
        check(game.getEnemies().stream().filter(Enemy::isMonster).allMatch(Enemy::isFastMonster), "Final-level monsters use their fast encounter configuration");
        completeArrangedLevel(game);
        check(game.getPlayer().getWeaponType() == WeaponType.GUN && game.getCampaign().getBankedDiamonds() == 10,
                "Level three crafts the gun and secures its ten-diamond objective");
        check(game.advanceLevel() && game.getState() == GameState.WON, "Final completion returns the survivor home");
        check(game.getCampaign().getTotalBankedDiamonds() == 15, "The campaign banks exactly one plus four plus ten diamonds");
        check(game.drainEvents().stream().anyMatch(event -> event.text().equals(GameSession.CONGRATULATIONS)),
                "Final victory emits the requested congratulations message");
        game.stayOnLevel();
        check(game.getState() == GameState.PLAYING && game.getCampaign().getLevel() == 3,
                "The player can continue the final world after finishing the campaign");
    }

    private static void defeatUsingInput(GameSession game, KeyHandler keys, Enemy target) {
        for (int swing = 0; target.isAlive() && swing < 16; swing++) {
            keys.clear();
            int[][] approaches = {{target.getWorldX(), target.getWorldY() - 44, KeyEvent.VK_S},
                    {target.getWorldX(), target.getWorldY() + 44, KeyEvent.VK_W},
                    {target.getWorldX() - 44, target.getWorldY(), KeyEvent.VK_D},
                    {target.getWorldX() + 44, target.getWorldY(), KeyEvent.VK_A}};
            int[] selected = Arrays.stream(approaches).filter(candidate ->
                    game.getChunk().canOccupy(new Rectangle(candidate[0], candidate[1], 32, 32), TILE)).findFirst().orElseThrow();
            // Travel is shortened; every hit, enemy response and kill credit still
            // passes through real keyboard input and ordinary session updates.
            game.getPlayer().setPosition(selected[0], selected[1]);
            press(keys, selected[2]); game.update(); release(keys, selected[2]);
            press(keys, KeyEvent.VK_E);
            for (int tick = 0; tick < 26; tick++) game.update();
            release(keys, KeyEvent.VK_E);
            check(game.getState() == GameState.PLAYING, "The explorer survives a real unarmed exchange");
        }
        check(!target.isAlive(), "Keyboard attacks defeat " + target.getDisplayName());
    }

    private static void completeArrangedLevel(GameSession game) {
        CampaignProgress progress = game.getCampaign();
        arrangeAchievements(progress);
        check(game.craft(), "Current level requirements craft its specified weapon");
        progress.gather(Resource.DIAMOND, progress.getDefinition().diamonds());
        check(!progress.isReady(), "Next level still requires a safe deposit");
        game.getPlayer().setPosition(GameSession.SHIP_X, GameSession.SHIP_Y);
        check(game.interact() && game.getState() == GameState.LEVEL_COMPLETE, "A successful ship deposit opens the current level choice");
    }

    private static void arrangeAchievements(CampaignProgress progress) {
        LevelDefinition rule = progress.getDefinition();
        while (progress.getEnemyKills() < rule.enemyKillTarget()) progress.recordKill(false);
        while (progress.getMonsterKills() < rule.monsterKillTarget()) progress.recordKill(true);
        rule.requiredGathered().forEach((resource, amount) -> {
            int missing = amount - progress.getGathered(resource);
            if (missing > 0) progress.gather(resource, missing);
        });
        rule.recipe().forEach((resource, amount) -> {
            int missing = amount - progress.getInventory().count(resource);
            if (missing > 0) progress.gather(resource, missing);
        });
    }

    private static void testSnapshotAndFileRoundTrip() throws Exception {
        CampaignProgress progress = new CampaignProgress(2, Difficulty.HARD);
        progress.getInventory().grantWeapon(WeaponType.PICKAXE);
        progress.gather(Resource.MEAT, 2);
        EnemyData enemy = new EnemyData("saved-sentinel", EnemyType.ORC, 4_000, 4_048, 6, 9, 4, 135, 38, false);
        GameSession game = fixture(new KeyHandler(), progress, new int[200][200], List.of(enemy), WeaponType.PICKAXE);
        GameSnapshot before = game.snapshot();
        FileSaveRepository repository = new FileSaveRepository(Path.of("build", "tests", "survival-campaign.properties"));
        repository.save(before);
        GameSession loaded = GameSession.fromSnapshot(new KeyHandler(), repository.load());
        GameSnapshot after = loaded.snapshot();
        check(loaded.getState() == GameState.PAUSED, "A loaded survival campaign waits safely for the player");
        check(before.player().equals(after.player()) && before.enemies().equals(after.enemies())
                && before.survival().equals(after.survival()) && Arrays.deepEquals(before.tiles(), after.tiles()),
                "A real file round trip retains champion, equipment, enemy identity/stats/stamina and campaign inventory");
        check(loaded.getEnemies().get(0).getStamina() == 38 && loaded.getCampaign().getLevel() == 2,
                "Enemy endurance and the current expedition are restored into actual objects");
        int x = loaded.getPlayer().getWorldX();
        for (int tick = 0; tick < 10; tick++) loaded.update();
        check(loaded.getPlayer().getWorldX() == x && loaded.getElapsedTicks() == after.survival().elapsedTicks(),
                "A restored campaign does not advance behind the pause screen");
    }

    private static GameSession fixture(KeyHandler keys, CampaignProgress progress, int[][] tiles,
            List<EnemyData> enemies, WeaponType weapon) {
        progress.getInventory().grantWeapon(weapon);
        GameSnapshot snapshot = new GameSnapshot(tiles, new PlayerData(83 * TILE, 84 * TILE, 4, ChampionType.VANGUARD, weapon), enemies, progress.toData());
        return GameSession.fromSnapshot(keys, snapshot);
    }

    private static void testAsyncPanelTransfers() throws Exception {
        CampaignProgress savedProgress = new CampaignProgress(2, Difficulty.HARD);
        savedProgress.getInventory().grantWeapon(WeaponType.PICKAXE);
        savedProgress.gather(Resource.MEAT, 3);
        GameSnapshot saved = new GameSnapshot(new int[200][200],
                new PlayerData(5_000, 5_000, 6, ChampionType.SCOUT, WeaponType.PICKAXE), List.of(), savedProgress.toData());
        AtomicReference<GameSnapshot> latest = new AtomicReference<>();
        AtomicReference<GamePanel> view = new AtomicReference<>();
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger loads = new AtomicInteger();
        AtomicBoolean backgroundOnly = new AtomicBoolean(true);
        CountDownLatch enteredSave = new CountDownLatch(1);
        CountDownLatch releaseSave = new CountDownLatch(1);
        SaveRepository repository = new SaveRepository() {
            @Override public void save(GameSnapshot snapshot) throws SaveGameException {
                if (SwingUtilities.isEventDispatchThread()) backgroundOnly.set(false);
                latest.set(snapshot);
                int attempt = saves.incrementAndGet();
                if (attempt == 1) {
                    enteredSave.countDown();
                    try {
                        if (!releaseSave.await(5, TimeUnit.SECONDS)) throw new SaveGameException("Test save timed out");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new SaveGameException("Test save interrupted", exception);
                    }
                }
                if (attempt == 2) throw new SaveGameException("Test storage is full. Your live campaign is safe.");
            }
            @Override public GameSnapshot load() throws SaveGameException {
                if (SwingUtilities.isEventDispatchThread()) backgroundOnly.set(false);
                if (loads.incrementAndGet() == 1) throw new SaveGameException("Test campaign is damaged. Choose another save.");
                return saved;
            }
        };
        onEdt(() -> {
            GamePanel panel = new GamePanel(repository);
            panel.setSize(GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT);
            view.set(panel);
            panelKey(panel, KeyEvent.VK_ENTER);
            panelKey(panel, KeyEvent.VK_F5);
        });
        check(enteredSave.await(2, TimeUnit.SECONDS), "Actual F5 starts a background campaign save");
        GameSnapshot before = latest.get();
        onEdt(() -> {
            writePanel(view.get(), "survival-saving");
            panelKey(view.get(), KeyEvent.VK_F9);
        });
        check(loads.get() == 0, "A pending save rejects competing load requests");
        releaseSave.countDown();
        settleCallbacks();
        onEdt(() -> panelKey(view.get(), KeyEvent.VK_F9));
        awaitCounter(loads, 1, "Actual F9 invokes the campaign loader");
        settleCallbacks();
        onEdt(() -> {
            writePanel(view.get(), "survival-load-error");
            panelKey(view.get(), KeyEvent.VK_F5);
        });
        awaitCounter(saves, 2, "An unsuccessful load still permits another save");
        check(sameSnapshot(before, latest.get()), "A failed load leaves the full live campaign unchanged");
        settleCallbacks();
        onEdt(() -> {
            writePanel(view.get(), "survival-save-error");
            panelKey(view.get(), KeyEvent.VK_F9);
        });
        awaitCounter(loads, 2, "A failed save leaves the interface usable for a fresh load");
        settleCallbacks();
        onEdt(() -> panelKey(view.get(), KeyEvent.VK_F5));
        awaitCounter(saves, 3, "The restored campaign can be saved again");
        check(sameSnapshot(saved, latest.get()), "Loading restores the saved champion, hard difficulty, level, health and inventory together");
        check(backgroundOnly.get(), "Campaign storage never runs on Swing's gameplay thread");
        settleCallbacks();
        onEdt(() -> view.get().stopGame());
    }

    private static void testMainMenuSaves() throws Exception {
        CampaignProgress progress = new CampaignProgress(2, Difficulty.HARD);
        progress.getInventory().grantWeapon(WeaponType.PICKAXE);
        progress.gather(Resource.MEAT, 3);
        progress.gather(Resource.METAL, 2);
        progress.recordKill(false);
        for (int tick = 0; tick < 137; tick++) progress.tick();
        GameSnapshot existing = new GameSnapshot(new int[200][200],
                new PlayerData(5_000, 5_000, 6, ChampionType.SCOUT, WeaponType.PICKAXE), List.of(), progress.toData());
        AtomicReference<GameSnapshot> latest = new AtomicReference<>();
        AtomicReference<GamePanel> view = new AtomicReference<>();
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger loads = new AtomicInteger();
        AtomicBoolean backgroundOnly = new AtomicBoolean(true);
        SaveRepository repository = new SaveRepository() {
            @Override public void save(GameSnapshot snapshot) {
                if (SwingUtilities.isEventDispatchThread()) backgroundOnly.set(false);
                latest.set(snapshot); saves.incrementAndGet();
            }
            @Override public GameSnapshot load() {
                if (SwingUtilities.isEventDispatchThread()) backgroundOnly.set(false);
                loads.incrementAndGet(); return existing;
            }
        };
        onEdt(() -> {
            GamePanel panel = new GamePanel(repository);
            panel.setSize(GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT);
            view.set(panel);
            panelClick(panel, 150, 490); // Champion menu.
            panelClick(panel, 500, 482); // Nyx.
            panelClick(panel, 420, 490); // Difficulty menu.
            panelClick(panel, 780, 470); // Hard.
            panelClick(panel, 140, 550); // Save before starting.
        });
        awaitCounter(saves, 1, "The main-menu Save button saves a fresh selected campaign");
        GameSnapshot fresh = latest.get();
        check(fresh.player().champion() == ChampionType.SCOUT && fresh.survival().difficulty() == Difficulty.HARD,
                "A fresh menu save uses the selected champion and difficulty");
        check(fresh.player().health() == 4 && fresh.survival().elapsedTicks() == 0,
                "A fresh menu save begins beside the ship with two hearts and no simulated time");
        settleCallbacks();
        onEdt(() -> {
            writePanel(view.get(), "survival-menu-save-paused");
            for (int tick = 0; tick < 10; tick++) view.get().update();
            panelKey(view.get(), KeyEvent.VK_F5);
        });
        awaitCounter(saves, 2, "A menu-saved campaign remains available to save while paused");
        check(sameSnapshot(fresh, latest.get()), "A menu save pauses the captured world without replacing its map or advancing time");
        settleCallbacks();
        onEdt(() -> {
            panelKey(view.get(), KeyEvent.VK_ENTER);
            panelKey(view.get(), KeyEvent.VK_F5);
        });
        awaitCounter(saves, 3, "Enter resumes a fresh menu-saved campaign");
        GameSnapshot resumed = latest.get();
        check(resumed.survival().elapsedTicks() == fresh.survival().elapsedTicks() + 1
                && resumed.player().equals(fresh.player()) && Arrays.deepEquals(resumed.tiles(), fresh.tiles()),
                "Resuming a fresh menu save retains its selected character and original world");
        check(resumed.survival().difficulty() == Difficulty.HARD, "Resuming retains the menu's chosen difficulty");
        settleCallbacks();
        onEdt(() -> panelKey(view.get(), KeyEvent.VK_F9));
        awaitCounter(loads, 1, "The progressed campaign fixture is loaded through actual load input");
        settleCallbacks();
        onEdt(() -> {
            panelClick(view.get(), 420, 456); // Main menu from the restored pause screen.
            panelClick(view.get(), 140, 550); // Save the retained expedition.
        });
        awaitCounter(saves, 4, "Returning to the main menu permits saving existing campaign progress");
        check(sameSnapshot(existing, latest.get()), "Saving from the main menu preserves the complete progressed campaign");
        settleCallbacks();
        onEdt(() -> {
            for (int tick = 0; tick < 10; tick++) view.get().update();
            panelKey(view.get(), KeyEvent.VK_ENTER);
            panelKey(view.get(), KeyEvent.VK_F5);
        });
        awaitCounter(saves, 5, "Enter resumes the retained campaign after its main-menu save");
        resumed = latest.get();
        check(resumed.player().equals(existing.player()) && Arrays.deepEquals(resumed.tiles(), existing.tiles()),
                "Main-menu save and resume keep position, health, equipment, champion and terrain");
        check(resumed.survival().level() == 2 && resumed.survival().difficulty() == Difficulty.HARD
                && resumed.survival().inventory().equals(existing.survival().inventory())
                && resumed.survival().gathered().equals(existing.survival().gathered())
                && resumed.survival().enemyKills() == existing.survival().enemyKills(),
                "Main-menu resume keeps the later level, difficulty, inventory and achievements");
        check(resumed.survival().elapsedTicks() == existing.survival().elapsedTicks() + 1,
                "The retained mission stays paused until Enter and then advances exactly one tick");
        check(backgroundOnly.get(), "Main-menu saves and loads also keep storage off the gameplay thread");
        settleCallbacks();
        onEdt(() -> view.get().stopGame());
    }

    @FunctionalInterface private interface EdtAction { void run() throws Exception; }
    private static void onEdt(EdtAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception exception) { throw new IllegalStateException("Campaign panel check failed", exception); }
        });
    }
    private static void settleCallbacks() throws Exception {
        CountDownLatch settled = new CountDownLatch(1);
        onEdt(() -> { Timer timer = new Timer(100, event -> settled.countDown()); timer.setRepeats(false); timer.start(); });
        check(settled.await(2, TimeUnit.SECONDS), "Swing stays responsive while storage callbacks settle");
    }
    private static void awaitCounter(AtomicInteger counter, int target, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (counter.get() < target && System.nanoTime() < deadline) settleCallbacks();
        check(counter.get() >= target, message);
    }
    private static boolean sameSnapshot(GameSnapshot first, GameSnapshot second) {
        return first.player().equals(second.player()) && first.enemies().equals(second.enemies())
                && first.survival().equals(second.survival()) && Arrays.deepEquals(first.tiles(), second.tiles());
    }
    private static void panelKey(GamePanel panel, int code) {
        for (var listener : panel.getKeyListeners()) listener.keyPressed(event(KeyEvent.KEY_PRESSED, code));
        panel.update();
        for (var listener : panel.getKeyListeners()) listener.keyReleased(event(KeyEvent.KEY_RELEASED, code));
    }
    private static void panelClick(GamePanel panel, int x, int y) {
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false);
        for (var listener : panel.getMouseListeners()) listener.mousePressed(event);
    }
    private static void writePanel(GamePanel panel, String name) throws Exception {
        BufferedImage image = new BufferedImage(GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.paint(graphics); } finally { graphics.dispose(); }
        Path folder = Path.of("build", "previews");
        Files.createDirectories(folder);
        check(ImageIO.write(image, "png", folder.resolve(name + ".png").toFile()), "Campaign storage screen renders: " + name);
    }

    private static void press(KeyHandler keys, int code) { keys.keyPressed(event(KeyEvent.KEY_PRESSED, code)); }
    private static void release(KeyHandler keys, int code) { keys.keyReleased(event(KeyEvent.KEY_RELEASED, code)); }
    private static KeyEvent event(int id, int code) { return new KeyEvent(SOURCE, id, 0, 0, code, KeyEvent.CHAR_UNDEFINED); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
