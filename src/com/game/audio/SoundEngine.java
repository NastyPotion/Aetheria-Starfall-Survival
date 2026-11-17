package com.game.audio;

import com.game.core.GameEvent;
import java.awt.GraphicsEnvironment;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Small synthesized event cues. One bounded daemon worker owns the audio device;
 * accepting events never opens a device or writes audio on Swing's event thread.
 */
public final class SoundEngine implements AutoCloseable {
    private static final float SAMPLE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    // Volatile makes the UI's flags visible while the worker is playing a cue.
    private volatile boolean muted;
    private volatile boolean disabled = GraphicsEnvironment.isHeadless();
    private volatile boolean closed;
    private long previousStrike;
    private final Map<GameEvent.Kind, byte[]> sounds = new EnumMap<>(GameEvent.Kind.class);
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(8), runnable -> {
                Thread thread = new Thread(runnable, "aetheria-audio");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());
    private SourceDataLine line;

    // These three short control methods share one lock. Device writes never hold it.
    public synchronized void accept(GameEvent event) {
        Objects.requireNonNull(event, "Event is required");
        if (isMuted()) return;
        if (event.kind() == GameEvent.Kind.STRIKE) {
            long now = System.nanoTime();
            if (now - previousStrike < TimeUnit.MILLISECONDS.toNanos(70)) return;
            previousStrike = now;
        }
        if (event.kind() == GameEvent.Kind.VICTORY || event.kind() == GameEvent.Kind.DEFEAT) {
            worker.getQueue().clear();
        }
        worker.execute(() -> play(event.kind()));
    }

    public synchronized void toggleMute() {
        if (closed || disabled) return;
        muted = !muted;
        if (muted) worker.getQueue().clear();
    }

    public boolean isMuted() {
        return muted || disabled || closed;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        worker.getQueue().clear();
        // Cleanup follows any current short cue, without waiting on the UI thread.
        worker.execute(this::closeLine);
        worker.shutdown();
    }

    private void play(GameEvent.Kind kind) {
        if (isMuted()) return;
        try {
            if (line == null) {
                line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, 4_096);
                line.start();
            }
            byte[] samples = sounds.computeIfAbsent(kind, SoundEngine::synthesize);
            int offset = 0;
            while (offset < samples.length && !isMuted()) {
                // Small writes also bound how much a cue can continue after mute.
                int written = line.write(samples, offset, Math.min(1_024, samples.length - offset));
                if (written <= 0) break;
                offset += written;
            }
            if (isMuted()) line.flush();
        } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException | SecurityException exception) {
            disabled = true;
            worker.getQueue().clear();
            closeLine();
        }
    }

    private void closeLine() {
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
            line = null;
        }
    }

    private static byte[] synthesize(GameEvent.Kind kind) {
        double duration = switch (kind) {
            case PICKUP -> 0.13;
            case STRIKE, DASH -> 0.075;
            case HURT -> 0.12;
            case ENEMY_DEFEATED -> 0.19;
            case CRAFTED -> 0.28;
            case VICTORY -> 0.38;
            case DEFEAT -> 0.29;
        };
        int count = (int) (duration * SAMPLE_RATE);
        byte[] pcm = new byte[count * 2];
        double phase = 0;
        for (int index = 0; index < count; index++) {
            double time = index / (double) SAMPLE_RATE;
            double progress = time / duration;
            double frequency = switch (kind) {
                case PICKUP -> progress < 0.45 ? 660 : 990;
                case STRIKE -> 560 - 410 * progress;
                case HURT -> 205 - 105 * progress;
                case ENEMY_DEFEATED -> progress < 0.5 ? 440 : 660;
                case CRAFTED -> progress < 0.33 ? 523.25 : progress < 0.66 ? 659.25 : 783.99;
                case DASH -> 330 + 400 * progress;
                case VICTORY -> progress < 0.25 ? 523.25 : progress < 0.5 ? 659.25
                        : progress < 0.75 ? 783.99 : 1_046.5;
                case DEFEAT -> progress < 0.33 ? 330 : progress < 0.66 ? 261.63 : 196;
            };
            phase += Math.PI * 2 * frequency / SAMPLE_RATE;
            double envelope = Math.min(1, time / 0.008) * Math.min(1, (duration - time) / 0.055);
            double signal = Math.sin(phase) * 0.78 + Math.sin(phase * 2) * 0.12;
            if (kind == GameEvent.Kind.STRIKE || kind == GameEvent.Kind.DASH) {
                double noise = ((index * 1_103_515_245L + 12_345) & 0xffff) / 32_768.0 - 1;
                signal = signal * 0.34 + noise * 0.3;
            }
            short value = (short) (signal * envelope * 0.15 * Short.MAX_VALUE);
            pcm[index * 2] = (byte) (value & 0xff);
            pcm[index * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return pcm;
    }
}
