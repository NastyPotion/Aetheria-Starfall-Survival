package com.game.world;

import com.game.entities.Champion;

/** A world object that can respond to a nearby explorer's deliberate action. */
public interface Interactable {
    String getPrompt();
    boolean interact(Champion explorer);
}
