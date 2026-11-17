package com.game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import com.game.assets.Assets;
import com.game.core.GameConfig;
import com.game.core.GameSession;
import com.game.entities.Champion;
import com.game.entities.Enemy;
import com.game.entities.MeleeEnemy;
import com.game.entities.Skeleton;


/** World-space feedback is separate from the mission's interaction rules. */
public final class SceneRenderer {
    private static final Color GOLD = new Color(245, 195, 114);
    private static final Color TEAL = new Color(113, 230, 211);

    public void drawDevices(Graphics2D g, GameSession game, int tick) {
        Champion player = game.getPlayer();
        int shipX = screenX(GameSession.SHIP_X, player);
        int shipY = screenY(GameSession.SHIP_Y, player);
        g.setColor(new Color(6, 19, 29, 95));
        g.fillOval(shipX - 38, shipY + 20, 175, 85);
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(179, 199, 172, 100));
        g.drawOval(shipX - 36, shipY + 12, 170, 92);
        g.drawImage(Assets.load("spaceship_2.png"), shipX - 18, shipY - 26, 144, 108, null);
        if (game.isNearShip()) {
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            String label = game.getShip().getPrompt();
            int width = g.getFontMetrics().stringWidth(label);
            g.setColor(new Color(8, 23, 32, 235));
            g.fillRoundRect(shipX + 140, shipY + 14, width + 20, 26, 10, 10);
            g.setColor(GOLD);
            g.drawString(label, shipX + 150, shipY + 32);
        }
    }

    public void drawTelegraphs(Graphics2D g, GameSession game, int tick) {
        Champion player = game.getPlayer();
        for (Enemy enemy : game.getEnemies()) {
            if (!enemy.isAlive()) continue;
            int x = screenX(enemy.getWorldX(), player) + 16;
            int y = screenY(enemy.getWorldY(), player) + 16;
            if (enemy instanceof MeleeEnemy melee && melee.isWindingUp()) {
                Rectangle strike = melee.getStrikeBounds(GameConfig.TILE_SIZE);
                int sx = screenX(strike.x, player);
                int sy = screenY(strike.y, player);
                g.setColor(new Color(255, 128, 103, 65));
                g.fillRoundRect(sx, sy, strike.width, strike.height, 12, 12);
                g.setColor(new Color(255, 175, 140, 185));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(sx, sy, strike.width, strike.height, 12, 12);
            }
            if (enemy instanceof Skeleton boss) {
                Color color = boss.isEnraged() ? new Color(255, 124, 130, 150) : new Color(196, 148, 245, 110);
                g.setColor(color);
                g.setStroke(new BasicStroke(1));
                int radius = 35 + (int) (Math.sin(tick * 0.07) * 3);
                g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            }
            if (Math.hypot(enemy.getWorldX() - player.getWorldX(), enemy.getWorldY() - player.getWorldY()) < 260) {
                g.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
                String name = enemy.getDisplayName();
                int width = g.getFontMetrics().stringWidth(name);
                g.setColor(new Color(6, 19, 29, 195));
                g.fillRoundRect(x - width / 2 - 5, y - 52, width + 10, 18, 6, 6);
                g.setColor(new Color(221, 231, 216));
                g.drawString(name, x - width / 2, y - 39);
            }
        }
    }

    public void drawAtmosphere(Graphics2D g, GameSession game, int tick) {
        for (int i = 0; i < 32; i++) {
            int x = Math.floorMod(i * 139 + (int) (Math.sin(tick * .009 + i) * 24), GameConfig.VIEW_WIDTH);
            int y = Math.floorMod(i * 83 - tick / 7, GameConfig.VIEW_HEIGHT);
            int alpha = 30 + (int) ((Math.sin(tick * .035 + i * 2) + 1) * 40);
            g.setColor(new Color(144, 246, 218, alpha));
            g.fillRect(x, y, i % 3 == 0 ? 3 : 2, 2);
        }
        g.setPaint(new RadialGradientPaint(GameConfig.VIEW_WIDTH / 2f, GameConfig.VIEW_HEIGHT / 2f,
                GameConfig.VIEW_WIDTH * .67f, new float[] {0, .5f, 1},
                new Color[] {new Color(4, 13, 26, 0), new Color(4, 13, 26, 8), new Color(4, 13, 26, 180)}));
        g.fillRect(0, 0, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT);
        if (game.getPlayer().getCurrentHealth() <= 3) {
            g.setColor(new Color(195, 52, 67, 35 + (int) (Math.sin(tick * .09) * 15)));
            g.setStroke(new BasicStroke(14));
            g.drawRect(0, 0, GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT);
        }
    }

    private int screenX(int worldX, Champion player) { return worldX - player.getWorldX() + player.getScreenX(); }
    private int screenY(int worldY, Champion player) { return worldY - player.getWorldY() + player.getScreenY(); }
}
