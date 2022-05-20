package model.display;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import model.logic.Passenger;
import model.logic.Road;
import model.logic.Settings;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.EPositions;
import model.utility.InputHandler;

public class GameCanvas extends Canvas {
    private GraphicsContext gc = null;
    private ArrayList<Sprite> sprites = null;
    private AnimationHandler handler = null;

    public GameCanvas() {
        this(Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
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
        loadedSprites.add(new Road(Settings.PRIMARY_ROAD_X, Settings.ROAD_Y, road_image));
        loadedSprites.add(new Road(Settings.SECONDARY_ROAD_X-19, Settings.ROAD_Y, road_image));
        loadedSprites.add(new Road(Settings.SECONDARY_ROAD_PIECE_X, Settings.ROAD_Y, road_piece_image));
        loadedSprites.add(new Road(Settings.PRIME_ROAD_PIECE_X-8, Settings.ROAD_Y, road_piece_image));


        Taxi taxi = new Taxi(0,Settings.TAXI_INIT_Y, taxi_prime,taxi_second);
        // By default we will start in lane 3.
        EPositions position = EPositions.values()[2];
        taxi.getMyImageView().setFitWidth(position.getWidthScale());
        taxi.getMyImageView().setFitHeight(position.getHeightScale());
        InputHandler.setTaxi(taxi);
        loadedSprites.add(taxi);

        
        loadedSprites.add(new Passenger(0,540,passenger_image));
        loadedSprites.add(new Passenger(0, 300, passenger_image));
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
