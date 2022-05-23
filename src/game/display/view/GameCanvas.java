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
import game.utility.ESettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class GameCanvas extends Canvas {
    private GraphicsContext gc = null;
    private ArrayList<Sprite> sprites = null;
    private AnimationHandler handler = null;
    private Taxi taxi = null;

    public GameCanvas() {
        this(ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    // could do loading logic here.
    public GameCanvas(double width, double height) {
        super(width, height);
        gc = this.getGraphicsContext2D();

    };

    public void initCanvas() {
        sprites = load_fresh_sprites();
        handler = new AnimationHandler(gc, sprites);
    }

    public void runAnimator() {
        handler.start();
    }

    public void stopAnimator() {
        handler.stop();
        handler.resetGame();
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    public boolean isGameRunning() {
        return handler.isRunning();
    }

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
        
       

        Pothole pothole_first = new Pothole(ESettings.PRIMARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal()+20);
        Pothole pothole_second = new Pothole(ESettings.SECONDARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal()+75);
        Pothole pothole_third = new Pothole(ESettings.TERTIARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal()+115);
        Pothole pothole_forth = new Pothole(ESettings.QUATERNARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal()+175);
        pothole_first.setImage(pothole_image);
        pothole_second.setImage(pothole_image);
        pothole_third.setImage(pothole_image);
        pothole_forth.setImage(pothole_image);
        

        loadedSprites.add(primary_road);
        loadedSprites.add(secondary_road);
        loadedSprites.add(tertiary_road);
        loadedSprites.add(quaternary_road);

        loadedSprites.add(pothole_first);
        loadedSprites.add(pothole_second);
        loadedSprites.add(pothole_third);
        loadedSprites.add(pothole_forth);

        // Maybe add a start anim here.
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

    private Image load_image(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            return new Image(fis);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERRR");
            return null;
        }
    }

}
