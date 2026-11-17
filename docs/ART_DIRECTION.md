# Aetheria: Starfall Survival - art direction and provenance

## Visual identity

The survival expedition combines pale explorer equipment, deep ink terrain, turquoise life and warm material/objective accents. Readable silhouettes distinguish Astra, Nyx, Orion and the hostile families. Pixel edges and shared highlights connect the characters, crafting materials, devices, terrain and interface.

| Shared palette role | Representative hex |
| --- | --- |
| Ink outline | `#0B1427` |
| Ivory material | `#F2F0D9` |
| Pale signal | `#CCFFF2` |
| Teal | `#51DDB7` |
| Deep teal | `#194E57` |
| Amber | `#FFBE68` |
| Ember | `#E87548` |
| Lilac | `#C1A4F5` |

These values come from `PixelArt`; the UI and feedback use related shades for legibility.

## Original code-generated gameplay art

`OriginalArt`, `TerrainArt` and `PixelArt` generate actor, directional attack, equipment, material/food, heart, shuttle and terrain images with Java2D. The three champions have distinct original appearances and displayed names. Melee weapons and the gun have appropriate equipment imagery, while gun pulses are drawn by their projectile implementation.

Resource-bearing trees and rocks communicate places to search. Metal, stone, rough rock, diamonds and meat have separate inventory icons. Stone and rock remain visually and logically distinct. Five named zones use four terrain styles with deterministic coordinate variation and curved region boundaries: Starfall Glade, Verdant Grove, Tidal Gardens, Ember Ruins and Eclipse Reach.

`SceneRenderer` adds ship/world feedback, hostile attack indications and atmosphere. `ParticleSystem` supplies bounded finite sparks, rings and floating labels. The top HUD, inventory/crafting panel, achievement tabs, ship radar and map are drawn in Java rather than embedded in a screenshot.

`Assets` first asks `OriginalArt.create(name)` and caches its result. Historical names such as `boy_down_1.png`, `orc_attack_right_2.png`, `icons8-minecraft-creeper-16.png` and `dragonfruit.png` are compatibility aliases for the new generated art. They do not load recognizable borrowed character images. Internal actor/enum IDs and the old Eclipse/distribution identifiers are also retained where useful; displayed identities and gameplay art are original to this project version.

## AI-generated title backdrop

`src/art/starfall-title.png` was generated with the built-in image-generation tool for the project. It is an **AI-generated title-screen backdrop**, separate from the procedural gameplay art. The exact submitted prompt is preserved verbatim in [TITLE_ART_PROMPT.txt](TITLE_ART_PROMPT.txt).

The prompt requests an original sci-fi/fantasy valley, an ivory/teal explorer with amber scarf, an abandoned shuttle and atmospheric negative space for interface text. It requests no letters, logos or UI in the artwork. Java draws the actual Starfall Survival title, controls and overlays on top. The prompt retains the earlier working title, Starfall Protocol; no title text is baked into the illustration, and the same backdrop suits the current survival campaign.

The image is bundled as a static PNG and decoded with ImageIO. Gameplay never contacts an image model or needs a network service. This disclosure distinguishes AI-assisted background art from code-generated gameplay visuals; it does not present the bitmap as a student's hand painting.

## Synthesized audio

`SoundEngine` generates short PCM event cues with JavaSound on a bounded single daemon worker. Pickups, combat, equipment crafting, dash, death and completion use the same immutable event boundary as particles. Some event identifiers retain compatibility names even though the former relay progression has been removed. There are no downloaded recordings or music packs.

Mute and unavailable-device fallback keep play functional without audio. Headless tests check silent fallback and event handling; actual speaker playback still needs a desktop audio check.

## Reproduction and delivery

A Java 21 build includes all procedural-art source and the title PNG. Generated art requires no external graphics service, image pack, browser or additional runtime language. Build/package scripts copy the included title resource into the JAR. The submission also preserves this provenance document and exact prompt. Document-export tools prepare printable PDFs and are not part of the executable game.