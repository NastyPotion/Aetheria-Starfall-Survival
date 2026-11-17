# Aetheria oral examination guide

This guide is a study route for the simplified Java design. Use it beside the project report and actual source. Practice explaining each example in your own words and changing a small value safely; memorizing definitions alone does not demonstrate understanding.

## 1. Present the game in two minutes

"Aetheria is a Java survival game. The explorer starts beside a ship with two hearts. Each of three levels requires fighting, gathering and crafting. Diamonds only count after the living explorer returns to the ship and deposits them. The player can stay or advance after completion."

"The central model is GameSession. It coordinates a Champion, Enemy objects, a terrain Chunk, CampaignProgress and ShipDock. CampaignProgress owns Inventory and checks the level rules. UI displays those same rules."

"Entity is the abstract spatial parent. Champion is one concrete configurable class. Enemy has subclasses because their behavior differs: Bat can fly and MeleeEnemy shares timed attacks. Damageable is the combat capability. Interactable is the ship-use capability."

"The program uses Java 21 standard libraries. Swing owns live gameplay, file work transfers immutable snapshots in the background, and a bounded audio worker plays cues. The project report gives a separate source example for every assessed OOP concept."

Then demonstrate one short expedition: move, fight, gather, open inventory/achievements, craft, mine and bank a diamond. Explain the difference between carried inventory and secured progress before showing the level-choice screen.

## 2. Learn the object model first

Explain each object in one sentence before discussing implementation details:

| Object | One sentence |
| --- | --- |
| Entity | Holds private spatial state and requires update/draw behavior |
| Champion | Handles the controlled explorer's movement, combat, health and dash |
| Enemy | Shares hostile health, stamina, pursuit and rendering |
| MeleeEnemy | Reuses a timed wind-up and strike cycle |
| Chunk | Owns terrain, collision and harvesting |
| Inventory | Owns loose resources and permanent equipment |
| CampaignProgress | Checks achievements, crafting, banking and progression |
| LevelDefinition | Supplies immutable targets and recipes |
| ShipDock | Allows a nearby living explorer to secure diamonds |
| GameSession | Coordinates those model objects and mission state |
| GamePanel | Converts desktop input into actions and draws the current screen |
| GameSnapshot | Holds validated copied values for saving/loading |

Draw three simple relationships on paper: `Bat is an Enemy`; `CampaignProgress owns an Inventory`; `ShipDock references the campaign`. The first is inheritance, the second composition and the third association. Do not label every object field as composition.

ChampionType configures Astra, Nyx and Orion using names, sprite prefixes, speeds and energy limits. It avoids three classes that would only select data. The enemy subclasses remain because their overridden behavior matters during play.

## 3. Explain all eleven concepts

**Modularity:** each package has a responsibility. LevelDefinition contains recipes; FileSaveRepository contains file keys. UI asks the campaign about progress rather than implementing another set of rules.

**Hierarchy:** Entity is general; Champion, Enemy and Projectile are specialized actors. MeleeEnemy is a narrower enemy family. A hierarchy organizes types from general to specific.

**Composition:** the session owns its model objects; the campaign creates/owns its inventory. Objects cooperate instead of inheriting unrelated jobs from one large class.

**Reuse:** MeleeEnemy supplies one attack algorithm to Orc and Skeleton. Bat reuses Enemy movement while overriding flight. Reuse saves copying an implementation.

**Encapsulation:** Inventory protects counts with add/remove/spend. A failed spend consumes nothing; counts cannot become negative. Describe the invariant as well as the private fields.

**Subtyping:** an Enemy can be used as Entity and Damageable. GameSession's Enemy variable and Damageable alias refer to the same target. Compatible references do not create another object.

**Information hiding:** private fields conceal representation. Class/instance and constant/variable are separate decisions. `private static final DASH_COST` is a class constant; `private final maxEnergy` is fixed per instance; `private currentHealth` changes per instance. final does not freeze an object's contents.

**Abstraction:** Damageable says what combat requires without revealing health storage. Entity declares update/draw without specifying each actor's algorithm. An abstraction is a useful contract with omitted details.

**Inheritance:** Bat extends Enemy, reuses behavior and overrides flight. super initializes the parent's part of the same object. Constructors are not inherited, and private fields are not directly accessible to the subclass.

**Polymorphism:** the session calls update on Enemy references. Runtime type determines overridden behavior. A melee enemy uses MeleeEnemy.update; Bat's movement uses its flight override. Contrast runtime overriding with compile-time overloading.

**Exception handling:** storage throws checked SaveGameException. The panel displays an error and keeps the current mission. Invalid arguments throw IllegalArgumentException; ordinary unavailable crafting returns false.

## 4. Walk through five small pieces of code

### Inventory.remove

Open src/com/game/survival/Inventory.java. Explain the positive-amount validation, reading the previous count, refusing insufficient funds, then updating the map. Explain why spend first checks the complete recipe: failure must not partly consume ingredients.

Then show add. The count is widened to long before adding. The upper-bound check makes the later conversion to int safe. Distinguish this numeric conversion from assigning an Enemy to Damageable, which uses the same object through a compatible reference.

### Bat and Enemy

