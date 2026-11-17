package com.game.tests;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import com.game.art.OriginalArt;
import com.game.art.TerrainArt;
import com.game.assets.Assets;
import com.game.audio.SoundEngine;
import com.game.core.GameEvent;
import com.game.core.KeyHandler;
import com.game.effects.ParticleSystem;
import com.game.entities.Champion;
import com.game.survival.ChampionType;

/** Asset contracts and feedback resource bounds, without opening an audio device. */
public final class FeedbackTest {
    private static int checks;
    public static void main(String[] args) {
        for (String actor : new String[] {"astra", "briar", "warden", "warden_phase2"}) {
            for (String direction : new String[] {"up", "down", "left", "right"}) {
                for (int frame = 1; frame <= 2; frame++) {
                    BufferedImage walking = OriginalArt.create(actor + "_" + direction + "_" + frame + ".png");
                    check(walking != null && walking.getWidth() == 48 && walking.getHeight() == 48, "Walking sprite dimensions");
                    BufferedImage attacking = OriginalArt.create(actor + "_attack_" + direction + "_" + frame + ".png");
                    boolean vertical = direction.equals("up") || direction.equals("down");
                    check(attacking != null && attacking.getWidth() == (frame == 2 && !vertical ? 96 : 48)
                            && attacking.getHeight() == (frame == 2 && vertical ? 96 : 48), "Attack canvas follows the directional animation");
                    check(hasVisiblePixels(attacking) && attacking.getRGB(0, 0) == 0, "Art has visible detail and a transparent edge");
                }
            }
        }
        BufferedImage title = Assets.load("art/starfall-title.png");
        check(title.getWidth() >= 1120 && title.getHeight() >= 720, "High-resolution original title is packaged");
        for (int biome = 0; biome < 4; biome++) {
            check(TerrainArt.floor(0, biome).getWidth() == 48, "Native biome floor");
            check(TerrainArt.obstacle(7, biome).getHeight() == 48, "Native biome obstacle");
            check(hash(TerrainArt.floor(0, biome)) != hash(TerrainArt.floor(0, (biome + 1) % 4)), "Biome palettes are distinct");
        }
        ParticleSystem first = new ParticleSystem(42);
        ParticleSystem second = new ParticleSystem(42);
        for (GameEvent.Kind kind : GameEvent.Kind.values()) {
            GameEvent event = new GameEvent(kind, 4800, 4800, 2, "Feedback");
            first.accept(event); second.accept(event);
        }
        check(Arrays.equals(render(first), render(second)), "Seeded effects render deterministically");
        for (int i = 0; i < 1000; i++) first.accept(new GameEvent(GameEvent.Kind.CRAFTED, 0, 0, 0, ""));
        check(first.getParticleCount() <= 400, "Long sessions cannot grow the effect collection without bound");
        for (int tick = 0; tick < 100; tick++) first.update();
        check(first.getParticleCount() == 0, "Every effect expires");
        second.clear();
        check(second.getParticleCount() == 0, "New missions clear old effects");
        try (SoundEngine sound = new SoundEngine()) {
            for (int i = 0; i < 1000; i++) sound.accept(new GameEvent(GameEvent.Kind.STRIKE, 0, 0, 1, ""));
            sound.toggleMute();
            check(sound.isMuted(), "Headless checks never require an audio device");
        }
        System.out.println("FeedbackTest: " + checks + " checks passed.");
    }
    private static int[] render(ParticleSystem system) {
        BufferedImage image = new BufferedImage(1120, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { system.draw(g, new Champion(new KeyHandler(), 48, ChampionType.VANGUARD)); }
        finally { g.dispose(); }
        return image.getRGB(0, 0, 1120, 720, null, 0, 1120);
    }
    private static boolean hasVisiblePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) != 0) return true;
        }
        return false;
    }
    private static int hash(BufferedImage image) { return Arrays.hashCode(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
