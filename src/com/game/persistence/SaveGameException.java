package com.game.persistence;

/** A recoverable persistence failure that the game can explain to its player. */
public final class SaveGameException extends Exception {
    private static final long serialVersionUID = 1L;

    public SaveGameException(String message) {
        super(message);
    }

    public SaveGameException(String message, Throwable cause) {
        super(message, cause);
    }
}
