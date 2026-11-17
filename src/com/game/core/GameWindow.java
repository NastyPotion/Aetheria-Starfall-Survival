package com.game.core;

import javax.swing.JFrame;

public final class GameWindow {
    private final JFrame frame;
    private final GamePanel gamePanel;

    public GameWindow() {
        frame = new JFrame(GameConfig.TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        
        gamePanel = new GamePanel();
        frame.add(gamePanel);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void startGame() {
        gamePanel.startGame();
    }
}
