package game.utility;

/**
 * The two pavements a passenger can wait on, with the scaling that sells the perspective.
 *
 * <p>
 * The far pavement is drawn small and high on the screen, the near pavement large and low.
 * Each entry also carries its collision box, which is deliberately wider than the sprite so
 * that flagging a fare down does not demand pixel-perfect positioning.
 * </p>
 */
public enum EPassenger {

    /** The near pavement, at the bottom of the screen; reachable from the outside lane. */
    PASSENGER_BOTTOM(86, 540, 100, 100),
    /** The far pavement, at the top of the screen; reachable from the inside lane. */
    PASSENGER_TOP(50, 300, 100, 50);

    private final int height_scale;
    private final int location;
    private final int bound_width;
    private final int bound_height;

    /**
     * Constructor for the passenger enum.
     *
     * @param heightScale how tall the sprite is drawn, in pixels
     * @param location    the Y coordinate of the pavement
     * @param boundWidth  the width of the pickup box
     * @param boundHeight the height of the pickup box
     */
    EPassenger(int heightScale, int location, int boundWidth, int boundHeight) {
        this.height_scale = heightScale;
        this.location = location;
        this.bound_width = boundWidth;
        this.bound_height = boundHeight;
    }

    /**
     * Gets the passenger enum location.
     *
     * @return The passenger enum location.
     */
    public int getLocation() {
        return location;
    }

    /**
     * This returns the height scale of the passenger enum.
     *
     * @return the height scale of the passenger enum.
     */
    public int getHeightScale() {
        return height_scale;
    }

    /**
     * Returns the width of the pickup box.
     *
     * @return the pickup box width in pixels.
     */
    public int getBoundWidth() {
        return bound_width;
    }

    /**
     * Returns the height of the pickup box.
     *
     * @return the pickup box height in pixels.
     */
    public int getBoundHeight() {
        return bound_height;
    }
}
