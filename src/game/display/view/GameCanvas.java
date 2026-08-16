package game.display.view;

import java.util.ArrayList;
import java.util.List;

import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Road;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.utility.Assets;
import game.utility.DifficultyLoader;
import game.utility.EPothole;
import game.utility.ESettings;
import game.utility.ETaxiPositions;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * This class is responsible for drawing the game to the screen.
 */
public final class GameCanvas extends Canvas {

    /**
     * Road tiles kept in play.
     *
     * <p>
     * Four is the smallest number that covers the canvas at
     * {@link ESettings#ROAD_TILE_SPACING}: one starts off screen to the left, two cover the
     * visible road, and the fourth is scrolling in from the right.
     * </p>
     */
    private static final int ROAD_TILES = 4;

    private final GraphicsContext gc;
    private List<Sprite> sprites = new ArrayList<>();
    private List<Road> roads = new ArrayList<>();
    private AnimationHandler handler = null;
    private Runnable onRunEnded = null;

    /**
     * This is the constructor for the game canvas. (default)
     */
    public GameCanvas() {
        this(ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    /**
     * This is the constructor for the game canvas.
     *
     * @param width  The width of the game canvas.
     * @param height The height of the game canvas.
     */
    public GameCanvas(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
    }

    /**
     * Registers a callback fired whenever a run ends, used to bank the player's progress.
     *
     * @param callback the action to run.
     */
    public void setOnRunEnded(Runnable callback) {
        this.onRunEnded = callback;
        if (handler != null) {
            handler.setOnRunEnded(callback);
        }
    }

    /**
     * This method is responsible for the initialization of the game canvas.
     *
     * <p>
     * This is done here rather than in the constructor because the game can be loaded and
     * saved, so the taxi being driven is not known when the canvas is built.
     * </p>
     */
    public void initCanvas() {
        // Passengers left on the street belong to the run that just ended.
        PassengerPool.getInstance().clear();

        roads = loadRoads();
        sprites = loadFreshSprites();

        handler = new AnimationHandler(gc, sprites, roads, Assets.image("img/passenger.png"));
        handler.setOnRunEnded(onRunEnded);
        handler.resetGame();
    }

    /**
     * This function runs the underlying animation handler.
     */
    public void runAnimator() {
        if (handler == null) {
            initCanvas();
        }
        InputHandler.resetForNewRun();
        handler.start();
    }

    /**
     * This function stops the underlying animation handler and resets the game state.
     */
    public void stopAnimator() {
        if (handler == null) {
            return;
        }
        handler.stop();
        handler.resetGame();
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    /**
     * Abandons the current run and immediately starts a fresh one, keeping the player's cash.
     */
    public void restartRun() {
        if (handler != null) {
            handler.stop();
        }
        initCanvas();
        runAnimator();
    }

    /**
     * Builds the scrolling road backdrop.
     *
     * @return the road tiles, left to right.
     */
    private List<Road> loadRoads() {
        Image roadImage = Assets.image("img/road_try.png");
        List<Road> tiles = new ArrayList<>(ROAD_TILES);
        for (int i = 0; i < ROAD_TILES; i++) {
            Road tile = new Road((i - 1) * ESettings.ROAD_TILE_SPACING.getVal(), ESettings.ROAD_Y.getVal());
            tile.setImage(roadImage);
            tiles.add(tile);
        }
        return tiles;
    }

    /**
     * This function loads sprites and populates the main sprite array as well as sets the
     * main police and taxi sprites.
     *
     * @return the collidable sprites for a fresh run.
     */
    private List<Sprite> loadFreshSprites() {
        Image potholeImage = Assets.image("img/pothole.png");
        Image taxiPrime = Assets.image("img/taxi_move_1.png");
        Image taxiSecond = Assets.image("img/taxi_move_2.png");
        Image passengerImage = Assets.image("img/passenger.png");
        Image policePrime = Assets.image("img/police_move_1.png");
        Image policeSecond = Assets.image("img/police_move_2.png");

        List<Sprite> loadedSprites = new ArrayList<>();

        // The chosen difficulty decides how many potholes sit in each lane.
        int[] potholeLanes = DifficultyLoader.getProfile().getPotholes();
        for (int lane = 0; lane < potholeLanes.length; lane++) {
            for (int k = 0; k < potholeLanes[lane]; k++) {
                Pothole pothole = new Pothole(randomX(), EPothole.values()[lane].getLocation());
                pothole.setImage(potholeImage);
                loadedSprites.add(pothole);
            }
        }

        // A continued game brings its own taxi; a new one starts from scratch.
        Taxi taxi = InputHandler.getTaxi();
        if (taxi == null) {
            taxi = new Taxi(0, ESettings.TAXI_INIT_Y.getVal());
            InputHandler.setTaxi(taxi);
        }
        taxi.setImageSet(taxiPrime, taxiSecond);
        taxi.setOccupantImage(passengerImage);
        // scale() parks the taxi in the middle of the road; nothing else moves it sideways.
        taxi.scale(ETaxiPositions.values()[InputHandler.DEFAULT_LANE]);

        Police police = new Police((int) Police.DEFAULT_START_DISTANCE, ESettings.TAXI_INIT_Y.getVal());
        police.setImageSet(policePrime, policeSecond);
        InputHandler.setPolice(police);

        PassengerPool.setPassengerImage(passengerImage);
        return loadedSprites;
    }

    /**
     * This function gets a random x coordinate.
     *
     * <p>
     * The band is twice the screen width because sprites scroll left and wrap around, so
     * half of them start off screen to the right.
     * </p>
     *
     * @return a random x coordinate
     */
    private static int randomX() {
        int width = ESettings.SCENE_WIDTH.getVal();
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(-width, width);
    }
}