Open src/com/game/entities/Bat.java. Explain extends, super, the constructor arguments and the canFly override. Follow Enemy's movement to the place that calls canFly. The override changes actual collision behavior; it is not an unused example.

Explain why Entity's coordinates are private. The subclass asks for coordinates and requests valid position changes through methods. Collision dimensions are private, and getBounds returns a fresh rectangle rather than internal mutable geometry.

### The same enemy through two types

Open GameSession.updateEnemies. The loop holds Enemy. `Damageable target = enemy` introduces another reference to that same object. `player.strike(target)` invokes the combat contract. Open Champion.strike to show the call to takeDamage.

The Enemy variable permits enemy-specific observations, while Damageable permits only the combat contract. No downcast is needed. Static type controls available operations; runtime type selects overridden instance methods.

### Crafting and banking

Open CampaignProgress.craft, isReady and depositDiamonds, then ShipDock.interact. Crafting checks achievements and actual ingredients. Banking moves carried diamonds into the ledger only after preparation, and the ship checks a living explorer's range. Completion reads banked diamonds, so merely carrying cargo cannot finish the level.

### Save/load failure

Open GamePanel's file-transfer method and FileSaveRepository.load. First the live session produces copied snapshot values. Background work reads/writes those values. Validation finishes before Swing installs the loaded session. An invalid file produces a message; it cannot partly overwrite the current mission.

## 5. Know the supporting files without learning every drawing coordinate

Main and GameWindow are the entry point/window. GameConfig holds shared view/tile constants. KeyHandler stores held controls and one-use commands; focus loss clears them. GameState describes the title/play/pause/outcome states. GameEvent carries feedback values from the model to particles and sound.

EncounterFactory chooses a concrete enemy and applies level/difficulty statistics. EnemyType names supported hostile kinds; Direction names facing. Projectile moves a gun pulse until it hits or expires. Resource and WeaponType name inventory choices; Difficulty supplies hostile multipliers/counts. TileType and Biome describe terrain and regions.

GamePanel orchestrates terrain and actor drawing. UI draws buttons, panels and progress from the model. SceneRenderer adds the ship, attack warnings and atmosphere. Assets caches images. OriginalArt, TerrainArt and PixelArt contain drawing operations. ArtGallery is an optional preparation tool outside the playable source. ParticleSystem provides bounded temporary effects. SoundEngine synthesizes cues on a bounded worker. These classes support presentation without deciding campaign completion.

GameSnapshot contains saved data and validation/copies. SaveRepository is the small storage contract, FileSaveRepository implements the file format, and SaveGameException represents recoverable storage failure. Tests use that contract to simulate errors without relying on an accidentally damaged real save.

Build scripts compile Java and package resources; documentation scripts export PDFs. Neither Python nor JavaScript is required to play. Be able to explain a drawing method's purpose and how it is called; drawing coordinates are data, whereas ownership, dispatch and validation are the main design mechanisms.

## 6. Practice likely questions

**Why use an abstract Entity?** Actors share spatial state and an update/draw contract, but there is no useful generic actor behavior to instantiate by itself.

**Why one Champion class and several enemy classes?** Champion choices differ by configuration. Enemies have genuine behavior overrides such as flight and timed attacks. The design uses each mechanism for the difference it expresses.

**What is the difference between abstraction and encapsulation?** Abstraction gives the useful contract. Encapsulation bundles state/operations and protects invariants. Damageable is a small contract; Inventory.spend enforces a valid state transition.

**What is the difference between inheritance and subtyping?** Inheritance derives a class and reuses implementation. Subtyping permits use through a compatible general reference. An interface supplies subtyping without sharing actor state.

**What is the difference between overloading and overriding?** GameSession constructors have different parameter lists and are selected at compile time. Bat.canFly overrides the parent's same signature and is selected through runtime dispatch.

**Does final make Inventory immutable?** No. The final reference cannot be reassigned; its contents change through validated operations. Copied views prevent external map/set editing.

**Why are some constants public?** Shared immutable configuration is part of the supported API. Mutable instance state is private. Access, class/instance storage and reassignment policy are three separate design decisions.

**Are getEnemies results deeply immutable?** No. The list structure is unmodifiable, but its elements are live domain objects. Actor methods still control their state. Snapshots instead copy saved values for the worker boundary.

**Why not do file work directly in the game update?** Disk work can block Swing and freeze input/rendering. A background worker operates on copied values; Swing applies the result.

**What happens to a corrupted save?** Parsing/validation fails, the panel explains the error, and the current campaign remains. Older incompatible formats are rejected explicitly.

**Can a player finish with diamonds in inventory?** No. Readiness checks banked diamonds. The player must survive the return and interact at the ship.

**What would adding an enemy require?** A behavior implementation when necessary, factory/type registration, art and save validation. Extensibility does not mean every new feature changes only one file.

**What are the limits?** One local campaign/save slot; no network play. Headless checks cannot prove speaker output. Administrative course steps and the examination are separate from software checks.

Before the exam, rehearse the two-minute explanation, trace the five code examples without notes, run the game and answer these questions aloud. If asked about unfamiliar code, read the method, identify its inputs/state changes/caller, and reason from the implementation rather than inventing an answer.
