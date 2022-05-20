package model.display;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import model.logic.InputHandler;
import model.logic.Passenger;
import model.logic.Road;
import model.logic.Sprite;
import model.logic.Taxi;
import model.utility.ESettings;

// This class should only be responsible for animation and not logic if possible.
public class AnimationHandler extends AnimationTimer {
    private GraphicsContext gc = null;
    private SnapshotParameters param = null;
    // speed
    private int speedAdjust = -5;
    private ArrayList<Sprite> sprites = null;
    private Taxi taxi = null;
    private long buffer = 0;
    private long pickup_slowdown = 0;
    private boolean pickup_flag = false;
    private Image instanceImage = null;

    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        taxi = InputHandler.getTaxi();

    };

    public void cleanPage() {
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        gc.setFill(Color.rgb(71, 71, 71));
        gc.fillRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    public Image getInstanceImage(Sprite s)
    {
       return s.getMyImageView().snapshot(param, null);
    }

    // Called 60 times a second.
    @Override
    public void handle(long now) {
        cleanPage();
        buffer += 1;

        // Taxi modification.
        if (pickup_flag && pickup_slowdown < taxi.getSlowDown()) {
            pickup_slowdown += 1;
            speedAdjust = -1;
        } else {
            pickup_slowdown = 0;
            speedAdjust = -5;
            pickup_flag = false;
        }

        if (buffer == 8) {
            taxi.swapImage();
            buffer = 0;
        }
        // Sprite drawing and animation
        for (Sprite sprite : sprites) {
            
            sprite.setX(sprite.getX() + speedAdjust);
            gc.drawImage(getInstanceImage(sprite), sprite.getX(), sprite.getY());

      
            // Handle Taxi touching passenger here.
            if (sprite instanceof Passenger) { // Smelly code
                Passenger passenger = (Passenger) sprite;

                if (taxi.intersects(passenger)) {
                    if (InputHandler.pickupAttempted()) {
                        System.out.println("Attempted Pickup recorded");
                        pickup_flag = true;
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
        gc.drawImage(getInstanceImage(taxi),taxi.getX(),taxi.getY());

    }
}
