package model.logic;

import javafx.util.Duration;

public class Settings {
    public static double SCENE_WIDTH = 1080;
    public static double SCENE_HEIGHT = 720;
    public static int ROAD_Y = 360;
    public static int PRIMARY_ROAD_X = -150;
    public static int PRIME_ROAD_PIECE_X = 735;
    public static int SECONDARY_ROAD_PIECE_X = 1810;
    public static int SECONDARY_ROAD_X = 900;
    public static Duration ANIMATION_DURATION = Duration.millis(5000);
    // Taxi is going to have predefined lane positions.
    // Lane 1: y = 360
    // Lane 2: y = 360
    // Lane 3: y = 360
    // Lane 4: y = 360
}
