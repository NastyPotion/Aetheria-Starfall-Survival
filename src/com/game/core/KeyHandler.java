package com.game.core;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Keyboard state and one-shot commands, used only on Swing's event thread. */
public final class KeyHandler implements KeyListener, FocusListener {
    public enum Command { CONFIRM, PAUSE, RESTART, SAVE, LOAD, INTERACT, MAP, HELP, MUTE,
        INVENTORY, ACHIEVEMENTS, CRAFT, EAT, NEXT, EQUIP_FISTS, EQUIP_PICKAXE, EQUIP_SWORD, EQUIP_GUN }
    private final Set<Integer> pressed = new HashSet<>();
    private final EnumSet<Command> commands = EnumSet.noneOf(Command.class);

    public boolean isUpPressed() { return pressed.contains(KeyEvent.VK_W) || pressed.contains(KeyEvent.VK_UP); }
    public boolean isDownPressed() { return pressed.contains(KeyEvent.VK_S) || pressed.contains(KeyEvent.VK_DOWN); }
    public boolean isLeftPressed() { return pressed.contains(KeyEvent.VK_A) || pressed.contains(KeyEvent.VK_LEFT); }
    public boolean isRightPressed() { return pressed.contains(KeyEvent.VK_D) || pressed.contains(KeyEvent.VK_RIGHT); }
    public boolean isAttackPressed() { return pressed.contains(KeyEvent.VK_E) || pressed.contains(KeyEvent.VK_SPACE); }
    public boolean isDashPressed() { return pressed.contains(KeyEvent.VK_SHIFT); }
    public boolean consume(Command command) { return commands.remove(command); }
    public void clear() { pressed.clear(); commands.clear(); }
    @Override public void keyTyped(KeyEvent event) { }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (!pressed.add(key)) return;
        switch (key) {
            case KeyEvent.VK_ENTER -> commands.add(Command.CONFIRM);
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> commands.add(Command.PAUSE);
            case KeyEvent.VK_R -> commands.add(Command.RESTART);
            case KeyEvent.VK_F5 -> commands.add(Command.SAVE);
            case KeyEvent.VK_F9 -> commands.add(Command.LOAD);
            case KeyEvent.VK_F -> commands.add(Command.INTERACT);
            case KeyEvent.VK_TAB -> commands.add(Command.MAP);
            case KeyEvent.VK_H -> commands.add(Command.HELP);
            case KeyEvent.VK_M -> commands.add(Command.MUTE);
            case KeyEvent.VK_I -> commands.add(Command.INVENTORY);
            case KeyEvent.VK_J -> commands.add(Command.ACHIEVEMENTS);
            case KeyEvent.VK_C -> commands.add(Command.CRAFT);
            case KeyEvent.VK_Q -> commands.add(Command.EAT);
            case KeyEvent.VK_N -> commands.add(Command.NEXT);
            case KeyEvent.VK_1 -> commands.add(Command.EQUIP_FISTS);
            case KeyEvent.VK_2 -> commands.add(Command.EQUIP_PICKAXE);
            case KeyEvent.VK_3 -> commands.add(Command.EQUIP_SWORD);
            case KeyEvent.VK_4 -> commands.add(Command.EQUIP_GUN);
            default -> { }
        }
    }

    @Override public void keyReleased(KeyEvent event) { pressed.remove(event.getKeyCode()); }
    @Override public void focusGained(FocusEvent event) { }
    @Override public void focusLost(FocusEvent event) { clear(); }
}
