package com.game.core;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.game.core.GameEvent.Kind;
import com.game.entities.*;
import com.game.persistence.GameSnapshot;
import com.game.persistence.GameSnapshot.EnemyData;
import com.game.persistence.GameSnapshot.PlayerData;
import com.game.survival.*;
import com.game.world.*;

/** Owns the three-level survival campaign; screens and files do not decide game rules. */
public final class GameSession {
    public static final int TILE_SIZE = GameConfig.TILE_SIZE;
    public static final int SHIP_X = 99 * TILE_SIZE;
    public static final int SHIP_Y = 98 * TILE_SIZE;
    public static final String CONGRATULATIONS = "Congratulations, you will return back to the planet and help your people. You are so brave.";
    private final KeyHandler input;
    private Chunk chunk;
    private final Champion player;
    private final CampaignProgress campaign;
    private final ShipDock ship;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<GameEvent> events = new ArrayList<>();
    private final Set<Enemy> hitThisSwing = new HashSet<>();
    private int attackSequence;
    private GameState state = GameState.INTRO;

    public GameSession(KeyHandler input) {
        this(input, ChampionType.VANGUARD, Difficulty.EASY);
    }
    public GameSession(KeyHandler input, long seed) {
        this(input, ChampionType.VANGUARD, Difficulty.EASY, seed);
    }
    public GameSession(KeyHandler input, ChampionType champion, Difficulty difficulty) {
        this(input, champion, difficulty, new java.util.Random().nextLong());
    }
    public GameSession(KeyHandler input, ChampionType champion, Difficulty difficulty, long seed) {
        this(input, champion, new CampaignProgress(difficulty), new Chunk(seed));
        populateEncounters();
    }
    private GameSession(KeyHandler input, ChampionType champion, CampaignProgress campaign, Chunk chunk) {
        this.input = input;
        this.campaign = campaign;
        this.chunk = chunk;
        player = new Champion(input, TILE_SIZE, champion);
        ship = new ShipDock(SHIP_X, SHIP_Y, campaign);
    }

    private void populateEncounters() {
        enemies.clear();
        projectiles.clear();
        hitThisSwing.clear();
        int[][] positions = {
            {108,100}, {94,105}, {106,108}, {90,96}, {113,93},
            {87,106}, {111,112}, {86,91}, {118,101}, {99,116},
            {82,101}, {115,85}, {84,115}, {119,113}, {91,82},
            {104,83}, {80,112}, {117,119}, {82,85}, {106,122}
        };
        LevelDefinition rule = campaign.getDefinition();
        int extra = campaign.getDifficulty().extraSpawns();
        int enemyCount = rule.enemyKillTarget() + extra / 2;
        int count = enemyCount + rule.monsterKillTarget() + (extra - extra / 2);
        for (int i = 0; i < count; i++) {
            EnemyType type = i < enemyCount ? i % 2 == 0 ? EnemyType.ORC : EnemyType.SKELETON
            : i % 2 == 0 ? EnemyType.ALIEN : EnemyType.BAT;
            Enemy enemy = EncounterFactory.create(type, "L" + campaign.getLevel() + "-" + i, campaign.getLevel(), campaign.getDifficulty(), player);
            int[] position = positions[i % positions.length];
            chunk.clearSpawn(position[0], position[1]);
            enemy.setPosition(position[0] * TILE_SIZE, position[1] * TILE_SIZE);
            enemies.add(enemy);
        }
    }

    public static GameSession fromSnapshot(KeyHandler input, GameSnapshot snapshot) {
        GameSession loaded = new GameSession(input, snapshot.player().champion(), CampaignProgress.fromData(snapshot.survival()), new Chunk(snapshot.tiles()));
        PlayerData playerData = snapshot.player();
        loaded.player.restoreState(playerData.x(), playerData.y(), playerData.health(), playerData.weapon());
        for (EnemyData data : snapshot.enemies()) {
            Enemy enemy = EncounterFactory.create(data.type(), data.id(), snapshot.survival().level(), snapshot.survival().difficulty(), loaded.player);
            enemy.configure(data.id(), data.maxHealth(), data.speed(), data.maxStamina(), data.fastMonster());
            enemy.restoreState(data.x(), data.y(), data.health(), data.stamina());
            loaded.enemies.add(enemy);
        }
        loaded.state = GameState.PAUSED;
        return loaded;
    }

