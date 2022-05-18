package model.display;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
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

    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        mainTaxi = InputHandler.getMainTaxi();

    };

    // Called 60 times a second.
    @Override
    public void handle(long now) {
        buffer +=1;
        param.setFill(Color.TRANSPARENT);
        gc.clearRect(0, 0, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        gc.setFill(Color.RED);
        gc.fillRect(0, 0, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        for (Sprite sprite : sprites) {
            Image instanceImage = sprite.getMyImageView().snapshot(param, null); // Instance Image.
            gc.drawImage(instanceImage, sprite.getX(), sprite.getY());
            if (!sprite.equals(mainTaxi)) {
            sprite.setX(sprite.getX() + speedAdjust);
            }
            else
            {
             if (buffer==8){mainTaxi.swapImage(); buffer = 0;}
            }
            if (sprite.getX() < -Settings.SCENE_WIDTH) {
                sprite.setX((int) Settings.SCENE_WIDTH); // Clean cast
            }
        }

    }
}
