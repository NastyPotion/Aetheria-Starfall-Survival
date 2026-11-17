package com.game.tests;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import com.game.core.*;
import com.game.entities.*;
import com.game.persistence.*;
import com.game.survival.*;
import com.game.ui.*;

/** Real menu events plus rendered survival screens, without an interactive window. */
public final class SurvivalUITest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                GamePanel panel=new GamePanel(new SaveRepository() {
                    @Override public void save(GameSnapshot s) { }
                    @Override public GameSnapshot load() throws SaveGameException { throw new SaveGameException("No saved campaign"); }
                });
                panel.setSize(1120,720);
                BufferedImage title=paint(panel,"survival-title");
                click(panel,150,490);BufferedImage champions=paint(panel,"choose-champion");
                check(!same(title,champions),"Champion menu responds to a real mouse click");
                click(panel,500,482); // Nyx
                click(panel,420,490);BufferedImage difficulties=paint(panel,"choose-difficulty");
                check(!same(title,difficulties),"Difficulty menu opens");
                click(panel,780,470); // Hard
                click(panel,150,425);panel.update();BufferedImage world=paint(panel,"survival-playing");
                check(!same(world,title),"Start Game enters the world");panel.stopGame();
                for(UI.Overlay overlay:new UI.Overlay[]{UI.Overlay.INVENTORY,UI.Overlay.ACHIEVEMENTS,UI.Overlay.GUIDE,UI.Overlay.MAP}){
                    GameSession game=new GameSession(new KeyHandler(),ChampionType.ENGINEER,Difficulty.MEDIUM,42);game.start();
                    render(game,overlay,"survival-"+overlay.name().toLowerCase());
                }
                GameSession game=new GameSession(new KeyHandler(),ChampionType.VANGUARD,Difficulty.EASY,42);game.start();
                for(int level=1;level<=3;level++){
                    CampaignProgress c=game.getCampaign();LevelDefinition d=c.getDefinition();
                    for(int i=0;i<d.enemyKillTarget();i++)c.recordKill(false);
                    for(int i=0;i<d.monsterKillTarget();i++)c.recordKill(true);
                    for(var entry:d.requiredGathered().entrySet())c.gather(entry.getKey(),entry.getValue());
                    check(game.craft(),"Current level recipe can be crafted");
                    c.gather(Resource.DIAMOND,d.diamonds());game.getPlayer().setPosition(GameSession.SHIP_X,GameSession.SHIP_Y);
                    check(game.interact(),"Ship accepts cargo after preparation");
                    render(game,UI.Overlay.ACHIEVEMENTS,"level-"+level+"-achievements");render(game,UI.Overlay.NONE,"level-"+level+"-choice");
                    check(game.getState()==GameState.LEVEL_COMPLETE,"Complete level offers a choice");
                    check(game.advanceLevel(),"Next level or final return is explicit");
                }
                render(game,UI.Overlay.NONE,"survival-congratulations");check(game.getState()==GameState.WON,"Final return completes all three levels");
                game.stayOnLevel();check(game.getState()==GameState.PLAYING,"Final completion also permits continued exploration");
                System.out.println("SurvivalUITest: "+checks+" checks passed.");
            } catch(Exception e){throw new IllegalStateException(e);}
        });
    }
    private static void click(GamePanel panel,int x,int y){MouseEvent e=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false);for(var listener:panel.getMouseListeners())listener.mousePressed(e);}
    private static BufferedImage paint(GamePanel panel,String name)throws Exception{BufferedImage b=new BufferedImage(1120,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();try{panel.paint(g);}finally{g.dispose();}write(b,name);return b;}
    private static void render(GameSession game,UI.Overlay overlay,String name)throws Exception{
        BufferedImage b=new BufferedImage(1120,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();
        try{g.setClip(0,0,1120,720);game.getChunk().draw(g,48,game.getPlayer());SceneRenderer scene=new SceneRenderer();scene.drawDevices(g,game,0);scene.drawTelegraphs(g,game,0);for(Enemy e:game.getEnemies())e.draw(g,48);game.getPlayer().draw(g,48);scene.drawAtmosphere(g,game,0);new UI().draw(g,game,"",false,new UI.ViewState(overlay,true,0,-1,-1,game.getPlayer().getChampionType(),game.getCampaign().getDifficulty(),game.getCampaign().getLevel()));}finally{g.dispose();}write(b,name);
    }
    private static void write(BufferedImage b,String name)throws Exception{Path folder=Path.of("build","previews");Files.createDirectories(folder);check(ImageIO.write(b,"png",folder.resolve(name+".png").toFile()),"Readable PNG written: "+name);}
    private static boolean same(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,1120,720,null,0,1120),b.getRGB(0,0,1120,720,null,0,1120));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
