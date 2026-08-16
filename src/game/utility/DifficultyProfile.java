package game.utility;

/**
 * Everything a difficulty setting controls.
 *
 * <p>
 * The original game let difficulty change one thing: how many potholes sat in each lane.
 * A profile now also drives the chase itself — how quickly the police close in, how much
 * ground a mistake costs, and how busy the road is — so that Hard is a different game
 * rather than the same game with more holes in it.
 * </p>
 */
public final class DifficultyProfile {

    /** One entry per lane: how many potholes to scatter there. */
    private int[] potholes = { 1, 1, 1, 1 };
    /** How far behind the police start, in metres. */
    private double policeStart = 1080;
    /**
     * Ground the police lose per step while the taxi is at full speed.
     *
     * <p>
     * Negative means clean driving pulls away. A positive value means the police creep
     * closer no matter how well you drive, which is what makes Hard relentless.
     * </p>
     */
    private double cleanDrift = -0.5;
    /** Ground the police gain per step while the taxi is slowed. */
    private double slowGain = 2.0;
    /** Ground the police lose per step while NOS is firing. */
    private double nosDrift = -12.0;
    /** Chance, per fare spawn tick, of a pedestrian stepping into the road instead. */
    private double jaywalkerChance = 0.2;
    /** Most fares allowed on the pavement at once. */
    private int maxFares = 10;
    /** Most pedestrians allowed in the road at once. */
    private int maxJaywalkers = 3;
    /** Steps between spawn attempts. */
    private int spawnSteps = 60;
    /** Ground the police gain instantly when a pedestrian is run down. */
    private double splatPenalty = 80;
    /** How long the police stay enraged after a pedestrian is run down, in steps. */
    private int enrageSteps = 300;
    /** Extra ground the police gain per step while enraged. */
    private double enrageGain = 0.8;

    /**
     * Returns the per-lane pothole counts.
     *
     * @return a copy of the pothole layout.
     */
    public int[] getPotholes() {
        return potholes.clone();
    }

    /**
     * Returns how far behind the police start.
     *
     * @return the starting gap, in metres.
     */
    public double getPoliceStart() {
        return policeStart;
    }

    /**
     * Returns the per-step police drift while driving cleanly.
     *
     * @return metres per step; negative pulls away.
     */
    public double getCleanDrift() {
        return cleanDrift;
    }

    /**
     * Returns the per-step police gain while slowed.
     *
     * @return metres per step.
     */
    public double getSlowGain() {
        return slowGain;
    }

    /**
     * Returns the per-step police drift while NOS is firing.
     *
     * @return metres per step; negative pulls away.
     */
    public double getNosDrift() {
        return nosDrift;
    }

    /**
     * Returns how often a pedestrian crosses instead of waiting for a taxi.
     *
     * @return a probability between 0 and 1.
     */
    public double getJaywalkerChance() {
        return jaywalkerChance;
    }

    /**
     * Returns the cap on waiting fares.
     *
     * @return the maximum number of fares on the pavement.
     */
    public int getMaxFares() {
        return maxFares;
    }

    /**
     * Returns the cap on pedestrians in the road.
     *
     * @return the maximum number crossing at once.
     */
    public int getMaxJaywalkers() {
        return maxJaywalkers;
    }

    /**
     * Returns how many steps pass between spawn attempts.
     *
     * @return the spawn interval, in steps.
     */
    public int getSpawnSteps() {
        return spawnSteps;
    }

    /**
     * Returns the instant chase penalty for running someone down.
     *
     * @return metres of ground handed to the police.
     */
    public double getSplatPenalty() {
        return splatPenalty;
    }

    /**
     * Returns how long the police stay enraged after a pedestrian is hit.
     *
     * @return the duration, in steps.
     */
    public int getEnrageSteps() {
        return enrageSteps;
    }

    /**
     * Returns the extra per-step gain while the police are enraged.
     *
     * @return metres per step.
     */
    public double getEnrageGain() {
        return enrageGain;
    }

    /**
     * Applies one {@code key = value} pair from a difficulty file.
     *
     * <p>
     * Unknown keys are reported and skipped rather than treated as fatal, so a difficulty
     * file written for a newer build still loads on an older one.
     * </p>
     *
     * @param key   the setting name
     * @param value the raw value text
     * @return {@code true} if the key was recognised and applied.
     */
    boolean apply(String key, String value) {
        switch (key) {
            case "potholes":
                potholes = parseLanes(value);
                return true;
            case "policeStart":
                policeStart = Math.max(100, parseNumber(value, policeStart));
                return true;
            case "cleanDrift":
                cleanDrift = parseNumber(value, cleanDrift);
                return true;
            case "slowGain":
                slowGain = parseNumber(value, slowGain);
                return true;
            case "nosDrift":
                nosDrift = parseNumber(value, nosDrift);
                return true;
            case "jaywalkerChance":
                jaywalkerChance = clamp01(parseNumber(value, jaywalkerChance));
                return true;
            case "maxFares":
                maxFares = (int) Math.max(1, parseNumber(value, maxFares));
                return true;
            case "maxJaywalkers":
                maxJaywalkers = (int) Math.max(0, parseNumber(value, maxJaywalkers));
                return true;
            case "spawnSteps":
                spawnSteps = (int) Math.max(6, parseNumber(value, spawnSteps));
                return true;
            case "splatPenalty":
                splatPenalty = Math.max(0, parseNumber(value, splatPenalty));
                return true;
            case "enrageSteps":
                enrageSteps = (int) Math.max(0, parseNumber(value, enrageSteps));
                return true;
            case "enrageGain":
                enrageGain = Math.max(0, parseNumber(value, enrageGain));
                return true;
            default:
                return false;
        }
    }

    /**
     * Replaces the pothole layout, used by the legacy four-line file format.
     *
     * @param lanes the per-lane counts
     */
    void setPotholes(int[] lanes) {
        this.potholes = lanes.clone();
    }

    /**
     * Parses a comma separated pothole layout such as {@code "3,4,5,3"}.
     *
     * @param value the raw text
     * @return four lane counts, padded with ones if the text is short.
     */
    private static int[] parseLanes(String value) {
        String[] parts = value.split(",");
        int[] lanes = { 1, 1, 1, 1 };
        for (int i = 0; i < lanes.length && i < parts.length; i++) {
            try {
                lanes[i] = Math.min(30, Math.max(0, Integer.parseInt(parts[i].trim())));
            } catch (NumberFormatException e) {
                System.err.println("Difficulty: lane " + i + " is not a number (\""
                        + parts[i].trim() + "\"); using 1.");
            }
        }
        return lanes;
    }

    /**
     * Parses a number, keeping the current value if the text is unusable.
     *
     * @param value    the raw text
     * @param fallback the value to keep on failure
     * @return the parsed number, or the fallback.
     */
    private static double parseNumber(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Difficulty: \"" + value.trim() + "\" is not a number; keeping "
                    + fallback + ".");
            return fallback;
        }
    }

    /**
     * Constrains a probability to the 0..1 range.
     *
     * @param value the raw probability
     * @return the value clamped to 0..1.
     */
    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
