# Aetheria: Starfall Survival - player guide

## Start playing

Follow the [README](../README.md) to download and run the game with Java 21. To build from source on Windows, install a Java 21 JDK and run `.\run.ps1` from the project folder.

Choose **CHAMPION** and **DIFFICULTY**, then **START GAME**. **CONTINUE SAVE** restores your saved campaign and its character/difficulty choices. The menu also offers Save, Guide and Quit.

| Champion | Movement and dash energy |
| --- | --- |
| Astra | Balanced movement, 100 energy |
| Nyx | Faster movement, 120 energy |
| Orion | Steady movement, 110 energy |

Everyone starts beside the ship with **two filled hearts (4 health)**, fists and an empty inventory. Food can heal you up to six hearts (12 health).

Medium adds two hostiles, 25% health/stamina and one speed point. Hard adds four hostiles, 50% health/stamina and two speed points. Recipes and achievement targets stay the same. Level-three health and stamina double the corresponding rounded level-two values at the same difficulty.

## Complete three levels

Open **J** for the checklist. Each level requires its kills, gathering, crafted weapon and **diamonds deposited at the ship**.

| Level | Enemy kills | Monster kills | Gather this level | Craft and consume | Diamonds banked this level |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | 2 | 1 meat, 2 metal, 2 stone | Pickaxe: 2 metal + 2 stone | 1 |
| 2 | 2 | 2 | 3 metal, 3 stone | Sword: 3 metal + 3 stone | 4 |
| 3 | 3 | 10 fast monsters | 4 metal, 4 stone, 4 rock | Gun: 4 metal + 4 stone + 4 rock | 10 |

Meat is food and a level-one gathering achievement; the pickaxe recipe does not consume it. Eating it still leaves the achievement completed. **Stone and rock are separate resources.**

1. Walk over loose items to collect them. Press **Q** to eat carried meat when injured; one restores two hearts.
2. Fight hostiles and search nearby trees with **F** for food, metal and stone. Defeated hostiles drop meat.
3. Open crafting with **C** or inventory with **I**. Complete the kill/gather checklist and carry the recipe materials, then press C again or click **CRAFT**. The recipe spends those materials and grants permanent equipment.
4. After crafting the pickaxe, use F near rocks to mine rock and diamonds.
5. Return alive and press F near the ship. Once preparation/crafting is complete, this deposits **all** carried diamonds into secured progress. Loose diamonds do not count toward completion.
6. Choose **STAY AND EXPLORE** or **PASS TO LEVEL 2/3**. Staying does not advance automatically; press **N** or F at the ship to reopen the choice later.

After level three, choose **COMPLETE MISSION & RETURN HOME** to finish, or keep exploring. The HUD shows carried resources, next steps and a ship radar; Tab expands the map.

## Controls

| Input | Action |
| --- | --- |
| WASD / arrows | Move and face a direction |
| E / Space | Attack |
| Shift | Dash in the movement direction or current facing |
| F | Gather nearby resources or deposit at the ship |
| I | Inventory and equipment |
| C | Open crafting; craft when inventory is already open |
| J | Achievements, with tabs for all three levels |
| Q | Eat one carried meat when injured |
| 1 / 2 / 3 / 4 | Equip owned fists / pickaxe / sword / gun |
| N | Reopen the level choice when ready at the ship |
| Tab / H | Map / guide |
| M | Mute/unmute |
| P / Escape | Pause/resume or close a panel |
| F5 / F9 | Save / load |
| Enter | Begin/resume; respawn after defeat; stay after level completion |
| R | Respawn after defeat |

Losing window focus pauses play and clears held keys. Inventory, map, guide and achievement panels pause the world while open.

## Survive and recover

Face a hostile before attacking. Fists are always available; the pickaxe and sword improve melee damage/reach, and the gun fires directional pulses. Each enemy takes damage at most once per melee swing. Hits briefly stagger and knock back opponents.

Mossling and Riftwing count as monsters; Briar Sentinel and Eclipse Warden count as enemies. Riftwing flies over obstacles. Watch melee wind-ups and exploit the rest periods when hostiles exhaust their stamina. Warden attacks can accelerate at low health.

Dash costs 35 energy, respects obstacles and briefly protects you from damage. Energy regenerates. Release Shift before dashing again, and finish a melee swing before starting a dash.

Medpods, batteryfruit, starblooms and surge orbs restore 2, 3, 4 and full health respectively. Avoid poison crystals.

**Death loses all carried resources**, including food and loose diamonds. Crafted equipment, kill/gather achievements, banked diamonds, time and score remain. Respawn beside the ship with two hearts. If you had not crafted yet, gather the lost recipe materials again even though the gathering achievements remain complete.

Advancing keeps carried inventory, equipment, cumulative bank, time and score. The new level starts a fresh kill/gather checklist, recipe and secured-diamond count. Existing weapons remain usable. Staying allows further exploration and occasional roaming encounters.

## Save and load

Use F5 to save and F9 to load the single slot, normally:

```text
%USERPROFILE%\.aetheria\save.properties
```

The folder is based on Java's user-home directory. Saving replaces the file; copy it elsewhere first to keep another checkpoint. Quit and closing the window do not save automatically.

Saves retain your champion/difficulty, equipment, inventory, achievements, secured diamonds, time/score, terrain/items and hostile health/stamina/statistics. Loaded campaigns start paused. Held keys, projectiles, attack/dash timers, particles and sound playback are not restored; dash energy returns to maximum.

The menu can save a selected new-game setup or a campaign returned to the menu. Saving leaves it paused; Enter resumes it. After death, respawn or load before saving. Wait for the result message before continuing. A failed load leaves your current campaign available.

Current saves use **v3**. Older v1/v2 relay-campaign saves are incompatible; loading one explains the problem and leaves its file unchanged.

## Quick fixes

- **Cannot craft:** complete the checklist and carry every recipe material. Press C inside inventory or click Craft.
- **Cannot mine:** craft the pickaxe first, stand near the deposit and press F.
- **Diamonds do not finish the level:** deposit them at the ship after crafting.
- **No advancement after Stay:** return to the ship and press N or F, then choose advancement. Enter chooses staying.
- **Controls do not respond:** click the window and resume play.
- **Save/load fails:** read the message and check disk space, write access and save version.
- **No sound:** check M and your audio output. The game continues silently if an audio device is unavailable.
