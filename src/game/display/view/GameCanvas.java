package game.display.view;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import game.display.models.Police;
import game.display.models.Road;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.utility.EPositions;
import game.utility.ESettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class GameCanvas extends Canvas {
    private GraphicsContext gc = null;
    private ArrayList<Sprite> sprites = null;
    private AnimationHandler handler = null;

    public GameCanvas() {
        this(ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    // could do loading logic here.
    public GameCanvas(double width, double height) {
        super(width, height);
        gc = this.getGraphicsContext2D();
        sprites = load_fresh_sprites();
        handler = new AnimationHandler(gc, sprites);
    };

    public void runAnimator() {
        handler.start();
    }

    public void stopAnimator() {
        handler.stop();
    }

   

    private ArrayList<Sprite> load_fresh_sprites() {
        // Image loading.
        Image road_image = load_image("img\\road_try.png");
        Image taxi_prime = load_image("img\\taxi_move_1.png");
        Image taxi_second = load_image("img\\taxi_move_2.png");
        Image passenger_image = load_image("img\\passenger.png");
        Image police_prime = load_image("img\\police_move_1.png");
        Image police_second = load_image("img\\police_move_2.png");
        ArrayList<Sprite> loadedSprites = new ArrayList<Sprite>();
        loadedSprites.add(new Road(ESettings.PRIMARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));
        loadedSprites.add(new Road(ESettings.QUATERNARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));
        loadedSprites.add(new Road(ESettings.TERTIARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));
        loadedSprites.add(new Road(ESettings.SECONDARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));

        // Maybe add a start anim here.
        Taxi taxi = new Taxi(0,ESettings.TAXI_INIT_Y.getVal(), taxi_prime,taxi_second);
        Police police = new Police(-1000, ESettings.TAXI_INIT_Y.getVal(), police_prime,police_second);
        taxi.scale(EPositions.values()[2]);
        police.scale(EPositions.values()[2]);
        InputHandler.setTaxi(taxi);
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
