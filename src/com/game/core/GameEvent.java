package com.game.core;

import java.util.Objects;

/** Immutable feedback emitted by the model without depending on sound or painting. */
public record GameEvent(Kind kind, int x, int y, int amount, String text) {
    public enum Kind { PICKUP, STRIKE, HURT, ENEMY_DEFEATED, CRAFTED, DASH, VICTORY, DEFEAT }

    public GameEvent {
        Objects.requireNonNull(kind, "Event kind is required");
        text = text == null ? "" : text;
    }
}
