package game.display.view;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Road;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.utility.ETaxiPositions;
import game.utility.DifficultyLoader;
import game.utility.EPothole;
import game.utility.ESettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * This class is responsible for drawing the game to the screen.
 */
public class GameCanvas extends Canvas {
    private GraphicsContext gc = null;
    private ArrayList<Sprite> sprites = null;
    private AnimationHandler handler = null;
    private Taxi taxi = null;

    /**
     * This is the constructor for the game canvas. (default)
     */
    public GameCanvas() {
        this(ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    /**
     * This is the constructor for the game canvas.
     * @param width The width of the game canvas.
     * @param height The height of the game canvas.
     */
    public GameCanvas(double width, double height) {
        super(width, height);
        gc = this.getGraphicsContext2D();

    };

    /**
     * This method is responsible for the initialization of the game canvas.
     * This is done here as the game can load and save and will not always occur during construction of the object.
     */
    public void initCanvas() {
        sprites = load_fresh_sprites();
        handler = new AnimationHandler(gc, sprites);
    }

    /**
     * This function runs the underlying animation handler.
     */
    public void runAnimator() {
        handler.start();
    }

    /**
     * This function stops the underlying animation handler and resets the game state.
     */
    public void stopAnimator() {
        handler.stop();
        handler.resetGame();
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }


    /**
     * This function loads sprites and populates the main sprite array as well as sets the main police and taxi sprites.
     * @return an {@code ArrayList<Sprite>} of sprites.
     */
    private ArrayList<Sprite> load_fresh_sprites() {
        // Image loading.
        Image pothole_image = load_image("img\\pothole.png");
        Image road_image = load_image("img\\road_try.png");
        Image taxi_prime = load_image("img\\taxi_move_1.png");
        Image taxi_second = load_image("img\\taxi_move_2.png");
        Image passenger_image = load_image("img\\passenger.png");
        Image police_prime = load_image("img\\police_move_1.png");
        Image police_second = load_image("img\\police_move_2.png");
        ArrayList<Sprite> loadedSprites = new ArrayList<Sprite>();
        Road primary_road = new Road(ESettings.PRIMARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal());
        primary_road.setImage(road_image);
        Road secondary_road = new Road(ESettings.SECONDARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal());
        secondary_road.setImage(road_image);
        Road tertiary_road = new Road(ESettings.TERTIARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal());
        tertiary_road.setImage(road_image);
        Road quaternary_road = new Road(ESettings.QUATERNARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal());
        quaternary_road.setImage(road_image);

        // Adding the roads
        loadedSprites.add(primary_road);
        loadedSprites.add(secondary_road);
        loadedSprites.add(tertiary_road);
        loadedSprites.add(quaternary_road);



       // Difficulty Loading
       int[] potholeLanes = DifficultyLoader.getLanes();
        
        for (int i = 0; i < potholeLanes.length; i++) {
            for (int k = 0; k < potholeLanes[i]; k++)
            {
            Pothole temp = new Pothole(rand_x(),EPothole.values()[i].getLocation());
            temp.setImage(pothole_image);
            loadedSprites.add(temp);
            }
        }
        



        // Logic to prevent invalid states.
        if (InputHandler.getTaxi() != null) {
            this.taxi = InputHandler.getTaxi();
            taxi.setImageSet(taxi_prime, taxi_second);
            taxi.scale(ETaxiPositions.values()[2]);
        } else {
            taxi = new Taxi(0, ESettings.TAXI_INIT_Y.getVal());
            taxi.setImageSet(taxi_prime, taxi_second);
            taxi.scale(ETaxiPositions.values()[2]);
            InputHandler.setTaxi(taxi);
        }
        Police police = new Police(-ESettings.SCENE_WIDTH.getVal(), ESettings.TAXI_INIT_Y.getVal());
        police.setImageSet(police_prime, police_second);
        InputHandler.setPolice(police);
        PassengerPool.setPassengerImage(passenger_image);
        return loadedSprites;
    }

    
    /**
     * This function gets a random number
     * @param min The minimum number (inclusive)
     * @param max The maximum number (inclusive)
     * @return a random number between min and max
     */
    private static int getRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }
    
    /**
     * This function gets a random x coordinate
     * @return a random x coordinate
     */
    private static int rand_x()
    {
        return getRandomNumber(-ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_WIDTH.getVal());
    }

    /**
     * This function loads an image from the file system.
     * @param path of the image
     * @return an Image from the path.
     */
    private static Image load_image(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            return new Image(fis);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERRR");
            return null;
        }
    }

}
