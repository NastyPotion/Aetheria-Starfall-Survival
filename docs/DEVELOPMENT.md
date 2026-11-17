# Development

The game uses Java 21 and the standard library. No game engine or extra runtime dependencies are required.

## Build and run

From the project folder in PowerShell:

```powershell
.\run.ps1
```

The launcher builds the game first. To select a JDK explicitly:

```powershell
.\run.ps1 -JavaHome 'C:\path\to\jdk-21'
```

The scripts also check `JAVA_HOME`, the local Android Studio JDK and `PATH`.

For Eclipse, import this folder as an existing project, select **JavaSE-21**, then run `com.game.core.Main`. Other Java IDEs can use `src` as the source/resource root and the same main class. The Eclipse project name is `AetheriaCrashLandings`.

## Test and package

```powershell
.\scripts\test.ps1
.\scripts\package.ps1
```

Both accept `-JavaHome`. Compilation uses `--release 21 -Xlint:all -Werror`. The five plain-Java test suites cover campaign progression, combat, persistence, interface input and resource rendering. Tests generate previews under `build/previews`.

Packaging produces:

- `dist/AetheriaCrashLandings.jar` — the playable game with its resources.
- `dist/Aetheria-Starfall-Survival-source.zip` — a clean source project with setup instructions and tests.

Run the JAR with `java -jar dist/AetheriaCrashLandings.jar`. The JAR requires a Java 21 runtime and a graphical desktop. The build helpers use Windows PowerShell; the Java application itself uses platform-independent libraries.

## Folder map

| Folder | Contents |
| --- | --- |
| `src` | Game code and title artwork |
| `docs` | Player guide, this development guide and artwork credits |
| `scripts` | Java discovery, build, test and packaging helpers |
| `tests` | Automated checks |

Within `src/com/game`, `core` coordinates the game; `entities`, `survival` and `world` implement its rules; `ui` draws the screens; `art` and `assets` supply graphics; `effects` and `audio` provide feedback; and `persistence` handles saves.

Swing's event thread owns live gameplay. Save workers use immutable snapshots, and audio uses a separate bounded worker. Validate a complete save before replacing a campaign.
