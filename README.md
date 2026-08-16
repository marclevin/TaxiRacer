# Taxi Racer

Collect fares, dodge potholes, mind the people crossing, and outrun the law.

A JavaFX arcade game. You drive a minibus taxi with the police closing in behind you;
every second you spend slowed down is a second they use to close the gap. Pick up fares
to earn, spend the money on upgrades, and try to buy the lot before they catch you.

---

## Play it

### One command, no Java needed

```
docker compose up
```

Then open **<http://localhost:8080/>**.

The game runs inside the container on its own display and is streamed to your browser, so
Docker is the only thing you need installed. Press `Ctrl+C` in the terminal, or `Exit` in
the game, to stop it.

There is also a wrapper that waits for startup and opens the browser for you:

```powershell
.\run.ps1          # Windows
./run.sh           # Linux / macOS
```

### Natively, if you have a JDK

Faster and smoother than streaming a display over the network. Needs **JDK 17 or newer**
and nothing else — JavaFX is downloaded once into `.javafx/` automatically.

```powershell
.\play.ps1         # Windows
./play.sh          # Linux / macOS
```

Set `JAVAFX_HOME` to reuse a JavaFX SDK you already have.

---

## How to play

| Key | Action |
| --- | --- |
| `UP` / `DOWN` | Change lane |
| `SPACE` | Pick up a fare; once full, drop them off |
| `D` | Pull over early to bank a half load |
| `E` | Fire NOS |
| `P` | Pause |
| `R` | Restart the run |
| `ESC` / `ENTER` | Back to the garage |

The taxi holds the middle of the screen and the city comes to you; the only thing you
choose is which of the four lanes to be in.

**Fares only pay when you drop them off.** Pick them up from the outside lanes — they wait
on the pavements — and they ride in the taxi where you can see them through the windows.

**Every fare is going somewhere specific.** They tell you how far when they get in, and the
lamp under their seat tracks the trip: blue at the start, amber as they near their stop,
**green when you are there**, red once you have driven them well past it. The load gauge in
the corner shows the same thing, one pip per seat, filling as each trip is driven.

Drop somebody off on the green and they pay up to **1.7×** the meter. Shove them out the
moment they get in and they pay **0.3×**; carry them miles past their stop and they pay
about half. The taxi always lets out whoever is closest to their destination first, so a
stop pays best in its opening seconds and worse the longer you sit there.

Once the taxi is full, `SPACE` pulls you over; then mash `SPACE` to let them out one at a
time, each paying as they go. You are stationary throughout and the police close in fast.
Change lane to pull away early, or press `D` to bank a half load the moment the lights go
green.

Being caught still carrying fares loses every unbanked cent. Buying seats is what turns
several stops into one.

| Seats | 0/3 | 1/3 | 2/3 | 3/3 |
| --- | --- | --- | --- | --- |
| Capacity | 5 | 8 | 11 | 15 |

**Watch for people crossing the road.** Hit one and you leave a mess on the tarmac and
across the front of your own taxi, hand the police a large chunk of the gap instantly, and
they drive furious for a while afterwards. The game keeps a running count.

**The police are not just a number behind you.** Once they are close enough to see, they
lean into whichever lane you take, lunge forward and drop back rather than closing at a
steady rate, and the officer in the back window leans out with a pistol. The shot takes
about a second and a half to line up and a reticle shows the lane it is settling on — it
locks on for the last stretch, so **changing lane late sends the round wide**. Take one in
a tyre and you limp, which is exactly the moment they use to catch you.

Your progress saves itself to `saves/profile.sav`. `File > Open profile` and
`File > Save profile as` are there for moving a profile between machines.

Save files written by the original version of the game are still readable. Running
natively, a `save.sav` in the project root is imported automatically the first time. To
carry one into the container instead, copy it to `saves/profile.sav`.

### Difficulty

Difficulty is not just "more potholes" — it changes the chase itself. The settings live in
plain text files in [`dat/`](dat/) and are easy to tweak.

| | Easy | Medium | Hard |
| --- | --- | --- | --- |
| Police start | 1080m | 950m | 820m |
| Clean driving | pulls you away | pulls you away, barely | **police still gain** |
| Potholes | 4 | 10 | 12 |
| People crossing | 18% of spawns | 32% | 46% |
| Cost of one body | ~176m | ~346m | ~540m |

On Hard the gap shrinks no matter how well you drive, so a run is only ever a question of
how much you can earn before they reach you.

---

## Project layout

```
src/                 Java sources
  Launcher.java      Entry point (see note below)
  Main.java          Menus, garage and scene wiring
  game/display/      Sprites, the game loop, backdrop and effects
  game/logic/        Input, collisions, the passenger pool, the upgrade shop
  game/utility/      Assets, difficulty, saves, tuning constants
img/  dat/  res/     Sprites, difficulty profiles, menu stylesheet
docker/              Container entrypoint
```

Assets are loaded from the classpath first and the working directory second, so the game
runs identically from a source checkout, from the packaged jar and inside the container.

`Launcher` exists because the JVM refuses to start a main class that extends
`Application` unless the JavaFX modules are on the module path. Launching through a class
that does not extend it is what lets the game ship as one self-contained jar.

### Building the jar by hand

```
mvn -Djavafx.platform=linux package     # or win / mac / mac-aarch64
```

`docs/build.bat` is the original coursework build script and expects a JavaFX 17 SDK at a
hardcoded path; the scripts above supersede it.
