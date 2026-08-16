package game.utility;

/**
 * This enum is a settings enum that contains commonly used values.
 */
public enum ESettings {

    /** Height of the game window and canvas, in pixels. */
    SCENE_HEIGHT(720),
    /** Width of the game window and canvas, in pixels. */
    SCENE_WIDTH(1080),
    /** Y coordinate at which the road backdrop starts. */
    ROAD_Y(360),
    /**
     * Horizontal step between road tiles, in pixels.
     *
     * <p>
     * The road sprite is drawn in perspective, so it is a trapezoid: its narrowest row is
     * only 722px of the 1080px image, with transparent corners either side. Tiles are
     * therefore stepped by less than that width so each one's transparent corner is covered
     * by its neighbour, instead of leaving a triangular hole at every seam.
     * </p>
     */
    ROAD_TILE_SPACING(700),
    /** Y coordinate the taxi and police cars start at. */
    TAXI_INIT_Y(390),
    /**
     * Fares that must be *delivered* in a single run to win outright.
     *
     * <p>
     * Far lower than it looks: a fare only counts once it has been dropped off, and every
     * drop-off means stopping while the police close in.
     * </p>
     */
    TARGET_PASSENGERS(30),
    /** Most fares allowed on the street at once. */
    MAX_ACTIVE_PASSENGERS(10);

    private final int internal_value;

    /**
     * Constructor for common enums.
     *
     * @param val The value of the enum
     */
    ESettings(int val) {
        internal_value = val;
    }

    /**
     * Returns the value of the enum.
     *
     * @return the value of the enum
     */
    public int getVal() {
        return this.internal_value;
    }
}
