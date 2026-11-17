package com.game.persistence;

/** A campaign can be stored without exposing its file format to the game panel. */
public interface SaveRepository {
    void save(GameSnapshot snapshot) throws SaveGameException;

    GameSnapshot load() throws SaveGameException;
}
