package model.display;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import model.logic.InputHandler;
import model.logic.Passenger;
import model.logic.PassengerPool;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.ESettings;

// This class should only be responsible for animation and not logic if possible.
public class AnimationHandler extends AnimationTimer {
    private PassengerPool passengerPool = null;


    private GraphicsContext gc = null;
    private SnapshotParameters param = null;
    // speed
    private double speedAdjust = -5;
    private ArrayList<Sprite> sprites = null;
    private Taxi taxi = null;
    private long buffer = 0;
    private long pickup_slowdown = 0;
    private boolean pickup_flag = false;
    private int frame_count, passenger_count = 0;
    private ArrayList<Passenger> cleanup_list = null;

    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        taxi = InputHandler.getTaxi();
        passengerPool = PassengerPool.getInstance();
        cleanup_list = new ArrayList<Passenger>();
    };

    public void cleanPage() {
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        gc.setFill(Color.rgb(71, 71, 71));
        gc.fillRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    public Image getInstanceImage(Sprite s) {
        return s.getMyImageView().snapshot(param, null);
    }

    // Called 60 times a second.
    @Override
    public void handle(long now) {
        cleanPage();
        frame_count++;
        buffer++;

        // Passenger start.


        for (Passenger p : cleanup_list)
        {
            sprites.remove(p);
            passengerPool.releasePassenger(p);
        }
        cleanup_list.clear();
        if (frame_count == 60 && passenger_count < 10) {
           sprites.add(passengerPool.aquirePassenger());
           passenger_count++;
        }


        // Taxi modification.
        if (pickup_flag && pickup_slowdown < taxi.getSlowDown()) {
            if (pickup_slowdown % 15 == 0) {
                speedAdjust -= 0.5;
            }
            pickup_slowdown += 1;
        } else {
            pickup_slowdown = 0;
            speedAdjust = -15;
            pickup_flag = false;
        }

        if (frame_count % 8 == 0) {
            taxi.swapImage();
        }
        // Sprite drawing and animation


        for (Sprite sprite : sprites) {

            sprite.setX((int) Math.ceil(sprite.getX() + speedAdjust));
            gc.drawImage(getInstanceImage(sprite), sprite.getX(), sprite.getY());
            gc.setFill(Color.RED);

            // Handle Taxi touching passenger here.
            if (sprite instanceof Passenger) { // Smelly code
                Passenger passenger = (Passenger) sprite;



                if (taxi.intersects(passenger)) {
                    if (InputHandler.pickupAttempted()) {
                        System.out.println("Attempted Pickup recorded");
                        pickup_flag = true;
                        speedAdjust = -1;
                        // Add passenger to clean up block.
                        cleanup_list.add(passenger);
                        passenger_count--;
                        // Passenger has been picked up, let's add the target to a remove list and
                        // release it back to the passenger pool.
                        InputHandler.pickupBlock();
                    }
                }
            }

            if (sprite.getX() < -ESettings.SCENE_WIDTH.getVal()) {
                sprite.setX((int) ESettings.SCENE_WIDTH.getVal()); // Clean cast
            }
        }
        gc.drawImage(getInstanceImage(taxi), taxi.getX(), taxi.getY());
        
        if (frame_count == 60)
        {
            frame_count = 0;
        }
    }
}
