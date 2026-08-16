# Sound

The game looks for audio files **in this folder** and plays whatever it finds. Nothing here
is required: any file you do not supply is simply skipped, and the game runs silently. You
do not need to touch any code — drop a correctly named file in and it starts working on the
next launch.

Press **`M`** in game to mute and unmute.

## What to add

Name the file exactly as listed. `.wav` is tried first, then `.mp3`.

| File | Plays when | Suggested length | Notes |
| --- | --- | --- | --- |
| `pickup.wav` | A fare climbs in | **0.1 – 0.3 s** | Door thunk or a short coin blip. Heard constantly, so keep it quiet and unobtrusive. |
| `dropoff.wav` | Each fare gets out and pays | **0.1 – 0.3 s** | Till / coin. This one fires up to 10× a second while mashing, so it *must* be short and dry — anything with a tail will smear into mush. |
| `full.wav` | The last seat is taken | **0.3 – 0.6 s** | A two-note "attention" chime. Fires once per load. |
| `pothole.wav` | You hit a pothole | **0.2 – 0.4 s** | Suspension thud. Dry, low. |
| `splat.wav` | You run someone down | **0.4 – 0.8 s** | Wet impact. The one sound allowed to be nasty. |
| `ready.wav` | A fare reaches the stop they asked for | **0.2 – 0.4 s** | A single soft bell. It fires several times a minute, so it must be pleasant and quiet — this is the cue the whole fare system is played by. |
| `siren.wav` | The police first close to shooting range | **1.0 – 2.0 s** | A siren whoop. Plays once per run, as they arrive. |
| `gunshot.wav` | An officer shoots at your tyres | **0.3 – 0.6 s** | A single crack, dry and short. No long tail — one can land every three seconds. |
| `nos.wav` | NOS is fired | **1.0 – 2.0 s** | Whoosh / turbo spool. The boost lasts 4 s, so a shorter sound that decays is fine. |
| `bust.wav` | The police catch you | **1.0 – 2.0 s** | Siren whoop or a crash. Plays once as the run ends. |
| `win.wav` | You deliver the target | **1.5 – 3.0 s** | Short fanfare. |
| `music.wav` | Continuously, looped | **60 – 120 s** | Optional. Use `.mp3` here — a two minute WAV is ~20 MB. Make it loop cleanly: no fade in or out, and trim to an exact bar. Plays at 35% volume under everything else. |

## Format

- **Sample rate** 44.1 kHz. **Bit depth** 16-bit PCM.
- **Mono** for effects (they are not positioned, so stereo just doubles the size).
  **Stereo** is fine for `music`.
- **Trim the silence** off the front of every effect. Even 50 ms of leading silence reads as
  input lag when it is attached to a key press.
- **Normalise to about -3 dBFS**, then let the per-sound volumes in
  [`Sounds.java`](../../src/game/utility/Sounds.java) balance the mix. They are set so that
  `splat` and `bust` sit loudest and `pothole` sits quietest.

## Where to get them

Anything under a permissive licence works — [freesound.org](https://freesound.org) (filter
by CC0), [kenney.nl/assets](https://kenney.nl/assets) (public domain game audio packs), or
[sfxr / jsfxr](https://sfxr.me) if you want to generate retro effects in the browser in
about a minute. If you use anything with an attribution requirement, add a credit line to
the project README.

## Checking it worked

Start the game and pick up a fare. If a file is malformed, the reason is printed to the
console at startup (`Sound: could not load ...`) and that one effect is skipped — the rest
carry on.

Audio needs the `javafx.media` module, which the build scripts and the Maven build already
request. In the Docker container there is no audio device, so the game runs silently there
regardless of what you put in this folder; use `.\play.ps1` to hear it.
