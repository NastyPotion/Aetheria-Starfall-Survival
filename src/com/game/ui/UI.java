package com.game.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import com.game.assets.Assets;
import com.game.core.*;
import com.game.entities.Champion;
import com.game.entities.Enemy;
import com.game.survival.*;

/** Presentation only: every recipe and achievement is read from the campaign model. */
public final class UI {
    public enum Overlay { NONE, GUIDE, MAP, INVENTORY, ACHIEVEMENTS, CHAMPION, DIFFICULTY }
    public enum Action { NONE, PLAY, LOAD, SAVE, QUIT, GUIDE, MUTE, RESUME, RESTART, MENU,
        CHAMPION_MENU, DIFFICULTY_MENU, CLOSE, ASTRA, NYX, ORION, EASY, MEDIUM, HARD,
        CRAFT, EAT, STAY, ADVANCE, LEVEL1, LEVEL2, LEVEL3, FISTS, PICKAXE, SWORD, GUN }
    public record ViewState(Overlay overlay, boolean muted, int tick, int mouseX, int mouseY,
            ChampionType champion, Difficulty difficulty, int achievementLevel) { }
    private static final Color INK = new Color(8,23,33,242), IVORY = new Color(238,240,218),
            MUTED = new Color(158,189,188), GOLD = new Color(246,199,125), TEAL = new Color(118,228,208);
    private final BufferedImage title = Assets.load("art/starfall-title.png");
    public void draw(Graphics2D g, GameSession game, String message, boolean busy) {
        draw(g, game, message, busy, new ViewState(Overlay.NONE,true,0,-1,-1,game.getPlayer().getChampionType(),game.getCampaign().getDifficulty(),game.getCampaign().getLevel()));
    }
    public void draw(Graphics2D g, GameSession game, String message, boolean busy, ViewState view) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (game.getState() == GameState.INTRO) title(g, view);
        else {
            GameSession.Objective objective = game.getObjective();
            hud(g,game,view,objective);
            if(game.getState()==GameState.PLAYING) waypoint(g,game,view.tick(),objective);
        }
        if(view.overlay()!=Overlay.NONE) {
            scrim(g); panel(g,128,78,864,566);
            if(view.overlay()==Overlay.GUIDE) guide(g,view);
            if(view.overlay()==Overlay.MAP) map(g,game);
            if(view.overlay()==Overlay.INVENTORY) inventory(g,game,view);
            if(view.overlay()==Overlay.ACHIEVEMENTS) achievements(g,game,view);
            if(view.overlay()==Overlay.CHAMPION) champions(g,view);
            if(view.overlay()==Overlay.DIFFICULTY) difficulty(g,view);
            button(g,new Rectangle(888,97,72,35),"CLOSE",false,view);
        } else if(game.getState()==GameState.PAUSED) pause(g,view);
        else if(game.getState()==GameState.LEVEL_COMPLETE || game.getState()==GameState.WON || game.getState()==GameState.LOST) outcome(g,game,view);
        if(busy) { scrim(g); panel(g,350,280,420,130); text(g,"SAVING YOUR JOURNEY",380,326,19,GOLD,true); text(g,"The world is safely paused.",380,365,15,IVORY,false); }
        if(!message.isBlank()) toast(g,message);
    }
    private void title(Graphics2D g, ViewState v) {
        int h=title.getHeight()*1120/title.getWidth(); g.drawImage(title,0,(720-h)/2,1120,h,null);
        g.setPaint(new GradientPaint(0,0,new Color(5,18,29,240),800,0,new Color(5,18,29,0))); g.fillRect(0,0,1120,720);
        text(g,"THREE LEVELS. ONE SHIP. A WAY HOME.",72,126,12,TEAL,true);
        g.setFont(new Font(Font.SERIF,Font.BOLD,76));g.setColor(IVORY);g.drawString("AETHERIA",65,243);
        text(g,"S T A R F A L L   S U R V I V A L",74,283,16,GOLD,false);
        text(g,"Start with two hearts. Find food. Craft your way home.",74,334,15,IVORY,false);
        text(g,"Diamonds count only when you bring them back alive.",74,360,14,MUTED,false);
        button(g,new Rectangle(72,402,246,52),"START GAME",true,v);
        button(g,new Rectangle(334,402,200,52),"CONTINUE SAVE",false,v);
        button(g,new Rectangle(72,470,246,44),"CHAMPION: "+v.champion().displayName().toUpperCase(),false,v);
        button(g,new Rectangle(334,470,200,44),"DIFFICULTY: "+v.difficulty().displayName().toUpperCase(),false,v);
        button(g,new Rectangle(72,530,145,44),"SAVE GAME",false,v);
        button(g,new Rectangle(231,530,145,44),"FIELD GUIDE",false,v);
        button(g,new Rectangle(390,530,144,44),"QUIT GAME",false,v);
        text(g,"ENTER  START     H  GUIDE     M  AUDIO",74,652,11,MUTED,false);
        text(g,"YOUR PEOPLE ARE WAITING.",830,664,11,IVORY,true);
    }
    private void hud(Graphics2D g,GameSession game,ViewState v,GameSession.Objective objective) {
        Champion p=game.getPlayer(); CampaignProgress c=game.getCampaign();
        panel(g,24,24,274,112);text(g,p.getDisplayName().toUpperCase(),42,48,12,IVORY,true);
        text(g,"LEVEL "+c.getLevel()+" / "+c.getDifficulty().displayName().toUpperCase(),143,48,10,MUTED,false);
        for(int i=0;i<6;i++){int hp=p.getCurrentHealth()-i*2;g.drawImage(Assets.load(hp>=2?"heart_full.png":hp==1?"heart_half.png":"heart_blank.png"),42+i*36,60,28,24,null);}
        text(g,"DASH",42,114,10,TEAL,true);g.setColor(new Color(36,58,65));g.fillRoundRect(86,104,156,8,8,8);g.setColor(TEAL);g.fillRoundRect(86,104,156*p.getEnergy()/p.getMaxEnergy(),8,8,8);
        panel(g,318,24,478,73);int i=0;
        for(Resource r:Resource.values()){int x=332+i++*91;g.drawImage(Assets.load(icon(r)),x,35,26,26,null);text(g,Integer.toString(c.getInventory().count(r)),x+32,56,14,IVORY,true);text(g,r.displayName(),x,81,9,MUTED,false);}
        panel(g,318,109,478,120);text(g,objective.label(),338,139,16,GOLD,true);
        text(g,"Enemies "+c.getEnemyKills()+"/"+c.getDefinition().enemyKillTarget()+"   Monsters "+c.getMonsterKills()+"/"+c.getDefinition().monsterKillTarget()+"   C  Craft "+c.getDefinition().weapon().displayName(),338,164,11,IVORY,false);
        text(g,"Diamonds: "+c.getBankedDiamonds()+" / "+c.getDefinition().diamonds()+" secured  |  J  Level achievements",338,187,11,TEAL,false);
        text(g,"F  Collectibles hide behind trees and rocks.",338,212,10,MUTED,false);
        panel(g,914,24,182,205);text(g,"SHIP RADAR",930,46,10,GOLD,true);minimap(g,game,939,60,3);text(g,"TAB  EXPAND",946,212,9,MUTED,false);
        panel(g,24,624,305,46);text(g,"WEAPON  "+p.getWeaponType().displayName().toUpperCase(),40,644,11,GOLD,true);text(g,"TOTAL BANKED  "+c.getTotalBankedDiamonds()+"     "+game.getMissionTime(),40,661,10,MUTED,false);
        g.setColor(INK);g.fillRoundRect(24,683,1072,25,8,8);text(g,"WASD MOVE    E ATTACK    SHIFT DASH    F GATHER / SHIP    I INVENTORY    C CRAFT    J ACHIEVEMENTS    Q EAT    P PAUSE",42,700,10,MUTED,false);
        text(g,v.muted()?"M AUDIO OFF":"M AUDIO ON",1000,652,10,MUTED,false);
    }
    private void waypoint(Graphics2D g,GameSession game,int tick,GameSession.Objective o) {
        Champion p=game.getPlayer();double dx=o.x()-p.getWorldX(),dy=o.y()-p.getWorldY();
        double scale=Math.min(1,Math.min(350/Math.max(1,Math.abs(dx)),160/Math.max(1,Math.abs(dy))));int x=p.getScreenX()+16+(int)(dx*scale),y=p.getScreenY()+16+(int)(dy*scale);
        int r=14+(int)(Math.sin(tick*.06)*2);g.setStroke(new BasicStroke(1.5f));g.setColor(GOLD);g.drawOval(x-r,y-r,r*2,r*2);g.drawLine(x-5,y,x+5,y);g.drawLine(x,y-5,x,y+5);
        text(g,(int)Math.ceil(Math.hypot(dx,dy)/48)+" tiles",x-18,y+33,11,GOLD,true);
    }
    private void champions(Graphics2D g,ViewState v) {
        text(g,"CHOOSE YOUR CHAMPION",164,130,23,IVORY,true);text(g,"All start with two hearts and no crafted weapon.",164,166,14,MUTED,false);
        int i=0;for(ChampionType type:ChampionType.values()){int x=164+i++*266;panel(g,x,202,244,330);String prefix=type==ChampionType.VANGUARD?"astra":type==ChampionType.SCOUT?"scout":"engineer";
            g.drawImage(Assets.load(prefix+"_down_1.png"),x+70,227,104,104,null);text(g,type.displayName(),x+78,362,22,GOLD,true);
            text(g,type.name(),x+30,394,11,TEAL,true);text(g,type==ChampionType.SCOUT?"Fast movement / 120 energy":type==ChampionType.ENGINEER?"Steady movement / 110 energy":"Balanced fighter / 100 energy",x+22,428,12,MUTED,false);
            button(g,new Rectangle(x+22,462,200,44),v.champion()==type?"SELECTED":"CHOOSE "+type.displayName().toUpperCase(),v.champion()==type,v);}
    }
    private void difficulty(Graphics2D g,ViewState v) {
        text(g,"CHOOSE DIFFICULTY",164,130,23,IVORY,true);text(g,"Recipes and level achievements stay the same. Only hostiles change.",164,169,14,MUTED,false);
        int i=0;for(Difficulty d:Difficulty.values()){int x=164+i++*266;panel(g,x,217,244,300);text(g,d.displayName().toUpperCase(),x+28,265,22,GOLD,true);
            text(g,"Extra hostiles: "+d.extraSpawns(),x+26,311,13,IVORY,false);text(g,"Health x"+d.healthMultiplier(),x+26,345,13,MUTED,false);text(g,"Stamina x"+d.staminaMultiplier(),x+26,377,13,MUTED,false);text(g,"Speed +"+d.speedBonus(),x+26,409,13,MUTED,false);
            button(g,new Rectangle(x+22,447,200,44),v.difficulty()==d?"SELECTED":"CHOOSE",v.difficulty()==d,v);}
    }
    private void inventory(Graphics2D g,GameSession game,ViewState v) {
        CampaignProgress c=game.getCampaign();text(g,"INVENTORY & CRAFTING",164,130,23,IVORY,true);text(g,"Carried diamonds are at risk until deposited at the ship.",164,165,13,GOLD,false);
        int i=0;for(Resource r:Resource.values()){int x=164+i++*158;panel(g,x,192,140,115);g.drawImage(Assets.load(icon(r)),x+18,208,40,40,null);text(g,""+c.getInventory().count(r),x+78,240,24,IVORY,true);text(g,r.displayName(),x+18,284,12,MUTED,false);}
        i=0;for(WeaponType w:WeaponType.values()){int x=164+i++*200;button(g,new Rectangle(x,333,182,44),(i)+"  "+w.displayName()+(c.getInventory().hasWeapon(w)?"":" [LOCKED]"),game.getPlayer().getWeaponType()==w,v);}
        text(g,"LEVEL "+c.getLevel()+" RECIPE: "+c.getDefinition().weapon().displayName().toUpperCase(),164,423,16,TEAL,true);
        text(g,recipe(c.getDefinition()),164,455,15,IVORY,false);text(g,c.isCrafted()?"Crafted. Use 1 / 2 / 3 / 4 to choose your equipment.":"Complete the kill and gather checklist, then craft here.",164,484,13,MUTED,false);
        button(g,new Rectangle(164,530,308,48),c.isCrafted()?"ALREADY CRAFTED":"CRAFT "+c.getDefinition().weapon().displayName().toUpperCase()+"  /  C",!c.isCrafted(),v);
        button(g,new Rectangle(492,530,252,48),"EAT MEAT  /  Q",false,v);
    }
    private void achievements(Graphics2D g,GameSession game,ViewState v) {
        CampaignProgress c=game.getCampaign();int level=v.achievementLevel();LevelDefinition d=LevelDefinition.of(level);boolean previous=level<c.getLevel(),future=level>c.getLevel();
        text(g,"LEVEL ACHIEVEMENTS",164,130,23,IVORY,true);for(int i=1;i<=3;i++)button(g,new Rectangle(164+(i-1)*220,157,200,40),"LEVEL "+i,i==level,v);
        if(future){text(g,"Complete earlier levels to unlock this stage.",164,238,13,GOLD,false);}
        List<String> labels=new ArrayList<>();List<Boolean> checks=new ArrayList<>();
        labels.add("Enemies defeated: "+(previous?d.enemyKillTarget():future?0:c.getEnemyKills())+" / "+d.enemyKillTarget());checks.add(previous||!future&&c.getEnemyKills()>=d.enemyKillTarget());
        labels.add((level==3?"Fast monsters":"Monsters")+" defeated: "+(previous?d.monsterKillTarget():future?0:c.getMonsterKills())+" / "+d.monsterKillTarget());checks.add(previous||!future&&c.getMonsterKills()>=d.monsterKillTarget());
        StringBuilder gathered=new StringBuilder("Gather ");boolean resources=true;
        for(Resource r:Resource.values())if(d.requiredGathered().containsKey(r)){int got=previous?d.requiredGathered().get(r):future?0:c.getGathered(r);gathered.append(r.displayName()).append(' ').append(got).append('/').append(d.requiredGathered().get(r)).append("   ");resources&=got>=d.requiredGathered().get(r);}
        labels.add(gathered.toString());checks.add(resources);
        labels.add("Craft "+d.weapon().displayName()+"  ("+recipe(d)+")");checks.add(previous||!future&&c.isCrafted());
        labels.add("Diamonds safely banked: "+(previous?d.diamonds():future?0:c.getBankedDiamonds())+" / "+d.diamonds());checks.add(previous||!future&&c.getBankedDiamonds()>=d.diamonds());
        for(int i=0;i<labels.size();i++){int y=278+i*57;g.setColor(checks.get(i)?TEAL:new Color(66,90,104));g.fillRoundRect(164,y-20,24,24,6,6);if(checks.get(i))text(g,"+",170,y-2,18,INK,true);text(g,labels.get(i),206,y-2,i==2?13:14,checks.get(i)?IVORY:MUTED,false);}
        text(g,"Completed? Return to the ship. You decide whether to stay or advance.",164,601,13,GOLD,false);
    }
    private void guide(Graphics2D g,ViewState v) {
        text(g,"SURVIVE. CRAFT. RETURN HOME.",164,130,25,IVORY,true);
        String[] lines={"Start at your ship with two hearts. Gather meat and press Q to eat it.","Walk over loose loot. Press F beside trees or rocks to uncover hidden supplies.","Defeat the required enemies and monsters. Gather your level's recipe materials.","Press C or open I to craft: pickaxe in level 1, sword in level 2, gun in level 3.","A pickaxe mines diamond-bearing rocks. Diamonds go into your carried inventory.","They count ONLY after you return alive to the ship and press F to deposit them.","Press J to inspect every level's achievements. Finished? Stay or choose Next level.","Level 3 has ten fast monsters and hostiles with doubled health and stamina.","WASD move | E attack | Shift dash | 1-4 equip | Tab map | P pause | F5/F9 save/load"};
        for(int i=0;i<lines.length;i++)text(g,lines[i],164,195+i*43,i==8?11:14,i==5?GOLD:MUTED,false);
    }
    private void pause(Graphics2D g,ViewState v) {
        scrim(g);panel(g,304,170,512,390);text(g,"SURVIVAL PAUSED",344,218,12,TEAL,true);text(g,"Your ship is waiting.",344,263,29,IVORY,true);
        button(g,new Rectangle(344,310,432,48),"RESUME",true,v);button(g,new Rectangle(344,374,208,44),"SAVE / F5",false,v);button(g,new Rectangle(568,374,208,44),"LOAD / F9",false,v);
        button(g,new Rectangle(344,436,208,44),"MAIN MENU",false,v);button(g,new Rectangle(568,436,208,44),"QUIT GAME",false,v);text(g,"I INVENTORY   J ACHIEVEMENTS   H GUIDE",344,525,11,MUTED,false);
    }
    private void outcome(Graphics2D g,GameSession game,ViewState v) {
        scrim(g);panel(g,234,155,652,410);CampaignProgress c=game.getCampaign();
        if(game.getState()==GameState.LEVEL_COMPLETE){text(g,"LEVEL "+c.getLevel()+" COMPLETE",274,209,13,TEAL,true);text(g,"Your cargo is safely home.",274,258,30,IVORY,true);text(g,"All achievements complete. You choose what comes next.",274,301,15,MUTED,false);text(g,c.getBankedDiamonds()+" diamonds secured this level. Total: "+c.getTotalBankedDiamonds(),274,337,15,GOLD,true);
            button(g,new Rectangle(274,392,572,48),"STAY AND EXPLORE",false,v);button(g,new Rectangle(274,458,572,48),c.getLevel()==3?"COMPLETE MISSION & RETURN HOME":"PASS TO LEVEL "+(c.getLevel()+1),true,v);
        }else if(game.getState()==GameState.WON){text(g,"CONGRATULATIONS",274,214,30,GOLD,true);text(g,"You will return back to the planet",274,266,23,IVORY,true);text(g,"and help your people. You are so brave.",274,303,23,IVORY,true);text(g,"All three levels completed. "+c.getTotalBankedDiamonds()+" diamonds safely delivered.",274,355,15,TEAL,false);button(g,new Rectangle(274,427,572,48),"STAY IN THE OPEN WORLD",true,v);button(g,new Rectangle(274,493,572,40),"MAIN MENU",false,v);
        }else{text(g,"YOU DID NOT REACH THE SHIP",274,214,23,GOLD,true);text(g,"Carried cargo was lost. Banked diamonds are safe.",274,264,16,IVORY,false);text(g,"Your completed achievements and crafted equipment remain.",274,303,14,MUTED,false);button(g,new Rectangle(274,392,572,48),"RESPAWN AT THE SHIP WITH TWO HEARTS",true,v);button(g,new Rectangle(274,458,572,48),"LOAD LAST SAVE",false,v);}
    }
    private void map(Graphics2D g,GameSession game){text(g,"OPEN WORLD / SHIP RADAR",164,130,22,IVORY,true);minimap(g,game,340,175,9);text(g,"GOLD = CHAMPION   IVORY = SHIP   CORAL = HOSTILE",300,608,11,MUTED,false);}
    private void minimap(Graphics2D g,GameSession game,int x,int y,int cell){for(int r=0;r<44;r++)for(int c=0;c<44;c++){g.setColor(game.getChunk().isSolid(c+78,r+78)?new Color(25,55,65):new Color(48,89,88));g.fillRect(x+c*cell,y+r*cell,cell,cell);}for(Enemy e:game.getEnemies())if(e.isAlive())dot(g,x,y,cell,e.getWorldX(),e.getWorldY(),new Color(240,130,120));dot(g,x,y,cell,GameSession.SHIP_X,GameSession.SHIP_Y,IVORY);dot(g,x,y,cell,game.getPlayer().getWorldX(),game.getPlayer().getWorldY(),GOLD);}
    private void dot(Graphics2D g,int x,int y,int cell,int wx,int wy,Color color){int px=Math.max(0,Math.min(43*cell,(wx/48-78)*cell)),py=Math.max(0,Math.min(43*cell,(wy/48-78)*cell));g.setColor(color);g.fillOval(x+px-3,y+py-3,6,6);}
    public Action actionAt(int x,int y,GameState state,ViewState v,boolean busy){
        if(busy)return Action.NONE;Overlay o=v.overlay();if(o!=Overlay.NONE){if(hit(x,y,888,97,72,35))return Action.CLOSE;
            if(o==Overlay.CHAMPION)for(int i=0;i<3;i++)if(hit(x,y,186+i*266,462,200,44))return new Action[]{Action.ASTRA,Action.NYX,Action.ORION}[i];
            if(o==Overlay.DIFFICULTY)for(int i=0;i<3;i++)if(hit(x,y,186+i*266,447,200,44))return new Action[]{Action.EASY,Action.MEDIUM,Action.HARD}[i];
            if(o==Overlay.INVENTORY){if(hit(x,y,164,530,308,48))return Action.CRAFT;if(hit(x,y,492,530,252,48))return Action.EAT;for(int i=0;i<4;i++)if(hit(x,y,164+i*200,333,182,44))return new Action[]{Action.FISTS,Action.PICKAXE,Action.SWORD,Action.GUN}[i];}
            if(o==Overlay.ACHIEVEMENTS)for(int i=0;i<3;i++)if(hit(x,y,164+i*220,157,200,40))return new Action[]{Action.LEVEL1,Action.LEVEL2,Action.LEVEL3}[i];return Action.NONE;}
        if(state==GameState.INTRO){if(hit(x,y,72,402,246,52))return Action.PLAY;if(hit(x,y,334,402,200,52))return Action.LOAD;if(hit(x,y,72,470,246,44))return Action.CHAMPION_MENU;if(hit(x,y,334,470,200,44))return Action.DIFFICULTY_MENU;if(hit(x,y,72,530,145,44))return Action.SAVE;if(hit(x,y,231,530,145,44))return Action.GUIDE;if(hit(x,y,390,530,144,44))return Action.QUIT;}
        if(state==GameState.PAUSED){if(hit(x,y,344,310,432,48))return Action.RESUME;if(hit(x,y,344,374,208,44))return Action.SAVE;if(hit(x,y,568,374,208,44))return Action.LOAD;if(hit(x,y,344,436,208,44))return Action.MENU;if(hit(x,y,568,436,208,44))return Action.QUIT;}
        if(state==GameState.LEVEL_COMPLETE){if(hit(x,y,274,392,572,48))return Action.STAY;if(hit(x,y,274,458,572,48))return Action.ADVANCE;}
        if(state==GameState.WON){if(hit(x,y,274,427,572,48))return Action.STAY;if(hit(x,y,274,493,572,40))return Action.MENU;}
        if(state==GameState.LOST){if(hit(x,y,274,392,572,48))return Action.RESTART;if(hit(x,y,274,458,572,48))return Action.LOAD;}return Action.NONE;
    }
    private static boolean hit(int x,int y,int bx,int by,int w,int h){return new Rectangle(bx,by,w,h).contains(x,y);}
    public static String icon(Resource r){return r.name().toLowerCase()+".png";}
    public static String recipe(LevelDefinition d){StringBuilder s=new StringBuilder();for(Resource r:Resource.values())if(d.recipe().containsKey(r)){if(!s.isEmpty())s.append(" + ");s.append(d.recipe().get(r)).append(' ').append(r.displayName());}return s.toString();}
    private void button(Graphics2D g,Rectangle b,String label,boolean primary,ViewState v){boolean hover=b.contains(v.mouseX(),v.mouseY());g.setColor(primary?hover?new Color(255,221,164):GOLD:hover?new Color(30,64,73):INK);g.fillRoundRect(b.x,b.y,b.width,b.height,9,9);g.setColor(new Color(110,155,158,100));g.drawRoundRect(b.x,b.y,b.width,b.height,9,9);g.setFont(new Font(Font.DIALOG,Font.BOLD,11));g.setColor(primary?new Color(24,37,42):IVORY);g.drawString(label,b.x+(b.width-g.getFontMetrics().stringWidth(label))/2,b.y+b.height/2+4);}
    private void panel(Graphics2D g,int x,int y,int w,int h){g.setColor(INK);g.fillRoundRect(x,y,w,h,17,17);g.setColor(new Color(100,148,155,80));g.setStroke(new BasicStroke(1));g.drawRoundRect(x,y,w,h,17,17);}
    private void scrim(Graphics2D g){g.setColor(new Color(3,13,23,190));g.fillRect(0,0,1120,720);}
    private void toast(Graphics2D g,String message){g.setFont(new Font(Font.DIALOG,Font.BOLD,12));while(message.length()>3&&g.getFontMetrics().stringWidth(message)>690)message=message.substring(0,message.length()-4)+"...";int w=g.getFontMetrics().stringWidth(message)+40;panel(g,(1120-w)/2,572,w,42);text(g,message,(1120-w)/2+20,598,12,GOLD,true);}
    private void text(Graphics2D g,String s,int x,int y,int size,Color color,boolean bold){g.setFont(new Font(Font.DIALOG,bold?Font.BOLD:Font.PLAIN,size));g.setColor(color);g.drawString(s,x,y);}
}
