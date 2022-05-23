package game.utility;

/**
 * This enum represents passenger positions and scaling.
 */
public enum EPassenger {
    PASSENGER_BOTTOM( 0, 540),
    PASSENGER_TOP(50, 300);

    private final int location;
    private final int height_scale;

    /**
     * Constructor for the passenger enum
     * @param y The height scale of the passenger
     * @param z The location of the passenger
     */
    EPassenger(int y, int z) {
        height_scale = y;
        location = z;
    }

    /**
     * Gets the passenger enum location.
     * @return The passenger enum location.
     */
    public final int getLocation() {
        return location;
    }

    /**
     * This returns the height scale of the passenger enum.
     * @return the height scale of the passenger enum.
     */
    public final int getHeightScale() {
        return height_scale;
    }

}
