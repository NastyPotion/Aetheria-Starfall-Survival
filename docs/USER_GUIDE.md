# Aetheria: Starfall Survival - user guide

## Launch and choose a champion

Use a Java 21 JDK and a desktop display. From the project folder in PowerShell:

```powershell
.\run.ps1
```

To choose a JDK, add `-JavaHome 'C:\path\to\jdk-21'`. In Eclipse, import the existing project, select JavaSE-21, refresh/clean and run `com.game.core.Main`. The [README](../README.md) explains packaging and launching the executable JAR.

The 1120 x 720 title menu offers **START GAME**, **CONTINUE SAVE**, **CHAMPION**, **DIFFICULTY**, **SAVE GAME**, the guide and **QUIT GAME**. Champion and difficulty selections apply to a new campaign. Continue restores the selections recorded in a v3 save.

| Champion | Movement and energy |
| --- | --- |
| Astra, Vanguard | Balanced movement, 100 maximum dash energy |
| Nyx, Scout | Faster movement, 120 maximum dash energy |
| Orion, Engineer | Steady movement, 110 maximum dash energy |

All champions begin beside the ship with **two filled hearts (4 health points)**, no crafted equipment and an empty inventory. Food can restore health up to six hearts (12 points). Fists are always available. A character's name is displayed below its sprite.

The choices share one player class with different settings. This keeps the Java design simple while preserving all three characters and their original graphics. The separate [oral exam guide](ORAL_EXAM_GUIDE.md) explains the code and OOP examples.

Easy, Medium and Hard change hostile counts and combat statistics, not recipes. Medium adds two hostiles, increases health/stamina by 25% and speed by one. Hard adds four, increases health/stamina by 50% and speed by two. Level-three health and stamina are exactly double the corresponding level-two values at the same difficulty, including rounding.

## Three levels

Your checklist appears in the achievement panel (**J**). Every level requires its kills, gathering achievements, crafted equipment and **diamonds deposited at the ship**.

| Level | Enemy kills | Monster kills | Gather this level | Recipe consumed | Diamonds secured this level |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | 2 | 1 meat, 2 metal, 2 stone | Pickaxe: 2 metal + 2 stone | 1 |
| 2 | 2 | 2 | 3 metal, 3 stone | Sword: 3 metal + 3 stone | 4 |
| 3 | 3 | 10 fast monsters | 4 metal, 4 stone, 4 rock | Gun: 4 metal + 4 stone + 4 rock | 10 |

**Meat is food, not a pickaxe ingredient.** Collecting it completes the level-one food achievement even if you eat it before crafting. **Stone and rock are different resources.** The diamond figure is a current-level target; the HUD also reports the cumulative bank across the campaign.

## First expedition

1. Find food near the ship before taking long fights. Loose items are collected by walking over them. Meat enters your inventory; press **Q** to eat one and restore two hearts when injured.
2. Fight the required enemies and monsters. Hostiles drop meat when defeated. The objective marker points toward remaining tasks.
3. Search behind trees for food, metal and stone. Stand beside a resource-bearing tree and press **F**; its contents enter the inventory.
4. Open crafting with **C** or inventory with **I**. Once the kill/gather checklist and carried recipe quantities are complete, press C again or click **CRAFT**. Crafting spends the recipe and grants permanent equipment.
5. Own a crafted pickaxe to mine rough rock and diamond deposits with **F**. The pickaxe can also be equipped with **2**.
6. Bring diamonds back alive. Press **F** near the ship to transfer every carried diamond into secured progress after preparation/crafting is complete.
7. When the level is ready, choose **STAY AND EXPLORE** or **PASS TO LEVEL 2/3**. Staying never advances automatically. Press **N** or F at the ship to reopen the choice later.

At level three, choose **COMPLETE MISSION & RETURN HOME** to finish. The concluding message is:

> Congratulations, you will return back to the planet and help your people. You are so brave.

You can also stay in the open world. Returning home is an explicit choice rather than an automatic transition when a counter reaches its target.

## Controls

| Input | Action |
| --- | --- |
| WASD / arrows | Move and face a direction |
| E / Space | Attack with the equipped weapon |
| Shift | Dash in the movement direction or current facing |
| F | Search/harvest nearby trees or rocks; interact/deposit at ship |
| I | Open/close inventory and equipment |
| C | Open crafting; attempt recipe when the inventory panel is already open |
| J | Achievements, with level 1/2/3 tabs |
| Q | Eat one carried meat when health is below maximum |
| 1 / 2 / 3 / 4 | Equip owned fists / pickaxe / sword / gun |
| N | Request the stay/advance choice when ready at the ship |
| Tab | Open/close map |
| H | Open/close guide |
| M | Mute/unmute sound |
| P / Escape | Pause/resume or close a panel |
| F5 / F9 | Save / load campaign |
| Enter | Begin/resume; respawn after defeat; stay after level completion |
| R | Respawn after defeat |

