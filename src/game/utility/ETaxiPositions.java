package game.utility;
/**
 * This enum represents Taxi positions and scaling.
 */
public enum ETaxiPositions {
    FIRST_LANE_TAXI(330,200,100), // 0
    SECOND_LANE_TAXI(350,260,120), // 1
    THIRD_LANE_TAXI(380,350,150), // 2
    FOURTH_LANE_TAXI(420,470,180); // 3
    
    private final int location;
    private final int width_scale;
    private final int height_scale;
    /**
     * Constructor for the taxi enum
     * @param y The location of the taxi enum.
     * @param x The width scale of the taxi enum.
     * @param z The height scale of the taxi enum.
     */
    ETaxiPositions(int y, int x, int z)
    {
        this.location = y;
        this.width_scale = x;
        this.height_scale = z;
    }
    /**
     * Returns the location of the taxi (Y-axis)
     * @return the location of the taxi (Y-axis)
     */
    public int getLocation()
    {
        return this.location;
    }
    /**
     * This returns the width scale of the taxi enum.
     * @return the width scale of the taxi enum.
     */
    public int getWidthScale()
    {
        return this.width_scale;
    }
    /**
     * This returns the height scale of the taxi enum.
     * @return the height scale of the taxi enum.
     */
    public int getHeightScale()
    {
        return this.height_scale;
    }
}
