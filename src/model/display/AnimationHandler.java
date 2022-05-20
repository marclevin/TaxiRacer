package model.display;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import model.logic.Passenger;
import model.logic.Road;
import model.logic.Settings;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.InputHandler;

// This class should only be responsible for animation and not logic if possible.
public class AnimationHandler extends AnimationTimer {
    private GraphicsContext gc = null;
    private SnapshotParameters param = null;
    // speed
    private int speedAdjust = -5;
    private ArrayList<Sprite> sprites = null;
    private Taxi mainTaxi = null;
    private long buffer = 0;
    private long pickup_slowdown = 0;
    private boolean pickup_flag = false;

    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        mainTaxi = InputHandler.getTaxi();

    };

    // Called 60 times a second.
    @Override
    public void handle(long now) {
        buffer += 1;
        if (pickup_flag && pickup_slowdown < mainTaxi.getSlowDown()) {
            pickup_slowdown += 1;
            speedAdjust = -1;
        } else {
            pickup_slowdown = 0;
            speedAdjust = -5;
            pickup_flag = false;
        }
        param.setFill(Color.TRANSPARENT);
        gc.clearRect(0, 0, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        gc.setFill(Color.rgb(71, 71, 71));
        gc.fillRect(0, 0, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        for (Sprite sprite : sprites) {
            Image instanceImage = sprite.getMyImageView().snapshot(param, null); // Instance Image.
            gc.drawImage(instanceImage, sprite.getX(), sprite.getY());
            if (sprite instanceof Road == false) {
                gc.fillRect(sprite.getBoundary().getX(), sprite.getBoundary().getY(), sprite.getBoundary().getWidth(),
                        sprite.getBoundary().getHeight());
            }
            // Handle Taxi touching passenger here.
            if (sprite instanceof Passenger) { // Smelly code
                Passenger passenger = (Passenger) sprite;

                if (mainTaxi.intersects(passenger)) {
                    if (InputHandler.pickupAttempted()) {
                        System.out.println("Attempted Pickup recorded");
                        pickup_flag = true;
                        // Passenger has been picked up, let's add the target to a remove list and release it back to the passenger pool.
                        InputHandler.pickupBlock();
                    }
                }
            }

            // The taxi shouldn't move here.
            if (!sprite.equals(mainTaxi)) {
                sprite.setX(sprite.getX() + speedAdjust);
            } else {
                // Clever tool to make it look like the wheels are moving.
                if (buffer == 8) {
                    mainTaxi.swapImage();
                    buffer = 0;
                }
            }
            if (sprite.getX() < -Settings.SCENE_WIDTH) {
                sprite.setX((int) Settings.SCENE_WIDTH); // Clean cast
            }
        }

    }
}
