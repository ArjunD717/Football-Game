# Football Duel

A fast, local-multiplayer 2D football game built with **Java 17** and **JavaFX 21**.
Two players battle on a single pitch — first to **5 goals** wins — with optional
Easy or Hard AI opponents, impulse-based physics, and a polished JavaFX HUD.

**Author:** Arjun Dhir

---

## Features

- **Local multiplayer** — Player 1 (`WASD` + `Space`) vs Player 2 (`Arrow keys` + `Enter`)
- **Two AI opponents** — Easy AI holds a defensive shape; Hard AI predicts ball
  trajectory, intercepts, and aims shots with lead
- **Real physics feel** — acceleration/drag movement, elastic circle collisions
  with positional correction, momentum-preserving kicks, wall restitution
- **Full match flow** — kickoff pauses, goal banners, winner announcement,
  restart via `R` or the File menu
- **Vector-drawn pitch** — goals, centre circle, penalty boxes/arcs, corner arcs,
  shadows, kick-cooldown rings, and ball-spin rendering — no assets required

## Project structure

```text
pom.xml                              Maven build (Java 17, JavaFX 21, JUnit 5)
src/main/java/footy/
  Game.java                          JavaFX app: game loop, HUD, menus, AI
  Player.java                        Movement, kicking, cooldowns, rendering
  Ball.java                          Friction, speed cap, spin rendering
  Pitch.java                         Geometry, goals, wall bounces, rendering
  GameObject.java                    Shared position/velocity/radius/mass base
  CollisionMath.java                 Elastic circle-collision resolution
  Vector2.java                       Mutable 2D vector math helper
src/test/java/footy/                Unit tests (physics, goals, vectors)
```

## Requirements

| Prerequisite | Windows | macOS | Linux (Ubuntu/Debian) |
|---|---|---|---|
| JDK 17+ (LTS recommended) | `winget install EclipseAdoptium.Temurin.17.JDK` | `brew install --cask temurin@17` | `sudo apt install -y temurin-17-jdk` (via Adoptium repo) or `sudo apt install -y openjdk-17-jdk` |
| Maven 3.8+ | `winget install Apache.Maven` | `brew install maven` | `sudo apt install -y maven` |
| JavaFX 21 | Resolved automatically from Maven Central — no manual install | Same — automatic | Same — automatic, but needs a display + GTK libs (see Linux note below) |

> Verify with `java -version` and `mvn -version`. Java must report 17+.
>
> **Linux display + system libs:** JavaFX needs a running X11/Wayland session plus GTK 3. On Debian/Ubuntu:
> `sudo apt install -y libgtk-3-0 libxtst6 libgl1-mesa-glx`.
> Headless servers/WSL without a display will fail — run in a desktop session (or WSLg) with `$DISPLAY`/`$WAYLAND_DISPLAY` set.

## Run

One command on every OS (run from the repo root):

```bash
mvn javafx:run
```

Shell-specific notes:

- **Windows (Command Prompt):** `mvn javafx:run`
- **Windows (PowerShell):** `mvn javafx:run` (same; use `.\` prefix only for local scripts, not needed here)
- **macOS (Terminal / zsh):** `mvn javafx:run`
- **Linux (bash):** `mvn javafx:run`

> First launch downloads JavaFX 21 + dependencies from Maven Central, so it takes longer than subsequent runs.

## Build and test

| Task | Windows (CMD / PowerShell) | macOS / Linux (bash / zsh) |
|---|---|---|
| Run unit tests | `mvn test` | `mvn test` |
| Build JAR | `mvn -q -DskipTests package` | `mvn -q -DskipTests package` |
| Build + test | `mvn package` | `mvn package` |

The build produces `target/football-duel-1.0.0.jar`.

> The packaged JAR needs the JavaFX modules on the module path at runtime;
> `mvn javafx:run` is the supported way to launch during development.

## Controls

| Action | Player 1 | Player 2 |
|---|---|---|
| Move | `W` `A` `S` `D` | `Arrow keys` |
| Kick | `Space` | `Enter` |
| Restart match | `R` (either player) | — |
| Mode / restart / quit | `Mode` and `File` menus | — |

Switch to **Easy AI** or **Hard AI** from the `Mode` menu to play solo —
Player 2 is then computer-controlled.

## Rules

- First to **5 goals** wins the match.
- The whole ball must cross the goal line, inside the goal mouth, to count.
- After each goal there is a short kickoff pause and positions reset.
- The kick has a 1-second cooldown, shown as a ring around each player.

## Architecture notes

- **Game loop** — single `AnimationTimer` with clamped variable timestep
  (`dt ≤ 33 ms`), so backgrounding the window can't explode the physics.
- **Update order** — input → integrate → collide → confine/bounce → goal check.
- **Collisions** — `CollisionMath.resolveCircleCollision` splits overlap by
  inverse mass and applies an impulse along the contact normal.
- **AI** — re-plans a target position and kick direction on a fixed interval
  (240 ms Easy / 70 ms Hard) with a movement dead-zone; only kicks when behind
  the ball to avoid own goals.
- **Rendering** — everything is immediate-mode `GraphicsContext` vector drawing;
  `Pitch.draw` takes the canvas size so the game is not coupled to constants.
