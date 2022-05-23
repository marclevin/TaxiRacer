package game.utility;

/**
 * This enum represents pothole positions (Y-Axis).
 */
public enum EPothole {
    FIRST_LANE_POTHOLE(380),
    SECOND_LANE_POTHOLE(435),
    THIRD_LANE_POTHOLE(475),
    FOURTH_LANE_POTHOLE(535);
    private final int location;

    /**
     * Constructor for the pothole enum
     * @param y location.
     */
    EPothole(int y)
    {
        this.location = y;
    }

    /**
     * Returns the location of the pothole (Y-axis)
     * @return the location of the pothole (Y-axis)
     */
    public int getLocation()
    {
        return this.location;
    }
}