    public Chunk getChunk() {
        return chunk;
    }
    public Champion getPlayer() {
        return player;
    }
    public CampaignProgress getCampaign() {
        return campaign;
    }
    public ShipDock getShip() {
        return ship;
    }
    public List<Enemy> getEnemies() {
        return List.copyOf(enemies);
    }
    public List<Projectile> getProjectiles() {
        return List.copyOf(projectiles);
    }
    public GameState getState() {
        return state;
    }
    public int getElapsedTicks() {
        return campaign.getElapsedTicks();
    }
    public int getScore() {
        return campaign.getScore();
    }
    public String getMissionTime() {
        return String.format("%02d:%02d", getElapsedTicks() / 3600, getElapsedTicks() / 60 % 60);
    }
    public boolean isNearShip() {
        return ship.isInRange(player);
    }
    public void start() {
        if (state == GameState.INTRO || state == GameState.PAUSED) state = GameState.PLAYING;
    }
    public void pause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
    }
    public void togglePause() {
        if (state == GameState.PLAYING) pause();
        else if (state == GameState.PAUSED) start();
    }
    public List<GameEvent> drainEvents() {
        List<GameEvent> result = List.copyOf(events);
        events.clear();
        return result;
    }
    private void emit(Kind kind, int x, int y, int amount, String text) {
        if (events.size() < 64) events.add(new GameEvent(kind, x + 16, y + 16, amount, text));
    }
    private void hint(String text) {
        emit(Kind.PICKUP, player.getWorldX(), player.getWorldY(), 0, text);
    }

    public boolean equip(WeaponType weapon) {
        if (!campaign.getInventory().hasWeapon(weapon) || !player.isAlive()) return false;
        player.setWeapon(weapon);
        return true;
    }
    public boolean craft() {
        if (state != GameState.PLAYING && state != GameState.PAUSED) return false;
        if (!campaign.craft()) {
            hint("Complete the kill and gathering checklist, and carry the recipe materials.");
            return false;
        }
        player.setWeapon(campaign.getDefinition().weapon());
        emit(Kind.CRAFTED, player.getWorldX(), player.getWorldY(), 200, player.getWeaponType().displayName() + " crafted. Mine diamonds and bring them home alive.");
        return true;
    }
    public boolean eat() {
        if (!player.isAlive() || player.getCurrentHealth() >= player.getMaxHealth() || !campaign.eat()) return false;
        player.heal(4);
        emit(Kind.PICKUP, player.getWorldX(), player.getWorldY(), 0, "Meat eaten: two hearts restored.");
        return true;
    }
    public void retry() {
        if (state != GameState.LOST) return;
        player.restoreState(100 * TILE_SIZE, 100 * TILE_SIZE, 4, player.getWeaponType());
        input.clear();
        projectiles.clear();
        state = GameState.PLAYING;
        hint("Two hearts. Your banked diamonds and achievements are safe; carried cargo was lost.");
    }
    public void stayOnLevel() {
        if (state == GameState.LEVEL_COMPLETE || state == GameState.WON) {
            campaign.setChoiceShown(true);
            state = GameState.PLAYING;
        }
    }
    public void returnToMenu() {
        if (player.isAlive()) {
            state = GameState.INTRO;
            input.clear();
        }
    }
    public boolean advanceLevel() {
        if (state != GameState.LEVEL_COMPLETE || !campaign.isReady()) return false;
        if (campaign.getLevel() == 3) {
            state = GameState.WON;
            emit(Kind.VICTORY, SHIP_X, SHIP_Y, getScore(), CONGRATULATIONS);
            return true;
        }
        if (!campaign.advance()) return false;
        chunk = new Chunk();
        populateEncounters();
        player.restoreState(100 * TILE_SIZE, 100 * TILE_SIZE, player.getCurrentHealth(), player.getWeaponType());
        input.clear();
        attackSequence = 0;
        state = GameState.PLAYING;
        hint("Level " + campaign.getLevel() + ": check J for achievements. You can keep exploring before returning home.");
        return true;
    }
    public boolean requestLevelChoice() {
        if (player.isAlive() && campaign.isReady() && isNearShip()) {
            state = GameState.LEVEL_COMPLETE;
            return true;
        }
        return false;
    }

    public boolean interact() {
        if (state != GameState.PLAYING) return false;
        if (isNearShip()) {
            int before = campaign.getBankedDiamonds();
            Interactable dock = ship;
            boolean deposited = dock.interact(player);
            if (deposited) hint("Cargo secured: " + (campaign.getBankedDiamonds() - before) + " diamonds banked at your ship.");
            if (campaign.isReady()) requestLevelChoice();
            else if (!deposited) hint(campaign.isCrafted() ? "Carry diamonds back to the ship and press F to bank them." : "Complete your kill, gathering and crafting achievements before banking diamonds.");
            return deposited;
        }
        int col = (player.getWorldX() + 16) / TILE_SIZE;
        int row = (player.getWorldY() + 16) / TILE_SIZE;
        int bestCol = -1, bestRow = -1;
        double distance = 85;
        for (int r = row - 2; r <= row + 2; r++) for (int c = col - 2; c <= col + 2; c++) {
            if (!chunk.contains(c, r) || !chunk.getTileAt(c, r).isHarvestable()) continue;
            double candidate = Math.hypot(c * TILE_SIZE - player.getWorldX(), r * TILE_SIZE - player.getWorldY());
            if (candidate < distance) {
                distance = candidate;
                bestCol = c;
                bestRow = r;
            }
        }
        if (bestCol < 0) {
            hint("Search behind trees and rocks. Stand nearby and press F to gather.");
            return false;
        }
        TileType loot = chunk.harvestAt(bestCol, bestRow, campaign.getInventory().hasWeapon(WeaponType.PICKAXE));
        if (loot == TileType.GRASS) {
            hint("A crafted pickaxe is needed to mine rocks and diamonds. Equip it with 2.");
            return false;
        }
        collect(loot);
        return true;
    }

    private void collect(TileType tile) {
        Resource resource = switch (tile) {
            case MEAT -> Resource.MEAT;
            case METAL -> Resource.METAL;
            case STONE -> Resource.STONE;
            case ROCK -> Resource.ROCK;
            case DIAMOND -> Resource.DIAMOND;
            default -> null;
        };
        if (resource != null) {
            campaign.gather(resource, 1);
            emit(Kind.PICKUP, player.getWorldX(), player.getWorldY(), 10,
            resource == Resource.DIAMOND ? "Diamond in inventory. It counts only after you reach the ship alive and deposit it." : resource.displayName() + " gathered.");
            return;
        }
        switch (tile) {
            case STRAWBERRY -> player.heal(2);
            case BANANA -> player.heal(3);
            case DRAGON_FRUIT -> player.heal(4);
            case BLUE_HEART -> player.heal(12);
            case MUSHROOM -> player.takeDamage(2);
            default -> {
                return;
            }
        }
        emit(Kind.PICKUP, player.getWorldX(), player.getWorldY(), 0, "");
    }

    private void defeat(Enemy enemy) {
        campaign.recordKill(enemy.isMonster());
        chunk.dropAt(enemy.getWorldX() / TILE_SIZE, enemy.getWorldY() / TILE_SIZE, TileType.MEAT);
        emit(Kind.ENEMY_DEFEATED, enemy.getWorldX(), enemy.getWorldY(), 100, enemy.getDisplayName() + " defeated. Meat dropped.");
    }
    public void update() {
        if (state != GameState.PLAYING) return;
        campaign.tick();
        int healthBefore = player.getCurrentHealth();
        boolean dashBefore = player.isDashing();
        player.update(chunk, TILE_SIZE);
        collect(chunk.collectAt((player.getWorldX() + 16) / TILE_SIZE, (player.getWorldY() + 16) / TILE_SIZE));
        if (!dashBefore && player.isDashing()) emit(Kind.DASH, player.getWorldX(), player.getWorldY(), 0, "");
        updatePlayerAttack();
        updateEnemies();
        updateProjectiles();
        if (player.getCurrentHealth() < healthBefore) emit(Kind.HURT, player.getWorldX(), player.getWorldY(), healthBefore - player.getCurrentHealth(), "");
        if (!player.isAlive()) {
            campaign.onDeath();
            projectiles.clear();
            state = GameState.LOST;
            emit(Kind.DEFEAT, player.getWorldX(), player.getWorldY(), 0, "Carried diamonds lost. Banked diamonds remain secure.");
            return;
        }
        if (campaign.isReady() && isNearShip() && !campaign.isChoiceShown()) state = GameState.LEVEL_COMPLETE;
        respawnRoamingMonster();
    }

    /** One attack sequence can hit each enemy once, even across several frames. */
    private void updatePlayerAttack() {
        if (attackSequence != player.getAttackSequence()) {
            attackSequence = player.getAttackSequence();
            hitThisSwing.clear();
            if (player.getWeaponType() == WeaponType.GUN && projectiles.size() < 24) {
                projectiles.add(new Projectile(player.getWorldX() + 16, player.getWorldY() + 16, player.getDirection(), player.getAttackDamage()));
            }
        }
    }

    /** The same loop updates every enemy through its inherited Enemy type. */
    private void updateEnemies() {
        Rectangle attack = player.getAttackBounds();
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || !player.isAlive()) continue;
            if (attack.intersects(enemy.getBounds()) && hitThisSwing.add(enemy)) {
                int before = enemy.getCurrentHealth();
                Damageable target = enemy;
                player.strike(target);
                emit(Kind.STRIKE, enemy.getWorldX(), enemy.getWorldY(), before - enemy.getCurrentHealth(), "");
                if (!enemy.isAlive()) defeat(enemy);
            }
            enemy.update(chunk, TILE_SIZE);
        }
    }

    private void updateProjectiles() {
        for (Projectile projectile : projectiles) {
            projectile.update(chunk, TILE_SIZE);
            if (!projectile.isActive()) continue;
            for (Enemy enemy : enemies) if (enemy.isAlive() && projectile.getBounds().intersects(enemy.getBounds())) {
                enemy.takeDamage(projectile.getDamage());
                projectile.deactivate();
                emit(Kind.STRIKE, enemy.getWorldX(), enemy.getWorldY(), projectile.getDamage(), "");
                if (!enemy.isAlive()) defeat(enemy);
                break;
            }
        }
        projectiles.removeIf(projectile -> !projectile.isActive());
    }

    private void respawnRoamingMonster() {
        // Staying is a real open-world choice: fresh roaming monsters return over time.
        if (campaign.getElapsedTicks() % 1800 == 0 && enemies.stream().filter(Enemy::isAlive).count() < 3) {
            for (int i = 0; i < enemies.size(); i++) if (!enemies.get(i).isAlive()) {
                int col = Math.max(3, Math.min(196, player.getWorldX() / TILE_SIZE + 9));
                int row = Math.max(3, Math.min(196, player.getWorldY() / TILE_SIZE + 6));
                chunk.clearSpawn(col, row);
                Enemy roaming = EncounterFactory.create(EnemyType.ALIEN, enemies.get(i).getId(), campaign.getLevel(), campaign.getDifficulty(), player);
                roaming.setPosition(col * TILE_SIZE, row * TILE_SIZE);
                enemies.set(i, roaming);
                break;
            }
        }
    }

    public GameSnapshot snapshot() {
        if (!player.isAlive()) throw new IllegalStateException("A defeated life cannot be saved. Retry or load a saved campaign.");
        PlayerData data = new PlayerData(player.getWorldX(), player.getWorldY(), player.getCurrentHealth(), player.getChampionType(), player.getWeaponType());
        List<EnemyData> saved = enemies.stream().map(enemy -> new EnemyData(enemy.getId(), enemy.getType(), enemy.getWorldX(), enemy.getWorldY(), enemy.getCurrentHealth(), enemy.getMaxHealth(), enemy.getSpeed(), enemy.getMaxStamina(), enemy.getStamina(), enemy.isFastMonster())).toList();
        return new GameSnapshot(chunk.copyTiles(), data, saved, campaign.toData());
    }
    public record Objective(String label, int x, int y) {
    }
    public Objective getObjective() {
        LevelDefinition rule = campaign.getDefinition();
        if (campaign.getLevel() == 1 && campaign.getGathered(Resource.MEAT) == 0) {
            return resourceObjective(Resource.MEAT, "Find meat. Q restores two hearts.");
        }
        boolean needEnemy = campaign.getEnemyKills() < rule.enemyKillTarget();
        boolean needMonster = campaign.getMonsterKills() < rule.monsterKillTarget();
        if (needEnemy || needMonster) {
            Enemy nearest = null;
            double best = Double.MAX_VALUE;
            for (Enemy enemy : enemies) if (enemy.isAlive() && (enemy.isMonster() ? needMonster : needEnemy)) {
                double distance = Math.hypot(enemy.getWorldX() - player.getWorldX(), enemy.getWorldY() - player.getWorldY());
                if (distance < best) {
                    best = distance;
                    nearest = enemy;
                }
            }
            if (nearest != null) return new Objective("Defeat " + nearest.getDisplayName(), nearest.getWorldX(), nearest.getWorldY());
        }
        for (Resource resource : Resource.values()) {
            int required = rule.requiredGathered().getOrDefault(resource, 0);
            if (campaign.getGathered(resource) < required || !campaign.isCrafted() && campaign.getInventory().count(resource) < rule.recipe().getOrDefault(resource, 0)) {
                return resourceObjective(resource, "Gather " + resource.displayName());
            }
        }
        if (!campaign.isCrafted()) return new Objective("C  Craft your " + rule.weapon().displayName(), player.getWorldX(), player.getWorldY());
        if (campaign.isReady()) return new Objective(campaign.getLevel() == 3
                ? "Stay and explore, or F at ship to return home"
                : "Stay and explore, or F at ship for next level", SHIP_X, SHIP_Y);
        if (campaign.getInventory().count(Resource.DIAMOND) + campaign.getBankedDiamonds() >= rule.diamonds()) return new Objective("Bring diamond cargo back to your ship", SHIP_X, SHIP_Y);
        return resourceObjective(Resource.DIAMOND, "Mine diamonds, then return alive");
    }
    private Objective resourceObjective(Resource resource, String label) {
        TileType[] types = switch (resource) {
            case MEAT -> new TileType[] {TileType.MEAT, TileType.TREE_FOOD};
            case METAL -> new TileType[] {TileType.METAL, TileType.TREE_METAL};
            case STONE -> new TileType[] {TileType.STONE, TileType.TREE_STONE};
            case ROCK -> new TileType[] {TileType.ROCK, TileType.BOULDER};
            case DIAMOND -> new TileType[] {TileType.DIAMOND, TileType.ROCK_DIAMOND};
        };
        int[] nearest = chunk.findNearest(types, player.getWorldX(), player.getWorldY());
        return nearest == null ? new Objective(label, SHIP_X, SHIP_Y) : new Objective(label, nearest[0] * 48, nearest[1] * 48);
    }
}
