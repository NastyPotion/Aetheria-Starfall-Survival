package com.game.survival;

import com.game.persistence.GameSnapshot.SurvivalData;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Owns three-level campaign achievements independently of a player's current life. */
public final class CampaignProgress {
    private int level;
    private final Difficulty difficulty;
    private final Inventory inventory;
    private final EnumMap<Resource, Integer> gathered = new EnumMap<>(Resource.class);
    private int enemyKills;
    private int monsterKills;
    private boolean crafted;
    private int bankedDiamonds;
    private int totalBankedDiamonds;
    private int elapsedTicks;
    private int score;
    private boolean choiceShown;

    public CampaignProgress(Difficulty difficulty) { this(1, difficulty); }

    public CampaignProgress(int level, Difficulty difficulty) {
        LevelDefinition.of(level);
        this.level = level;
        this.difficulty = Objects.requireNonNull(difficulty, "Difficulty is required");
        inventory = new Inventory();
        for (Resource resource : Resource.values()) gathered.put(resource, 0);
    }

    private CampaignProgress(SurvivalData data) {
        level = data.level();
        difficulty = data.difficulty();
        inventory = Inventory.restore(data.inventory(), data.weapons());
        for (Resource resource : Resource.values()) gathered.put(resource, data.gathered().getOrDefault(resource, 0));
        enemyKills = data.enemyKills();
        monsterKills = data.monsterKills();
        crafted = data.crafted();
        bankedDiamonds = data.bankedDiamonds();
        totalBankedDiamonds = data.totalBankedDiamonds();
        elapsedTicks = data.elapsedTicks();
        score = data.score();
        choiceShown = data.choiceShown();
    }

    public static CampaignProgress fromData(SurvivalData data) { return new CampaignProgress(Objects.requireNonNull(data)); }

    public SurvivalData toData() {
        return new SurvivalData(level, difficulty, inventory.copyItems(), inventory.getWeapons(),
                Map.copyOf(gathered), enemyKills, monsterKills, crafted, bankedDiamonds,
                totalBankedDiamonds, elapsedTicks, score, choiceShown);
    }

    public int getLevel() { return level; }
    public Difficulty getDifficulty() { return difficulty; }
    public LevelDefinition getDefinition() { return LevelDefinition.of(level); }
    public Inventory getInventory() { return inventory; }
    public int getEnemyKills() { return enemyKills; }
    public int getMonsterKills() { return monsterKills; }
    public int getGathered(Resource resource) { return gathered.get(Objects.requireNonNull(resource)); }
    public boolean isCrafted() { return crafted; }
    public int getBankedDiamonds() { return bankedDiamonds; }
    public int getTotalBankedDiamonds() { return totalBankedDiamonds; }
    public int getElapsedTicks() { return elapsedTicks; }
    public int getScore() { return score; }
    public boolean isChoiceShown() { return choiceShown; }
    public void setChoiceShown(boolean shown) { choiceShown = shown; }

    public void recordKill(boolean monster) {
        if (monster) monsterKills = increment(monsterKills);
        else enemyKills = increment(enemyKills);
        addScore(monster ? 150 : 100);
    }

    public void gather(Resource resource, int amount) {
        Objects.requireNonNull(resource);
        if (amount <= 0 || (long) getGathered(resource) + amount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Gathered amount must be positive and fit its counter");
        }
        inventory.add(resource, amount);
        gathered.put(resource, getGathered(resource) + amount);
        addScore((long) amount * 10);
    }

    public boolean craft() {
        if (crafted || !achievementsComplete() || !inventory.spend(getDefinition().recipe())) return false;
        inventory.grantWeapon(getDefinition().weapon());
        crafted = true;
        addScore(200);
        return true;
    }

    /** The caller applies the healing effect after this successful food consumption. */
    public boolean eat() { return inventory.remove(Resource.MEAT, 1); }

    public int depositDiamonds() {
        if (!crafted || !achievementsComplete()) return 0;
        int amount = inventory.count(Resource.DIAMOND);
        if (amount == 0) return 0;
        if ((long) bankedDiamonds + amount > Integer.MAX_VALUE
                || (long) totalBankedDiamonds + amount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Diamond bank is full");
        }
        inventory.remove(Resource.DIAMOND, amount);
        bankedDiamonds += amount;
        totalBankedDiamonds += amount;
        addScore((long) amount * 100);
        return amount;
    }

    public boolean isReady() {
        return crafted && achievementsComplete() && bankedDiamonds >= getDefinition().diamonds();
    }

    public boolean advance() {
        if (level >= 3 || !isReady()) return false;
        level++;
        enemyKills = 0;
        monsterKills = 0;
        gathered.replaceAll((resource, amount) -> 0);
        crafted = false;
        bankedDiamonds = 0;
        choiceShown = false;
        addScore(500);
        return true;
    }

    public void tick() { elapsedTicks = increment(elapsedTicks); }
    public void onDeath() { inventory.clearCarried(); }

    private boolean achievementsComplete() {
        LevelDefinition definition = getDefinition();
        if (enemyKills < definition.enemyKillTarget() || monsterKills < definition.monsterKillTarget()) return false;
        for (Map.Entry<Resource, Integer> requirement : definition.requiredGathered().entrySet()) {
            if (getGathered(requirement.getKey()) < requirement.getValue()) return false;
        }
        return true;
    }

    private void addScore(long amount) { score = (int) Math.min(10_000_000, (long) score + amount); }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
}
