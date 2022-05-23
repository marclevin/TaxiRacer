package game.utility;

/**
 * This enum represents police scaling and positions on the sreen.
 */
public enum EPolicePositions {
    FIRST_LANE_POLICE(350,150,0),
    SECOND_LANE_POLICE(375,185,0),
    THIRD_LANE_POLICE(410,220,0),
    FOURTH_LANE_POLICE(460,255,0),
    POLICE_DISPLAY(105,100,50);
    
    private final int location;
    private final int width_scale;
    private final int height_scale;
    /**
     * Constructor for the police enum
     * @param y The location of the police enum.
     * @param x The width scale of the police enum.
     * @param z The height scale of the police enum.
     */
    EPolicePositions(int y, int x, int z)
    {
        this.location = y;
        this.width_scale = x;
        this.height_scale = z;
    }
    /**
     * Gets the police enum location.
     * @return The police enum location.
     */
    public int getLocation()
    {
        return this.location;
    }
    /**
     * This returns the width scale of the police enum.
     * @return the width scale of the police enum.
     */
    public int getWidthScale()
    {
        return this.width_scale;
    }
    /**
     * This returns the height scale of the police enum.
     * @return the height scale of the police enum.
     */
    public int getHeightScale()
    {
        return this.height_scale;
    }
}