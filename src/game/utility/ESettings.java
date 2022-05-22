package game.utility;

public enum ESettings {
     SCENE_HEIGHT(720),
     ROAD_Y(360),
     PRIMARY_ROAD_X(-150),
     SECONDARY_ROAD_X(500),
     TERTIARY_ROAD_X(1500),
     QUATERNARY_ROAD_X(936),
     SCENE_WIDTH(1080),
     TAXI_INIT_Y(390);

    private final int internal_value;
    ESettings(int val)
    {
    internal_value = val;
    }

    public final int getVal()
    {
        return this.internal_value;
    }
}
