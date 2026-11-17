package com.game.core;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting " + GameConfig.TITLE + "...");
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.startGame();
        });
    }
}
