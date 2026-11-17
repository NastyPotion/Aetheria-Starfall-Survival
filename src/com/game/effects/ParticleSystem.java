package com.game.effects;

import com.game.core.GameEvent;
import com.game.entities.Champion;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Camera-aware, finite visual feedback. All calls belong to the game/UI thread. */
public final class ParticleSystem {
    private static final int MAX_EFFECTS = 400;
    private static final Color GOLD = new Color(255, 211, 113);
    private static final Color ICE = new Color(130, 234, 246);
    private static final Color CORAL = new Color(255, 119, 110);
    private static final Color MINT = new Color(161, 239, 189);
    private static final Color INK = new Color(7, 19, 27);
    private final Random random;
    private final List<Effect> effects = new ArrayList<>();

    public ParticleSystem() {
        this(0xAE7E_21AL);
    }

    /** A seed also makes effect lifetimes and screenshots reproducible in tests. */
    public ParticleSystem(long seed) {
        random = new Random(seed);
    }

    public void accept(GameEvent event) {
        Objects.requireNonNull(event, "Event is required");
        switch (event.kind()) {
            case PICKUP -> {
                burst(event.x(), event.y(), 12, MINT, 1.4, 34);
                add(new Ring(event.x(), event.y(), MINT, 5, 24, 22));
                if (event.amount() > 0) label(event, "+" + event.amount() + " signal", MINT);
            }
            case STRIKE -> {
                burst(event.x(), event.y(), 13, GOLD, 2.3, 23);
                add(new Ring(event.x(), event.y(), GOLD, 3, 21, 14));
                if (event.amount() > 0) label(event, "-" + event.amount(), GOLD);
            }
            case HURT -> {
                burst(event.x(), event.y(), 18, CORAL, 2.1, 29);
                add(new Ring(event.x(), event.y(), CORAL, 6, 30, 19));
                if (event.amount() > 0) label(event, "-" + event.amount(), CORAL);
            }
            case ENEMY_DEFEATED -> {
                burst(event.x(), event.y(), 32, GOLD, 2.6, 46);
                add(new Ring(event.x(), event.y(), GOLD, 5, 47, 32));
                label(event, event.amount() > 0 ? "+" + event.amount() : "Guardian defeated", GOLD);
            }
            case CRAFTED -> {
                burst(event.x(), event.y(), 58, ICE, 3.2, 57);
                add(new Ring(event.x(), event.y(), ICE, 10, 76, 47));
                add(new Ring(event.x(), event.y(), GOLD, 4, 51, 34));
                label(event, "Relay online", ICE);
            }
            case DASH -> {
                burst(event.x(), event.y(), 13, ICE, 0.9, 20);
                add(new Ring(event.x(), event.y(), ICE, 5, 20, 15));
            }
            case VICTORY -> {
                burst(event.x(), event.y(), 90, GOLD, 3.5, 76);
                add(new Ring(event.x(), event.y(), ICE, 10, 115, 64));
                add(new Ring(event.x(), event.y(), GOLD, 3, 83, 52));
                label(event, "Mission complete", GOLD);
            }
            case DEFEAT -> {
                burst(event.x(), event.y(), 25, CORAL, 1.6, 47);
                add(new Ring(event.x(), event.y(), CORAL, 8, 43, 36));
            }
        }
    }

    public void update() {
        effects.removeIf(effect -> !effect.update());
    }

    public void draw(Graphics2D graphics, Champion player) {
        Objects.requireNonNull(graphics, "Graphics are required");
        Objects.requireNonNull(player, "Player camera is required");
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cameraX = player.getWorldX() - player.getScreenX();
            int cameraY = player.getWorldY() - player.getScreenY();
            for (Effect effect : effects) effect.draw(g, cameraX, cameraY);
        } finally {
            g.dispose();
        }
    }

    public void clear() {
        effects.clear();
    }

    /** Counts every live spark, ring and label against the same bounded budget. */
    public int getParticleCount() {
        return effects.size();
    }

    private void burst(int x, int y, int count, Color color, double speed, int lifetime) {
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double velocity = speed * (0.35 + random.nextDouble() * 0.65);
            add(new Spark(x + random.nextDouble() * 6 - 3, y + random.nextDouble() * 6 - 3,
                    Math.cos(angle) * velocity, Math.sin(angle) * velocity - 0.3,
                    color, 1.4 + random.nextDouble() * 1.8, lifetime + random.nextInt(13)));
        }
    }

    private void label(GameEvent event, String text, Color color) {
        String concise = text.length() > 32 ? text.substring(0, 29) + "..." : text;
        add(new FloatingLabel(event.x(), event.y() - 14, concise, color));
    }

    private void add(Effect effect) {
        if (effects.size() == MAX_EFFECTS) effects.remove(0);
        effects.add(effect);
    }

    private static void opacity(Graphics2D g, double opacity) {
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, opacity))));
    }

    private interface Effect {
        boolean update();
        void draw(Graphics2D g, int cameraX, int cameraY);
    }

    private static final class Spark implements Effect {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private final Color color;
        private final double size;
        private final int lifetime;
        private int age;

        private Spark(double x, double y, double vx, double vy, Color color, double size, int lifetime) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.lifetime = lifetime;
        }

        @Override public boolean update() {
            x += vx;
            y += vy;
            vx *= 0.968;
            vy = vy * 0.968 + 0.012;
            return ++age < lifetime;
        }

        @Override public void draw(Graphics2D g, int cameraX, int cameraY) {
            int screenX = (int) Math.round(x - cameraX);
            int screenY = (int) Math.round(y - cameraY);
            double fade = 1.0 - (double) age / lifetime;
            int radius = Math.max(1, (int) Math.round(size * (0.4 + fade * 0.6)));
            g.setColor(color);
            opacity(g, fade * 0.13);
            g.fillOval(screenX - radius * 3, screenY - radius * 3, radius * 6, radius * 6);
            opacity(g, fade * 0.88);
            g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        }
    }

    private static final class Ring implements Effect {
        private final int x;
        private final int y;
        private final Color color;
        private final int startRadius;
        private final int endRadius;
        private final int lifetime;
        private int age;

        private Ring(int x, int y, Color color, int startRadius, int endRadius, int lifetime) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.lifetime = lifetime;
        }

        @Override public boolean update() { return ++age < lifetime; }

        @Override public void draw(Graphics2D g, int cameraX, int cameraY) {
            double progress = (double) age / lifetime;
            int radius = (int) (startRadius + (endRadius - startRadius) * progress);
            opacity(g, (1 - progress) * 0.65);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x - cameraX - radius, y - cameraY - radius, radius * 2, radius * 2);
        }
    }

    private static final class FloatingLabel implements Effect {
        private final int x;
        private final int y;
        private final String text;
        private final Color color;
        private int age;

        private FloatingLabel(int x, int y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        @Override public boolean update() { return ++age < 58; }

        @Override public void draw(Graphics2D g, int cameraX, int cameraY) {
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
            int screenX = x - cameraX - g.getFontMetrics().stringWidth(text) / 2;
            int screenY = y - cameraY - age / 3;
            opacity(g, Math.min(1, (58 - age) / 18.0));
            g.setColor(INK);
            g.drawString(text, screenX + 1, screenY + 1);
            g.setColor(color);
            g.drawString(text, screenX, screenY);
        }
    }
}
