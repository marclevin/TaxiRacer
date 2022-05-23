package game.utility;

public enum EPothole {
    FIRST_LANE_POTHOLE(380),
    SECOND_LANE_POTHOLE(435),
    THIRD_LANE_POTHOLE(475),
    FOURTH_LANE_POTHOLE(535);
    private final int location;

    EPothole(int y)
    {
        this.location = y;
    }

    public int getLocation()
    {
        return this.location;
    }
}
