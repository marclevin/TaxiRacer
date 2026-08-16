package game.utility;

import java.util.EnumMap;
import java.util.Map;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * The game's audio, and its absence.
 *
 * <p>
 * Every sound is optional. The game ships without audio files, and this class is built so
 * that adding one is nothing more than dropping a correctly named file into
 * {@code res/sound/}: anything present is loaded and played, anything missing is silently
 * skipped, and a machine with no audio device at all (a container, for instance) degrades
 * to silence rather than failing.
 * </p>
 *
 * <p>
 * See {@code res/sound/README.md} for the file names, formats and lengths expected.
 * </p>
 */
public final class Sounds {

    /** Where sound files are looked for, relative to the classpath root. */
    private static final String SOUND_DIR = "res/sound/";
    /** Extensions tried for each effect, in order of preference. */
    private static final String[] EXTENSIONS = { ".wav", ".mp3" };

    /**
     * Every sound the game will play, and the file name it looks for.
     *
     * <p>
     * The base name is the contract with whoever supplies the audio: drop
     * {@code pickup.wav} into {@code res/sound/} and {@link Sfx#PICKUP} starts working.
     * </p>
     */
    public enum Sfx {
        /** A fare climbs in. */
        PICKUP("pickup", 0.7),
        /** A fare gets out and pays. */
        DROPOFF("dropoff", 0.7),
        /** The taxi fills up. */
        FULL("full", 0.8),
        /** A pothole is struck. */
        POTHOLE("pothole", 0.6),
        /** A pedestrian is run down. */
        SPLAT("splat", 0.9),
        /** A fare reaches the place they asked to be taken to. */
        READY("ready", 0.55),
        /** The police close to shooting range and start hunting in earnest. */
        SIREN("siren", 0.65),
        /** An officer takes a shot at the taxi. */
        GUNSHOT("gunshot", 0.85),
        /** NOS is fired. */
        NOS("nos", 0.8),
        /** The police make contact. */
        BUST("bust", 1.0),
        /** The run target is reached. */
        WIN("win", 1.0);

        private final String baseName;
        private final double volume;

        /**
         * @param baseName file name, without extension, inside {@code res/sound/}
         * @param volume   playback volume from 0 to 1
         */
        Sfx(String baseName, double volume) {
            this.baseName = baseName;
            this.volume = volume;
        }

        /**
         * Returns the file name this effect looks for, without an extension.
         *
         * @return the base file name.
         */
        public String getBaseName() {
            return baseName;
        }
    }

    private static final Map<Sfx, AudioClip> CLIPS = new EnumMap<>(Sfx.class);

    private static MediaPlayer music = null;
    private static boolean loaded = false;
    private static boolean muted = false;
    /** Cleared for good if the audio stack turns out to be unusable on this machine. */
    private static boolean available = true;

    private Sounds() {
    }

    /**
     * Loads whatever audio files are present. Safe to call more than once.
     *
     * <p>
     * Failures here are never fatal: a container with no sound card should still run the
     * game, so anything thrown by the media stack disables audio and is otherwise ignored.
     * </p>
     */
    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        for (Sfx sfx : Sfx.values()) {
            String url = findSound(sfx.getBaseName());
            if (url == null) {
                continue;
            }
            try {
                AudioClip clip = new AudioClip(url);
                clip.setVolume(sfx.volume);
                CLIPS.put(sfx, clip);
            } catch (Throwable t) {
                // One bad file should not take the rest of the audio with it.
                System.err.println("Sound: could not load " + sfx.getBaseName() + " (" + t + ")");
            }
        }

        loadMusic();
    }

    /**
     * Plays an effect, if a file for it was found.
     *
     * @param sfx the effect to play
     */
    public static void play(Sfx sfx) {
        if (muted || !available) {
            return;
        }
        AudioClip clip = CLIPS.get(sfx);
        if (clip == null) {
            return;
        }
        try {
            clip.play();
        } catch (Throwable t) {
            // No usable audio device. Stop trying rather than logging once a frame.
            available = false;
        }
    }

    /**
     * Turns all audio on or off.
     *
     * @param value {@code true} to silence the game
     */
    public static void setMuted(boolean value) {
        muted = value;
        if (music == null) {
            return;
        }
        try {
            if (muted) {
                music.pause();
            } else {
                music.play();
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    /**
     * Reports whether audio is currently silenced.
     *
     * @return {@code true} when muted.
     */
    public static boolean isMuted() {
        return muted;
    }

    /**
     * Reports whether any audio file was actually found.
     *
     * <p>
     * Used to keep a mute control from being offered on a build that has no sound at all.
     * </p>
     *
     * @return {@code true} if at least one sound or a music track is loaded.
     */
    public static boolean hasAudio() {
        return !CLIPS.isEmpty() || music != null;
    }

    /**
     * Starts the background track looping, if one was supplied.
     */
    private static void loadMusic() {
        String url = findSound("music");
        if (url == null) {
            return;
        }
        try {
            music = new MediaPlayer(new Media(url));
            music.setCycleCount(MediaPlayer.INDEFINITE);
            music.setVolume(0.35);
            if (!muted) {
                music.play();
            }
        } catch (Throwable t) {
            System.err.println("Sound: could not start music (" + t + ")");
            music = null;
        }
    }

    /**
     * Looks for a sound file, trying each supported extension.
     *
     * @param baseName the file name without an extension
     * @return a URL string, or {@code null} when no such file is bundled.
     */
    private static String findSound(String baseName) {
        for (String extension : EXTENSIONS) {
            String url = Assets.url(SOUND_DIR + baseName + extension);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
