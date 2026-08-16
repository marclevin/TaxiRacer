package game.display.models;

/**
 * One fare in the back of the taxi, and how far they actually wanted to go.
 *
 * <p>
 * A fare is no longer a flat lump of cash collected whenever it suits the driver. Each one
 * boards with a destination a fixed distance down the road, and the meter only reads right
 * when they are let out near it. Stop short and they have to walk; carry them past it and
 * they are late and unimpressed. Either way they pay less than they would have.
 * </p>
 *
 * <p>
 * The payout curve is deliberately continuous: there is no cliff at a band edge, so a fare
 * dropped a metre outside the sweet spot is worth very slightly less than one dropped inside
 * it rather than dramatically less. The bands exist to be *shown* to the player — they drive
 * the colour of the seat lights and the pips — not to create hidden step changes in income.
 * </p>
 */
public final class Ride {

    /**
     * How a trip is going, as shown to the player.
     *
     * <p>
     * This is what colours the head in the taxi window and the pip in the load gauge, so the
     * player can read a full taxi at a glance and decide when stopping is worth it.
     * </p>
     */
    public enum Grade {
        /** Nowhere near the destination yet. */
        EARLY,
        /** Getting close; worth thinking about pulling over. */
        NEARLY,
        /** In the sweet spot. Let them out now. */
        ON_THE_DOT,
        /** Carried well past where they asked to go. */
        OVERSHOT
    }

    /** Below this fraction of the trip, the fare is still settling in. */
    private static final double NEARLY_LOW = 0.68;
    /** Where the paying sweet spot opens. */
    private static final double PERFECT_LOW = 0.90;
    /** Where the paying sweet spot closes. */
    private static final double PERFECT_HIGH = 1.15;

    /** Multiplier at the very edges of the sweet spot. */
    private static final double BAND_MULTIPLIER = 1.25;
    /** Extra multiplier awarded for landing exactly on the destination. */
    private static final double PEAK_BONUS = 0.45;
    /** Multiplier for shoving somebody out the moment they get in. */
    private static final double EARLY_FLOOR = 0.30;
    /** Multiplier lost per whole extra trip length driven past the destination. */
    private static final double OVERSHOOT_DECAY = 0.80;
    /** However lost the fare gets, they still pay this much of the base. */
    private static final double OVERSHOOT_FLOOR = 0.45;

    private final double baseFare;
    private final double targetDistance;

    private double travelled = 0;
    /** Set once the "this fare is ready" shout has been made, so it is only made once. */
    private boolean announced = false;

    /**
     * Takes a fare aboard.
     *
     * @param baseFare       what the meter reads for the trip they asked for
     * @param targetDistance how far down the road they want to go, in metres
     */
    public Ride(double baseFare, double targetDistance) {
        this.baseFare = Math.max(0, baseFare);
        this.targetDistance = Math.max(1, targetDistance);
    }

    /**
     * Advances the meter.
     *
     * @param metres how far the taxi moved this step
     */
    public void advance(double metres) {
        if (metres > 0) {
            travelled += metres;
        }
    }

    /**
     * Returns how far this fare has been carried.
     *
     * @return the distance driven since boarding, in metres.
     */
    public double getTravelled() {
        return travelled;
    }

    /**
     * Returns how far this fare asked to go.
     *
     * @return the requested trip length, in metres.
     */
    public double getTargetDistance() {
        return targetDistance;
    }

    /**
     * Returns how much further there is to go.
     *
     * @return the metres still to drive, or {@code 0} once the destination is passed.
     */
    public double getRemaining() {
        return Math.max(0, targetDistance - travelled);
    }

    /**
     * Returns the base fare, before the accuracy of the drop-off is taken into account.
     *
     * @return the metered fare in rand.
     */
    public double getBaseFare() {
        return baseFare;
    }

    /**
     * Returns how much of the requested trip has been driven.
     *
     * @return {@code 1.0} exactly at the destination, more once it is behind you.
     */
    public double getRatio() {
        return travelled / targetDistance;
    }

    /**
     * Returns progress towards the destination, clamped for drawing a bar.
     *
     * @return a fraction between 0 and 1.
     */
    public double getFill() {
        return Math.min(1.0, getRatio());
    }

    /**
     * Returns how this trip is going.
     *
     * @return the grade the fare would be paid at right now.
     */
    public Grade getGrade() {
        double ratio = getRatio();
        if (ratio > PERFECT_HIGH) {
            return Grade.OVERSHOT;
        }
        if (ratio >= PERFECT_LOW) {
            return Grade.ON_THE_DOT;
        }
        if (ratio >= NEARLY_LOW) {
            return Grade.NEARLY;
        }
        return Grade.EARLY;
    }

    /**
     * Returns what the fare pays right now, as a multiple of the metered fare.
     *
     * @return the payout multiplier; {@code 1.7} at best, {@code 0.3} at worst.
     */
    public double getMultiplier() {
        double ratio = getRatio();

        if (ratio < PERFECT_LOW) {
            // A straight ramp up to the band edge: a fare shoved out immediately is nearly
            // worthless, and every metre driven towards their destination is worth something.
            return EARLY_FLOOR + (BAND_MULTIPLIER - EARLY_FLOOR) * (ratio / PERFECT_LOW);
        }

        if (ratio <= PERFECT_HIGH) {
            // Normalised per side, because the band is not symmetric: overshooting slightly
            // is more forgivable than stopping short, so it is given more room.
            double off = ratio <= 1.0
                    ? (1.0 - ratio) / (1.0 - PERFECT_LOW)
                    : (ratio - 1.0) / (PERFECT_HIGH - 1.0);
            return BAND_MULTIPLIER + PEAK_BONUS * (1.0 - off);
        }

        return Math.max(OVERSHOOT_FLOOR,
                BAND_MULTIPLIER - OVERSHOOT_DECAY * (ratio - PERFECT_HIGH));
    }

    /**
     * Returns what this fare would hand over if they were let out this instant.
     *
     * @return the payout in rand.
     */
    public double getPayout() {
        return baseFare * getMultiplier();
    }

    /**
     * Reports whether the "this one is ready" shout has already been made for this fare.
     *
     * @return {@code true} once announced.
     */
    public boolean isAnnounced() {
        return announced;
    }

    /**
     * Records that the player has been told this fare is ready.
     */
    public void markAnnounced() {
        announced = true;
    }
}
