package com.game.tests;

import com.game.core.EncounterFactory;
import com.game.core.GameConfig;
import com.game.core.KeyHandler;
import com.game.entities.Alien;
import com.game.entities.Champion;
import com.game.entities.Direction;
import com.game.entities.Enemy;
import com.game.entities.EnemyType;
import com.game.entities.Orc;
import com.game.entities.Projectile;
import com.game.survival.ChampionType;
import com.game.survival.Difficulty;
import com.game.survival.WeaponType;
import com.game.world.Chunk;
import com.game.world.TileType;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Real input and collision checks for survival actors, equipment and enemy endurance. */
public final class SurvivalCombatTest {
    private static final int TILE = GameConfig.TILE_SIZE;
    private static final JPanel SOURCE = new JPanel();
    private static int checks;

    public static void main(String[] arguments) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            testChampionsAndTwoHeartStart();
            testWeaponsAndUnarmedKill();
            testDashAndTerrainOwnership();
            testStaminaHasConsequences();
            testDifficultyAndLevelThree();
            testProjectiles();
            System.out.println("SurvivalCombatTest: " + checks + " checks passed.");
        });
    }

    private static void testChampionsAndTwoHeartStart() {
        int[] movement = {4, 5, 4};
        int[] energy = {100, 120, 110};
        String[] names = {"Astra", "Nyx", "Orion"};
        Chunk world = emptyWorld();
        int index = 0;
        for (ChampionType type : ChampionType.values()) {
            KeyHandler keys = new KeyHandler();
            Champion player = new Champion(keys, TILE, type);
            check(player.getSpeed() == type.speed(), "The selected champion supplies its movement stat");
            check(player.getChampionType() == type && player.getDisplayName().equals(names[index]), "Champion identity is stable");
            check(player.getCurrentHealth() == 4 && player.getMaxHealth() == 12, "Each champion begins with exactly two of six hearts");
            check(player.getWeaponType() == WeaponType.FISTS && player.getAttackDamage() == 1, "New champions must earn their weapons");
            check(player.getEnergy() == energy[index] && player.getMaxEnergy() == energy[index], "Champion dash reserves differ in actual play");
            int x = player.getWorldX();
            press(keys, KeyEvent.VK_D);
            player.update(world, TILE);
            check(player.getWorldX() == x + movement[index], "Champion movement stat changes distance travelled");
            check(!player.getSignature().isBlank(), "Champion class description is available to selection screens");
            index++;
        }
    }

    private static void testWeaponsAndUnarmedKill() {
        KeyHandler keys = new KeyHandler();
        Champion player = new Champion(keys, TILE, ChampionType.VANGUARD);
        Chunk world = emptyWorld();
        Alien target = new Alien(TILE, player);
        target.configure("first-kill", 2, 2, 45, false);
        target.setPosition(4_000, 4_048);
        player.restoreState(4_000, 4_000, 4, WeaponType.FISTS);
        press(keys, KeyEvent.VK_E);
        for (int tick = 0; tick < 6; tick++) player.update(world, TILE);
        check(player.isAttacking() && !player.getAttackBounds().isEmpty(), "Fists attack immediately without any crafted equipment");
        player.strike(target);
        check(target.getCurrentHealth() == 1, "An unarmed strike deals one health point");
        player.strike(target);
        check(!target.isAlive(), "The damage contract supports the first unarmed monster kill");
        release(keys, KeyEvent.VK_E);
        int[] damages = {1, 2, 3, 4};
        int[] reach = {30, 36, 42, 0};
        int index = 0;
        for (WeaponType weapon : WeaponType.values()) {
            player.restoreState(4_000, 4_000, 4, weapon);
            keys.clear();
            press(keys, KeyEvent.VK_SPACE);
            for (int tick = 0; tick < 6; tick++) player.update(world, TILE);
            check(player.getAttackDamage() == damages[index], "Damage scales with earned equipment: " + weapon);
            Rectangle bounds = player.getAttackBounds();
            check(weapon == WeaponType.GUN ? bounds.isEmpty() : bounds.height == reach[index], "Weapon reach matches its combat role: " + weapon);
            if (weapon != WeaponType.GUN) {
                bounds.width = 1;
                check(player.getAttackBounds().width == 32, "Melee hitboxes remain defensive copies");
            }
            index++;
        }
        check(player.getAttackSequence() == 1, "A gun still emits attack sequences for session-owned projectiles");
    }

    private static void testDashAndTerrainOwnership() {
        int[][] tiles = new int[200][200];
        tiles[100][102] = TileType.TREE.getId();
        tiles[100][100] = TileType.DRAGON_FRUIT.getId();
        Chunk world = new Chunk(tiles);
        for (ChampionType type : ChampionType.values()) {
            KeyHandler keys = new KeyHandler();
            Champion player = new Champion(keys, TILE, type);
            player.update(world, TILE);
            check(player.getCurrentHealth() == 4 && world.getTileAt(100, 100) == TileType.DRAGON_FRUIT,
                    "Champion does not consume inventory/world items behind the session's back");
            press(keys, KeyEvent.VK_D);
            player.update(world, TILE);
            release(keys, KeyEvent.VK_D);
            int energy = player.getMaxEnergy();
            press(keys, KeyEvent.VK_SHIFT);
            for (int tick = 0; tick < 8; tick++) player.update(world, TILE);
            check(player.getEnergy() == energy - 35 && player.isDashing(), "Dash costs 35 energy for each champion");
            player.takeDamage(4);
            check(player.getCurrentHealth() == 4, "Active dash protects the two-heart explorer");
            check(player.getWorldX() + player.getBounds().width <= 102 * TILE && world.canOccupy(player.getBounds(), TILE),
                    "Fast champion dash cannot cross a solid tree");
            int stopped = player.getWorldX();
            for (int tick = 0; tick < 80; tick++) player.update(world, TILE);
            check(!player.isDashing() && player.getWorldX() == stopped, "Holding Shift cannot repeat a dash");
            player.restoreState(4_000, 4_000, 4, WeaponType.FISTS);
            check(player.getEnergy() == player.getMaxEnergy() && player.getDashCooldown() == 0,
                    "Life restoration clears transient dash state");
        }
    }

    private static void testStaminaHasConsequences() {
        Champion player = new Champion(new KeyHandler(), TILE, ChampionType.VANGUARD);
        player.setPosition(4_000, 4_000);
        Alien monster = new Alien(TILE, player);
        monster.configure("tired-monster", 6, 2, 3, false);
        monster.setPosition(4_300, 4_000);
        Chunk world = emptyWorld();
        for (int tick = 0; tick < 9; tick++) monster.update(world, TILE);
        check(monster.getStamina() == 0 && monster.isResting(), "Pursuit actually exhausts enemy stamina");
        int restingX = monster.getWorldX();
        for (int tick = 0; tick < 47; tick++) monster.update(world, TILE);
        check(monster.getWorldX() == restingX && monster.isResting(), "Exhaustion creates a real escape window");
        monster.update(world, TILE);
        check(!monster.isResting() && monster.getStamina() == monster.getMaxStamina(), "Rest replenishes the enemy's endurance");
        monster.update(world, TILE);
        check(monster.getWorldX() < restingX, "A recovered monster resumes pursuit");
        monster.restoreState(1_000, 1_000, 5, 1);
        for (int tick = 0; tick < 4; tick++) monster.update(world, TILE);
        check(monster.getStamina() == 3, "Idle distant enemies regenerate stamina");
        Orc melee = new Orc(TILE, player);
        melee.configure("exhausted-melee", 6, 1, 1, false);
        melee.setPosition(4_000, 4_048);
        for (int tick = 0; tick < 3; tick++) melee.update(world, TILE);
        check(melee.isResting() && !melee.isAttacking() && player.getCurrentHealth() == 4,
                "Stamina exhaustion interrupts melee before it can land a strike");
        monster.restoreState(4_300, 4_000, 4, 0);
        check(monster.isResting() && monster.getStamina() == 0, "Loading zero stamina preserves its gameplay consequence");
    }

    private static void testDifficultyAndLevelThree() {
        Champion player = new Champion(new KeyHandler(), TILE, ChampionType.VANGUARD);
        for (Difficulty difficulty : Difficulty.values()) for (EnemyType type : EnemyType.values()) {
            Enemy second = EncounterFactory.create(type, "second", 2, difficulty, player);
            Enemy third = EncounterFactory.create(type, "third", 3, difficulty, player);
            check(third.getMaxHealth() == 2 * second.getMaxHealth(), "Level-three health exactly doubles at every difficulty");
            check(third.getMaxStamina() == 2 * second.getMaxStamina(), "Level-three stamina exactly doubles at every difficulty");
            check(third.isMonster() == (type == EnemyType.ALIEN || type == EnemyType.BAT), "Monster classification drives kill achievements");
            check(third.isFastMonster() == third.isMonster(), "Only level-three monster encounters carry the fast flag");
            check(third.getId().equals("third"), "Encounter IDs distinguish repeated types");
        }
        Enemy easy = EncounterFactory.create(EnemyType.ORC, "easy", 2, Difficulty.EASY, player);
        Enemy medium = EncounterFactory.create(EnemyType.ORC, "medium", 2, Difficulty.MEDIUM, player);
        Enemy hard = EncounterFactory.create(EnemyType.ORC, "hard", 2, Difficulty.HARD, player);
        check(easy.getMaxHealth() < medium.getMaxHealth() && medium.getMaxHealth() < hard.getMaxHealth(), "Difficulty increases enemy health");
        check(easy.getSpeed() < medium.getSpeed() && medium.getSpeed() < hard.getSpeed(), "Difficulty increases actual pursuit speed");
        check(easy.getMaxStamina() < medium.getMaxStamina() && medium.getMaxStamina() < hard.getMaxStamina(), "Difficulty increases endurance");
    }

    private static void testProjectiles() {
        Chunk world = emptyWorld();
        for (Direction facing : Direction.values()) {
            Projectile pulse = new Projectile(4_000, 4_000, facing, 4);
            pulse.update(world, TILE);
            check(Math.abs(pulse.getWorldX() - 4_000) + Math.abs(pulse.getWorldY() - 4_000) == 10,
                    "Gun pulses travel ten pixels in their chosen direction");
            check(pulse.getDamage() == 4 && pulse.isActive(), "Gun pulse retains its damage and active state");
            pulse.deactivate();
            int x = pulse.getWorldX();
            pulse.update(world, TILE);
            check(!pulse.isActive() && pulse.getWorldX() == x, "An impacted pulse cannot move or hit again");
        }
        int[][] tiles = new int[200][200];
        tiles[83][84] = TileType.TREE.getId();
        Projectile blocked = new Projectile(4_000, 4_000, Direction.RIGHT, 4);
        Chunk wall = new Chunk(tiles);
        for (int tick = 0; tick < 4; tick++) blocked.update(wall, TILE);
        check(!blocked.isActive() && blocked.getWorldX() + blocked.getBounds().width <= 84 * TILE,
                "Projectile substeps stop at a solid obstacle");
        Projectile lifetime = new Projectile(4_000, 4_000, Direction.RIGHT, 4);
        for (int tick = 0; tick < 59; tick++) lifetime.update(world, TILE);
        check(lifetime.isActive(), "Pulse remains active through its penultimate lifetime tick");
        lifetime.update(world, TILE);
        check(!lifetime.isActive() && lifetime.getWorldX() == 4_600, "Pulse expires after sixty movement ticks");
    }

    private static Chunk emptyWorld() { return new Chunk(new int[200][200]); }
    private static void press(KeyHandler keys, int code) { keys.keyPressed(event(KeyEvent.KEY_PRESSED, code)); }
    private static void release(KeyHandler keys, int code) { keys.keyReleased(event(KeyEvent.KEY_RELEASED, code)); }
    private static KeyEvent event(int id, int code) { return new KeyEvent(SOURCE, id, 0, 0, code, KeyEvent.CHAR_UNDEFINED); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
