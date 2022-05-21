package model.display;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import model.logic.InputHandler;
import model.logic.PassengerPool;
import model.logic.Road;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.EPositions;
import model.utility.ESettings;

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
        Image road_piece_image = load_image("img\\road_piece.png");
        Image taxi_prime = load_image("img\\taxi_move_1.png");
        Image taxi_second = load_image("img\\taxi_move_2.png");
        Image passenger_image = load_image("img\\passenger.png");
        ArrayList<Sprite> loadedSprites = new ArrayList<Sprite>();
        loadedSprites.add(new Road(ESettings.PRIMARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));
        loadedSprites.add(new Road(ESettings.SECONDARY_ROAD_X.getVal(), ESettings.ROAD_Y.getVal(), road_image));
        loadedSprites.add(new Road(ESettings.SECONDARY_ROAD_PIECE_X.getVal(), ESettings.ROAD_Y.getVal(), road_piece_image));
        loadedSprites.add(new Road(ESettings.PRIME_ROAD_PIECE_X.getVal(), ESettings.ROAD_Y.getVal(), road_piece_image));

        // Maybe add a start anim here.
        Taxi taxi = new Taxi(0,ESettings.TAXI_INIT_Y.getVal(), taxi_prime,taxi_second);
        taxi.scale(EPositions.values()[2]);
        InputHandler.setTaxi(taxi);
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
