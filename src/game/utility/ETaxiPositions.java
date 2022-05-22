package game.utility;
public enum ETaxiPositions {
    FIRST_LANE_TAXI(330,200,100), // 0
    SECOND_LANE_TAXI(350,260,120), // 1
    THIRD_LANE_TAXI(380,350,150), // 2
    FOURTH_LANE_TAXI(420,470,180); // 3
    
    private final int location;
    private final int width_scale;
    private final int height_scale;
    ETaxiPositions(int y, int x, int z)
    {
        this.location = y;
        this.width_scale = x;
        this.height_scale = z;
    }
    public int getLocation()
    {
        return this.location;
    }
    public int getWidthScale()
    {
        return this.width_scale;
    }
    public int getHeightScale()
    {
        return this.height_scale;
    }
}
