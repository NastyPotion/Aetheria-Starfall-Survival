package com.game.world;

import com.game.entities.Champion;
import com.game.survival.CampaignProgress;

/** The safe destination that turns carried diamonds into secured campaign progress. */
public final class ShipDock implements Interactable {
    private final int x;
    private final int y;
    private final CampaignProgress progress;
    public ShipDock(int x, int y, CampaignProgress progress) { this.x = x; this.y = y; this.progress = java.util.Objects.requireNonNull(progress); }
    public int getWorldX() { return x; }
    public int getWorldY() { return y; }
    public boolean isInRange(Champion player) { return Math.hypot(player.getWorldX() - x, player.getWorldY() - y) <= 48 * 2.6; }
    @Override public String getPrompt() {
        if (!progress.isReady()) return "F  Deposit diamond cargo";
        return progress.getLevel() == 3 ? "F  Stay or return home" : "F  Stay or next level";
    }
    @Override public boolean interact(Champion player) {
        return player.isAlive() && isInRange(player) && progress.depositDiamonds() > 0;
    }
}
