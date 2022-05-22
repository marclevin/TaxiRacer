package game.utility;
public enum EPolicePositions {
    FIRST_LANE_POLICE(350,150,0),
    SECOND_LANE_POLICE(375,185,0),
    THIRD_LANE_POLICE(410,220,0),
    FOURTH_LANE_POLICE(460,255,0),
    POLICE_DISPLAY(105,100,50);
    
    private final int location;
    private final int width_scale;
    private final int height_scale;
    EPolicePositions(int y, int x, int z)
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