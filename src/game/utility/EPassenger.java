package game.utility;

public enum EPassenger {
    PASSENGER_BOTTOM( 0, 540),
    PASSENGER_TOP(50, 300);

    private final int location;
    private final int height_scale;

    EPassenger(int y, int z) {
        height_scale = y;
        location = z;
    }

    public final int getLocation() {
        return location;
    }

  

    public final int getHeightScale() {
        return height_scale;
    }

}
