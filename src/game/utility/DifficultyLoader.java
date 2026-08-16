package game.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads difficulty profiles from the files in {@code dat/}.
 *
 * <p>
 * The current format is a list of {@code key = value} lines with {@code #} comments, which
 * lets a difficulty tune the whole chase rather than only the pothole count. Files in the
 * original format — four bare numbers, one pothole count per line — are still accepted.
 * </p>
 */
public final class DifficultyLoader {

    /** One entry per lane of the road. */
    private static final int LANE_COUNT = 4;

    private static DifficultyProfile current = new DifficultyProfile();

    private DifficultyLoader() {
    }

    /**
     * Loads the profile for a difficulty, falling back to safe defaults on any problem.
     *
     * @param difficulty the difficulty to load
     */
    public static void loadDifficulty(EDifficulty difficulty) {
        DifficultyProfile profile = new DifficultyProfile();

        try (InputStream in = Assets.open(difficulty.getResource());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }

            if (isLegacyFormat(lines)) {
                readLegacy(lines, profile, difficulty);
            } else {
                readKeyValues(lines, profile, difficulty);
            }
        } catch (IOException e) {
            System.err.println("Could not read difficulty " + difficulty.getLabel()
                    + " (" + difficulty.getResource() + "): " + e.getMessage()
                    + " - using defaults.");
        }

        current = profile;
    }

    /**
     * Returns the profile currently in force.
     *
     * @return the loaded difficulty profile, never {@code null}.
     */
    public static DifficultyProfile getProfile() {
        return current;
    }

    /**
     * This returns the number of potholes in each lane.
     *
     * @return a copy of the per-lane pothole counts.
     */
    public static int[] getLanes() {
        return current.getPotholes();
    }

    /**
     * Detects a file written in the original format: four bare numbers and nothing else.
     *
     * @param lines the file's non-blank, non-comment lines
     * @return {@code true} if this looks like an original-format file.
     */
    private static boolean isLegacyFormat(List<String> lines) {
        if (lines.size() != LANE_COUNT) {
            return false;
        }
        for (String line : lines) {
            if (line.indexOf('=') >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads the original four-line format.
     *
     * @param lines      the file's lines
     * @param profile    the profile to populate
     * @param difficulty the difficulty being loaded, for error messages
     */
    private static void readLegacy(List<String> lines, DifficultyProfile profile, EDifficulty difficulty) {
        int[] lanes = new int[LANE_COUNT];
        for (int i = 0; i < LANE_COUNT; i++) {
            try {
                lanes[i] = Math.min(30, Math.max(0, Integer.parseInt(lines.get(i))));
            } catch (NumberFormatException e) {
                System.err.println("Difficulty " + difficulty.getLabel() + " lane " + i
                        + " is not a number (\"" + lines.get(i) + "\"); using 1.");
                lanes[i] = 1;
            }
        }
        profile.setPotholes(lanes);
    }

    /**
     * Reads the current {@code key = value} format.
     *
     * @param lines      the file's lines
     * @param profile    the profile to populate
     * @param difficulty the difficulty being loaded, for error messages
     */
    private static void readKeyValues(List<String> lines, DifficultyProfile profile, EDifficulty difficulty) {
        for (String line : lines) {
            int split = line.indexOf('=');
            if (split < 0) {
                System.err.println("Difficulty " + difficulty.getLabel()
                        + ": ignoring line without '=': " + line);
                continue;
            }
            String key = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            if (!profile.apply(key, value)) {
                System.err.println("Difficulty " + difficulty.getLabel()
                        + ": ignoring unknown setting '" + key + "'.");
            }
        }
    }
}
