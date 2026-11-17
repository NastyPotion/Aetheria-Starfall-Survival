# Aetheria: Starfall Survival - five-minute demonstration

## Prepare

- Complete student identities and confirm group/proposal acknowledgment requirements. Source files do not establish instructor approval or an assessment outcome.
- Run `test.ps1`, then `package.ps1`. Launch the JAR from another working directory and check the title image, original champion visuals, resources and panels.
- Rehearse a full three-level campaign and the difference between carried and secured diamonds. Check speaker output on the examination computer; headless tests cannot verify it.
- Open the report's class/interaction diagrams and relevant source. Optional: keep an identified rehearsal v3 checkpoint near level-three completion. Copy it aside before the demonstration's save replaces the single slot; describe prepared progress honestly.

## Five-minute walkthrough

| Time | Action | What it demonstrates |
| --- | --- | --- |
| 0:00-0:40 | Show Start/Continue/Save/Quit, choose a champion and difficulty, begin with two hearts. | One Champion configured by ChampionType; difficulty data changes hostiles rather than recipes. |
| 0:40-1:20 | Gather food, eat with Q, show F harvesting behind a tree and I inventory. | Explorer/world interaction, private resource counts and the distinction between food and recipe ingredients. |
| 1:20-2:10 | Show a fight, J achievements and C crafting after the checklist. | Damageable combat, enemy/monster kill categories, reusable behavior and the same LevelDefinition feeding UI and rules. |
| 2:10-2:55 | Mine a diamond, return to the ship and F deposit; choose Stay. | Loose diamonds are at risk, ShipDock/Interactable banks them, readiness offers an explicit choice without auto-advancing. |
| 2:55-3:25 | Save, move, load; show paused restoration. Use an identified prepared checkpoint if demonstrating the gun/final return. | Immutable v3 snapshot, background storage, per-enemy stats, and explicit level-three completion. |
| 3:25-4:40 | Walk through the eleven source examples below and the class diagram. | Explain contracts and ownership before runtime dispatch; distinguish inheritance from composition. |
| 4:40-5:00 | Show latest test output and an invalid/old-save test; point to user guide and PDF delivery. | Checked failure with no partial state change, reproducible packaging and honest remaining administrative steps. |

This short presentation samples a larger campaign. Rehearsal and the committed progression tests establish the complete sequence; a prepared checkpoint should be identified as prepared.

## Eleven assessed concepts

| Concept | Source to explain |
| --- | --- |
| Modularity | Core, entities, survival, world and persistence packages; GameEvent feedback boundary |
| Hierarchy | Entity/Champion; Enemy/MeleeEnemy; Projectile as a spatial actor |
| Composition | Session owns CampaignProgress/ShipDock/world/actors; progress owns Inventory |
| Reuse | Shared enemy pursuit/stamina/melee; one LevelDefinition reused by gates and UI |
| Encapsulation | Inventory.spend is all-or-nothing; private carried/banked counters |
| Subtyping | Champion.strike(Damageable); ShipDock used as Interactable |
| Information hiding | Private tile grid and immutable snapshot copies of arrays/maps/sets |
| Abstraction | Abstract actor operations and combat/interaction/storage interfaces |
| Inheritance | Shared entity implementation and flying/melee enemy variations |
| Polymorphism | Heterogeneous enemy updates and injected SaveRepository implementation |
| Exception handling | SaveGameException, strict v3 validation, stream cleanup and UI recovery |

## Questions to rehearse

Explain why gathering achievements survive death while recipe materials do not; why meat is not consumed by the pickaxe recipe; why carried diamonds cannot finish a level; why a ship has a progress reference rather than inheriting from the ledger; why level-three stats double the rounded level-two values; and why old relay saves are rejected explicitly instead of silently losing their progress.

Show that the level-complete screen's Stay button does not advance, and that the player can later reopen the choice at the ship. Explain transient versus saved state and why file/audio workers never mutate live entities. Point to actual final test results without promising a grade or claiming audio-device playback that was not checked.
