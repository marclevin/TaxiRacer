package game.display.models;

import java.util.concurrent.ThreadLocalRandom;

import game.logic.Visitor;
import game.utility.EPassenger;

/**
 * A fare waiting on one of the two pavements.
 *
 * <p>
 * Passengers stand on the top or bottom pavement only, which is why they can be picked up
 * from the outermost lanes: the taxi has to pull over to reach them.
 * </p>
 *
 * <p>
 * Each one is waiting to be taken somewhere specific rather than simply "somewhere", so they
 * carry a trip length as well as a fare. The fare is derived from that trip length rather
 * than rolled independently, so a long ride is visibly worth more than a short one and the
 * player is never asked to guess which of two waiting fares is the better pickup.
 * </p>
 */
public final class Passenger extends Sprite {

    /** Shortest trip anybody asks for, in metres. */
    private static final double MIN_TRIP_METRES = 150;
    /** Longest trip anybody asks for, in metres. */
    private static final double MAX_TRIP_METRES = 620;
    /** Flat charge for getting in at all. */
    private static final double BOARDING_FEE = 1.5;
    /** What the meter adds per metre of the requested trip. */
    private static final double FARE_PER_METRE = 0.015;
    /** Fraction the metered fare is randomly nudged by, either way. */
    private static final double FARE_JITTER = 0.18;

    private EPassenger passenger_location = null;
    private double baseFare = 0;
    private double tripDistance = 0;

    /**
     * Constructor for the passenger.
     *
     * @param x X coordinate of the passenger
     * @param y Y coordinate of the passenger
     */
    public Passenger(int x, int y) {
        super(x, y);
        roll();
    }

    /**
     * Returns the metered fare for the trip this passenger wants.
     *
     * @return the base fare, before the accuracy of the drop-off is taken into account.
     */
    public double getBaseFare() {
        return this.baseFare;
    }

    /**
     * Returns how far this passenger wants to be taken.
     *
     * @return the requested trip length, in metres.
     */
    public double getTripDistance() {
        return this.tripDistance;
    }

    /**
     * Rolls a fresh destination and the fare that goes with it.
     *
     * <p>
     * Called both on creation and every time the pool hands a recycled passenger back out,
     * so a reused instance is indistinguishable from a new one.
     * </p>
     */
    public void roll() {
        this.tripDistance = ThreadLocalRandom.current()
                .nextDouble(MIN_TRIP_METRES, MAX_TRIP_METRES);

        double metered = BOARDING_FEE + tripDistance * FARE_PER_METRE;
        double jitter = ThreadLocalRandom.current().nextDouble(-FARE_JITTER, FARE_JITTER);
        this.baseFare = metered * (1.0 + jitter);
    }

    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * Gets the pavement this passenger is standing on.
     *
     * @return The passenger location.
     */
    public EPassenger getEPassenger() {
        return passenger_location;
    }

    /**
     * Places the passenger on a pavement and sizes it to match.
     *
     * @param info The pavement to stand on.
     */
    public void scale(EPassenger info) {
        this.passenger_location = info;

        this.setY(info.getLocation());
        fitToHeight(info.getHeightScale());
        this.myBound.setWidth(info.getBoundWidth());
        this.myBound.setHeight(info.getHeightScale());

        // Passengers on the near pavement are drawn large and low, so their hit box is
        // pulled up to sit over the kerb rather than over their feet.
        if (info == EPassenger.PASSENGER_BOTTOM) {
            this.myBound.setHeight(EPassenger.PASSENGER_BOTTOM.getBoundHeight());
            this.myBound.setY(this.myBound.getY() - 40);
        }
    }
}