Click the window if controls do not respond. Losing focus clears held keys and pauses play. Inventory, guide, map and achievement panels pause the world while open; closing a panel normally resumes the play that opened it.

## Combat and survival

Fists are weak but always available. The pickaxe improves close attacks; the sword has greater damage and reach. The gun fires directional, finite-lived projectiles. Equip the appropriate owned item with 1-4 or the inventory buttons. Uncrafted equipment remains locked.

Face a hostile before attacking. Melee weapons have a wind-up and each target can take damage at most once per swing. A hit flashes and briefly staggers/knocks back the opponent. Mossling and Riftwing count as monsters; the Briar Sentinel and Eclipse Warden count as enemies. Riftwing flies over terrain that blocks ground actors. Melee enemies show their attack wind-up; some Warden animations accelerate at low health.

Hostiles have health and stamina. Active pursuit drains stamina; an exhausted opponent rests before recovering. Later levels and difficulty selection change their statistics, and level three includes fast monsters. Use distance, their wind-ups and stamina recovery to find openings.

Dash costs 35 energy, lasts briefly and protects you from damage while active. It respects obstacles and world boundaries. Energy regenerates. Release Shift before requesting another dash; holding it does not repeat the move. A current melee swing prevents starting a dash.

The HUD shows hearts/dash energy, carried resources along the top, next steps in the upper middle, secured diamonds and a ship radar. Use Tab to expand navigation. Mission time measures active simulation ticks, not wall-clock time.

## Food, resources and death

| Item | Use |
| --- | --- |
| Meat | Carried food; Q consumes one to restore four health points |
| Metal | Crafting material from scrap-bearing trees or loose finds |
| Stone | Crafting material, distinct from rough rock |
| Rock | Mined rough rock needed for the gun |
| Diamond | Cargo; only deposited diamonds count toward completion |
| Medpod / batteryfruit / starbloom / surge orb | Immediate health recovery: 2 / 3 / 4 / full health |
| Poison crystal | Harmful collectible; avoid it |

At zero health, the current life ends. Death clears **all carried resources**, including loose diamonds, food, metal, stone and rock. It preserves crafted equipment, kill/gather achievements, secured diamonds, campaign time and score. Respawn begins beside the ship with two hearts. If a recipe is not yet crafted, retained gathering achievements do not replace the actual materials you lost; gather the recipe quantities again.

Advancing keeps carried inventory, owned equipment, cumulative bank, time and score. It resets the new level's kill/gather achievements, crafted flag and current-level bank. Equipment already owned remains usable while preparing the next recipe. Remaining in a level permits further exploration and occasional roaming encounters.

## Save and load

F5 saves the current living campaign and F9 loads the single slot:

```text
%USERPROFILE%\.aetheria\save.properties
```

The base is Java's user-home directory. Saving replaces that file. Copy it elsewhere first if you want to preserve another checkpoint. The menu's Save action can save the selected new-game setup before starting, or the current campaign after returning to the menu. Saving leaves that campaign paused; press Enter to resume it without starting over. After defeat, respawn or load before saving.

The v3 file includes the map and harvested/loose items, champion and equipped weapon, individually identified enemies with health/stamina/statistics, inventory/owned equipment, achievements, current and cumulative secured diamonds, time and score. Loaded campaigns start paused. Held input, projectiles, animation/attack/dash timers, particles and sound playback are temporary and not restored; dash energy returns to its champion's maximum.

File operations run in the background. Wait for their result message before continuing. Loading checks all data before applying it; a missing or damaged slot produces a message and leaves the current campaign available. Version 1 and 2 files are from the earlier relay design and cannot be loaded into this survival campaign. The explanatory error preserves the file and does not silently reinterpret its progress.

## Troubleshooting

- **Cannot craft:** finish the level's enemy/monster and gathering achievements, carry every recipe material, then press C inside inventory or click Craft. Crafting the same recipe twice is disabled.
- **Cannot mine rocks or diamonds:** craft the pickaxe first. Stand nearby and press F.
- **Diamonds do not complete the level:** return alive to the ship and press F. Loose inventory diamonds are not secured progress.
- **No next level after choosing Stay:** return to the ship and press N or F when ready; choose the advancement button. Enter chooses staying.
- **Recipe materials vanished after death:** loose resources are lost; achievements and completed equipment remain.
- **Dash does not start:** check energy/cooldown, finish an attack, release Shift and press it again.
- **Java not found or Eclipse uses old code:** choose JavaSE-21, refresh and clean, or pass `-JavaHome` to the script.
- **Saving/loading fails:** inspect the on-screen reason; verify write access, disk space and that the slot uses v3. Old v1/v2 files are deliberately incompatible.
- **Audio is silent:** check M and the operating system's output. Unavailable JavaSound devices disable cues without affecting gameplay. Actual speaker output needs a desktop check.

Save before quitting if you want the current progress restored later. Closing or Quit does not implicitly save it.
