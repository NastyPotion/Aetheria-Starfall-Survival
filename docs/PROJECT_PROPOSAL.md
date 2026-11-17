# Aetheria: Starfall Survival - project proposal

**Course:** Object Oriented Programming  
**Implementation:** Java 21 standard-library desktop game  
**Student names and registration numbers:** complete before submission  
**Group arrangement:** confirm with the course requirements  
**Proposal status:** draft; instructor acknowledgment is not established by the supplied archive.

## Idea and scope

A stranded explorer survives three crafting expeditions before returning home. The player selects Astra, Nyx or Orion and Easy, Medium or Hard difficulty, begins beside a ship with two hearts, fights enemies and monsters, gathers materials/food and crafts a pickaxe, sword and gun. Diamonds in the inventory remain at risk; only diamonds carried home alive and deposited at the ship count toward the level.

Death loses loose materials but preserves achievements, banked diamonds and equipment. After a completed level the player explicitly chooses to stay and explore or advance. Level three offers completion and return home. Exact approved quantities are recorded in [SURVIVAL_REQUIREMENTS.md](SURVIVAL_REQUIREMENTS.md).

This is one local campaign and save slot, with no database, web integration, external service or third-party game engine. The main menu supports saving both a selected new-game setup and a retained campaign; saving pauses that mission so Enter can resume it. The scope keeps the object interactions and state transitions suitable for implementation and oral discussion.

## Objects and direct interactions

`Entity` supplies private position, bounds and update/draw operations. One concrete `Champion` implements player behavior; `ChampionType` supplies the three choices as configuration data. `Enemy` and `MeleeEnemy` share autonomous health/stamina/movement/attack behavior. Monsters may fly or use faster level-three configurations. `Projectile` is a short-lived spatial entity for the crafted gun. `Damageable` is the common combat capability.

`Chunk` owns collision, resource-bearing obstacles and loose items. `Inventory` owns carried quantities and permanent equipment. `LevelDefinition` supplies immutable requirements used by the UI and model. `CampaignProgress` tracks achievements, crafting, banking and transitions across lives. `ShipDock implements Interactable` to deposit diamonds through that ledger. `GameSession` composes and coordinates these objects; feedback and storage are separate services.

## Assessed OOP concepts

| Concept | Implementation evidence |
| --- | --- |
| Modularity | Core, entities, survival, world, UI, art/assets, effects/audio and persistence packages |
| Hierarchy | Entity/Champion; Entity/Enemy/MeleeEnemy; Entity/Projectile |
| Composition | Session owns world, player, encounters, ledger and ship; ledger owns inventory |
| Reuse | Shared combat/movement/stamina, data-driven recipes/difficulty and art helpers |
| Encapsulation | Private inventory/achievements, atomic spending, controlled banking and death loss |
| Subtyping | Concrete opponents as Enemy and Damageable; ship as Interactable |
| Information hiding | Every declared instance field private; controlled operations and defensive copies |
| Abstraction | Abstract actor contracts and combat, interaction and campaign storage interfaces |
| Inheritance | Varied hostiles reuse parent implementation for position, movement and combat |
| Polymorphism | Heterogeneous encounter updates, contract-based strikes/devices and injected storage |
| Exception handling | Checked SaveGameException, validated v3 state, specific resource/audio recovery |

## Technology and original resources

Runtime dependencies are the Java 21 standard library: Swing, AWT/Java2D, ImageIO, JavaSound, collections, NIO/Properties and concurrency utilities. No package downloads, server or account are required. PowerShell build scripts and optional document-export tools prepare artifacts; no Python or JavaScript runtime is required to play.

Character, equipment, item, shuttle and terrain visuals are original procedural Java2D drawings. One included AI-generated title background is disclosed with its exact generation prompt in [ART_DIRECTION.md](ART_DIRECTION.md). Audio cues are synthesized in Java rather than obtained from an external pack. Compatibility class/resource IDs remain internal names, not permission to restore borrowed imagery.

## Delivery and remaining course steps

Source, resources, Eclipse configuration, build/run/test/package scripts, tests, executable JAR, report, user guide and matching printable PDFs are included. The report has one separate source-backed section for each assessed concept and diagrams of class structure and interactions. See the [demonstration checklist](DEMO_CHECKLIST.md) for a short oral walkthrough.

Before submission, the students must complete identifying details, confirm group arrangements and obtain the mandatory instructor acknowledgment. Neither this document nor source code establishes those facts or sends a proposal to the professor.

The assessment basis is `OOP-0-intro.it.en.pdf`, slides 8-12, in the supplied `oop-syllabus.zip`: project/report/code/oral process, Java and eleven concepts, direct entity interactions, proposal/library/build/user-guide delivery, and the report's title/index/motivation/diagrams/code-example structure.
