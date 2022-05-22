package game.utility;
// TODO: Clean up Enum remove width.
public enum EPositions {
    FIRST_LANE(330,200,100),
    SECOND_LANE(350,260,120),
    THIRD_LANE(380,350,150),
    FOURTH_LANE(420,470,180),
    POLICE_DISPLAY(105,100,50);
    
    private final int location;
    private final int width_scale;
    private final int height_scale;
    EPositions(int y, int x, int z)
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
