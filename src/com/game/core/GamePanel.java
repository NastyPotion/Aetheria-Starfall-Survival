package com.game.core;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import com.game.audio.SoundEngine;
import com.game.core.KeyHandler.Command;
import com.game.effects.ParticleSystem;
import com.game.entities.Enemy;
import com.game.entities.Projectile;
import com.game.persistence.*;
import com.game.survival.*;
import com.game.ui.SceneRenderer;
import com.game.ui.UI;
import com.game.ui.UI.Overlay;

/** All live campaign state belongs to Swing. Only snapshots cross the file-worker boundary. */
@SuppressWarnings("serial") // Swing serialization is not used; saves contain GameSnapshot values.
public final class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final KeyHandler input = new KeyHandler();
    private final UI ui = new UI();
    private final SceneRenderer scene = new SceneRenderer();
    private final ParticleSystem particles = new ParticleSystem();
    private final SoundEngine audio = new SoundEngine();
    private final SaveRepository repository;
    private final Timer timer = new Timer(1000 / 60, event -> update());
    private GameSession session = new GameSession(input);
    private ChampionType selectedChampion = ChampionType.VANGUARD;
    private Difficulty selectedDifficulty = Difficulty.EASY;
    private Overlay overlay = Overlay.NONE;
    private boolean ioBusy;
    private boolean resumeAfterOverlay;
    private String message = "";
    private int messageTicks, visualTicks, shakeTicks;
    private int mouseX = -1, mouseY = -1, achievementLevel = 1;

    public GamePanel() {
        this(new FileSaveRepository(Path.of(System.getProperty("user.home"), ".aetheria", "save.properties")));
    }
    public GamePanel(SaveRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
        setPreferredSize(new Dimension(GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(input);
        addFocusListener(input);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                session.pause();
                resumeAfterOverlay=false;
                repaint();
            }
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouseX=e.getX();
                mouseY=e.getY();
                setCursor(Cursor.getPredefinedCursor(ui.actionAt(mouseX,mouseY,session.getState(),view(),ioBusy)!=UI.Action.NONE?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                mouseX=-1;
                mouseY=-1;
                repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if(overlay==Overlay.MAP){
                    closeOverlay();
                    return;
                }
                perform(ui.actionAt(e.getX(),e.getY(),session.getState(),view(),ioBusy));
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        timer.setCoalesce(true);
    }
    private UI.ViewState view() {
        return new UI.ViewState(overlay,audio.isMuted(),visualTicks,mouseX,mouseY,selectedChampion,selectedDifficulty,achievementLevel);
    }
    public void startGame() {
        timer.start();
        requestFocusInWindow();
    }
    public void stopGame() {
        timer.stop();
        input.clear();
        audio.close();
    }
    public void update() {
        visualTicks=(visualTicks+1)%1_000_000;
        if(shakeTicks>0)shakeTicks--;
        if(messageTicks>0&&--messageTicks==0)message="";
        if(ioBusy){
            input.clear();
            repaint();
            return;
        }
        handleCommands();
        if(!ioBusy&&overlay==Overlay.NONE){
            session.update();
            if(session.getState()!=GameState.PAUSED)particles.update();
        }
        drainFeedback();
        repaint();
    }
    /** Translate input into model operations before advancing the simulation. */
    private void handleCommands() {
        if(input.consume(Command.MUTE))audio.toggleMute();
        if(input.consume(Command.HELP))toggleOverlay(Overlay.GUIDE);
        if(input.consume(Command.MAP)&&session.getState()!=GameState.INTRO)toggleOverlay(Overlay.MAP);
        if(input.consume(Command.INVENTORY)&&session.getState()!=GameState.INTRO)toggleOverlay(Overlay.INVENTORY);
        if(input.consume(Command.ACHIEVEMENTS)&&session.getState()!=GameState.INTRO){
            achievementLevel=session.getCampaign().getLevel();
            toggleOverlay(Overlay.ACHIEVEMENTS);
        }
        if(input.consume(Command.CRAFT)){
            if(overlay==Overlay.INVENTORY)session.craft();
            else if(session.getState()==GameState.PLAYING||session.getState()==GameState.PAUSED)toggleOverlay(Overlay.INVENTORY);
        }
        if(input.consume(Command.EAT))session.eat();
        if(input.consume(Command.EQUIP_FISTS))session.equip(WeaponType.FISTS);
        if(input.consume(Command.EQUIP_PICKAXE))session.equip(WeaponType.PICKAXE);
        if(input.consume(Command.EQUIP_SWORD))session.equip(WeaponType.SWORD);
        if(input.consume(Command.EQUIP_GUN))session.equip(WeaponType.GUN);
        if(input.consume(Command.NEXT)&&!session.requestLevelChoice())showMessage("Complete the achievements and return to your ship to choose the next level.");
        if(input.consume(Command.PAUSE)){
            if(overlay!=Overlay.NONE)closeOverlay();
            else session.togglePause();
        }
        if(input.consume(Command.CONFIRM)){
            if(overlay!=Overlay.NONE)closeOverlay();
            else switch(session.getState()){
                case INTRO -> beginNewGame();
                case LOST -> session.retry();
                case LEVEL_COMPLETE,WON -> session.stayOnLevel();
                default -> session.start();
            }
        }
        if(input.consume(Command.RESTART)&&session.getState()==GameState.LOST)session.retry();
        if(input.consume(Command.INTERACT)&&overlay==Overlay.NONE)session.interact();
        boolean save=input.consume(Command.SAVE),load=input.consume(Command.LOAD);
        if(load)transferSave(false);
        else if(save)transferSave(true);
    }

    private void drainFeedback(){
        for(GameEvent event:session.drainEvents()){
            particles.accept(event);
            audio.accept(event);
            if(!event.text().isBlank())showMessage(event.text());
            if(event.kind()==GameEvent.Kind.HURT)shakeTicks=10;
        }
    }
    private void perform(UI.Action action){
        switch(action){
            case PLAY -> beginNewGame();
            case RESUME -> session.start();
            case RESTART -> session.retry();
            case LOAD -> transferSave(false);
            case SAVE -> transferSave(true);
            case GUIDE -> toggleOverlay(Overlay.GUIDE);
            case CHAMPION_MENU -> toggleOverlay(Overlay.CHAMPION);
            case DIFFICULTY_MENU -> toggleOverlay(Overlay.DIFFICULTY);
            case CLOSE -> closeOverlay();
            case MUTE -> audio.toggleMute();
            case ASTRA -> {
                selectedChampion=ChampionType.VANGUARD;
                closeOverlay();
            }
            case NYX -> {
                selectedChampion=ChampionType.SCOUT;
                closeOverlay();
            }
            case ORION -> {
                selectedChampion=ChampionType.ENGINEER;
                closeOverlay();
            }
            case EASY -> {
                selectedDifficulty=Difficulty.EASY;
                closeOverlay();
            }
            case MEDIUM -> {
                selectedDifficulty=Difficulty.MEDIUM;
                closeOverlay();
            }
            case HARD -> {
                selectedDifficulty=Difficulty.HARD;
                closeOverlay();
            }
            case CRAFT -> session.craft();
            case EAT -> session.eat();
            case FISTS -> session.equip(WeaponType.FISTS);
            case PICKAXE -> session.equip(WeaponType.PICKAXE);
            case SWORD -> session.equip(WeaponType.SWORD);
            case GUN -> session.equip(WeaponType.GUN);
            case STAY -> session.stayOnLevel();
            case ADVANCE -> {
                session.advanceLevel();
                particles.clear();
                achievementLevel=session.getCampaign().getLevel();
            }
            case LEVEL1 -> achievementLevel=1;
            case LEVEL2 -> achievementLevel=2;
            case LEVEL3 -> achievementLevel=3;
            case MENU -> {
                overlay=Overlay.NONE;
                session.returnToMenu();
            }
            case QUIT -> {
                Window window=SwingUtilities.getWindowAncestor(this);
                if(window!=null)window.dispatchEvent(new WindowEvent(window,WindowEvent.WINDOW_CLOSING));
            }
            case NONE -> {
            }
        }
        input.clear();
        drainFeedback();
    }
    private void beginNewGame(){
        input.clear();
        particles.clear();
        session=new GameSession(input,selectedChampion,selectedDifficulty);
        session.start();
        overlay=Overlay.NONE;
        achievementLevel=1;
        showMessage("Two hearts. Find food, then follow J for your level-one achievements.");
    }
    private void toggleOverlay(Overlay target){
        if(overlay==target){
            closeOverlay();
            return;
        }
        if(overlay==Overlay.NONE)resumeAfterOverlay=session.getState()==GameState.PLAYING;
        session.pause();
        input.clear();
        overlay=target;
    }
    private void closeOverlay(){
        overlay=Overlay.NONE;
        if(resumeAfterOverlay)session.start();
        resumeAfterOverlay=false;
        input.clear();
    }
    private void transferSave(boolean saving){
        if(ioBusy)return;
        if(saving&&session.getState()==GameState.LOST){
            showMessage("Respawn or load a saved campaign before saving.");
            return;
        }
        if(saving&&session.getState()==GameState.INTRO&&session.getCampaign().getElapsedTicks()==0)session=new GameSession(input,selectedChampion,selectedDifficulty);
        // A menu save must resume this captured campaign when Enter is pressed.
        if(saving&&session.getState()==GameState.INTRO)session.start();
        session.pause();
        GameSnapshot captured=saving?session.snapshot():null;
        ioBusy=true;
        overlay=Overlay.NONE;
        resumeAfterOverlay=false;
        input.clear();
        new SwingWorker<GameSnapshot,Void>(){
            @Override protected GameSnapshot doInBackground()throws SaveGameException{
                if(saving){
                    repository.save(captured);
                    return null;
                }
                return repository.load();
            }
            @Override protected void done(){
                try{
                    GameSnapshot loaded=get();
                    if(!saving){
                        session=GameSession.fromSnapshot(input,loaded);
                        selectedChampion=session.getPlayer().getChampionType();
                        selectedDifficulty=session.getCampaign().getDifficulty();
                        achievementLevel=session.getCampaign().getLevel();
                        particles.clear();
                    }else session.pause();
                    showMessage(saving?"Campaign saved. Enter to resume.":"Campaign restored. Enter to resume.");
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    showMessage("Save operation interrupted. Please retry.");
                }
                catch(ExecutionException e){
                    showMessage(e.getCause().getMessage());
                }
                finally{
                    ioBusy=false;
                    input.clear();
                    repaint();
                }
            }
        }.execute();
    }
    private void showMessage(String text){
        message=text==null?"Save operation failed.":text;
        messageTicks=300;
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if(session.getState()!=GameState.INTRO){
                int dx=shakeTicks>0?(int)(Math.sin(visualTicks*3.1)*3):0,dy=shakeTicks>0?(int)(Math.cos(visualTicks*2.7)*3):0;
                g.translate(dx,dy);
                session.getChunk().draw(g,48,session.getPlayer());
                scene.drawDevices(g,session,visualTicks);
                scene.drawTelegraphs(g,session,visualTicks);
                for(Enemy enemy:session.getEnemies())enemy.draw(g,48);
                session.getPlayer().draw(g,48);
                for(Projectile projectile:session.getProjectiles())projectile.draw(g,48,session.getPlayer());
                particles.draw(g,session.getPlayer());
                g.translate(-dx,-dy);
                scene.drawAtmosphere(g,session,visualTicks);
            }
            ui.draw(g,session,message,ioBusy,view());
        }finally{
            g.dispose();
        }
    }
    @Override public void removeNotify(){
        stopGame();
        super.removeNotify();
    }
}
