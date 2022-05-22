package game.display.view;

import java.util.ArrayList;

import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.utility.EPassenger;
import game.utility.EPolicePositions;
import game.utility.ESettings;
import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// This class should only be responsible for animation and not logic if possible.
public class AnimationHandler extends AnimationTimer {
    private PassengerPool passengerPool = null;

    private GraphicsContext gc = null;
    private SnapshotParameters param = null;
    // speed
    private double speedAdjust = -5;
    private ArrayList<Sprite> sprites = null;
    private Taxi taxi = null;
    private Police police = null;
    private long pickup_slowdown = 0;
    private boolean pickup_flag = false;
    private int frame_count, passenger_count = 0;
    private ArrayList<Passenger> cleanup_list = null;
    private int pickup_count = 0;
    private int warning_buffer = 0;

    private boolean stop_game = false;

    private ArrayList<Passenger> bottom_render_list = null;
    private Font common_font = new Font("Verdana", 20);
    private Font warning_font = new Font("Verdana", 40);

    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        taxi = InputHandler.getTaxi();
        police = InputHandler.getPolice();
        passengerPool = PassengerPool.getInstance();
        cleanup_list = new ArrayList<Passenger>();
        bottom_render_list = new ArrayList<Passenger>();
    };

    private void cleanPage() {
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    private void render_sprite(Sprite s) {
        gc.drawImage(getInstanceImage(s), s.getX(), s.getY());
    }

    public boolean isRunning()
    {
        System.out.println(!this.stop_game);
        return !this.stop_game;
    }

    private void render_boundBox(Sprite s)
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(s.getBoundary().getX(), s.getBoundary().getY(), s.getBoundary().getWidth(), s.getBoundary().getHeight());
    }

    private void render_scoreboard() {
        Color flashColor = null;
        int currentDistance = police.getDistance();
        gc.setFont(common_font);
        gc.setFill(Color.BLACK);
        gc.fillText("Taxi Information:", 10, 20);
        gc.setFill(Color.DARKGREEN);
        gc.fillText(String.format("Wallet: R%.2f", taxi.getWallet()), 10, 40);
        gc.setFill(Color.DARKRED);
        gc.fillText(String.format("Passengers: %d", pickup_count), 10, 60);
        if (police.isHidden()) {
            if (frame_count < 30) {
                flashColor = Color.RED;
            } else {
                flashColor = Color.BLUE;
            }
            gc.setFill(Color.BLACK);
            gc.fillRect(10, 90, 120, 80);
            gc.setFill(flashColor);
            gc.fillRect(15, 95, 110, 70);
            gc.fillText(String.format("Police Distance: %dM", -police.getDistance()), 10, 80);
            police.setX(20);
            police.scale(EPolicePositions.POLICE_DISPLAY);
            render_sprite(police);
            if (currentDistance > -500) {
                warning_buffer++;
                if (warning_buffer < 300) {
                    gc.setFont(warning_font);
                    gc.fillText(String.format("The police are less than %dM away!\nSPEED UP!",-currentDistance), 150, 120);
                } else {
                    warning_buffer = 0;
                }
            }

        }
    }

    /**
     * This function cleans up collected passengers and adds their cash to the taxi.
     */
    private void cleanPassenger() {
        for (Passenger p : cleanup_list) {
            sprites.remove(p);
            // text_hang.add(new TempText(p.getX(), p.getY(), String.format("+R%.2f",
            // p.getCash()),p.getEPassenger()));
            taxi.addCash(p.getCash());
            passengerPool.releasePassenger(p);
            pickup_count++;
        }
        cleanup_list.clear();
    }

    private Image getInstanceImage(Sprite s) {
        return s.getMyImageView().snapshot(param, null);
    }

    private void checkPassengerIntersection(Sprite sprite) {
        if (sprite instanceof Passenger) { // Smelly code
            Passenger passenger = (Passenger) sprite;
            if (taxi.intersects(passenger) || police.intersects(passenger)) {
                if (passenger.getEPassenger() == EPassenger.PASSENGER_BOTTOM) {
                    bottom_render_list.add(passenger);
                }
                if (InputHandler.pickupAttempted()) {
                    pickup_flag = true;
                    speedAdjust = -1;
                    cleanup_list.add(passenger);
                    passenger_count--;
                    InputHandler.pickupBlock();
                }
            }
        }
    }

    // Called 60 times a second.
    @Override
    public void handle(long now) {
        // Local variable setup.
        
        
        cleanPage();
        cleanPassenger();

        if (stop_game)
        {
            // Lose conditions go here.
            gc.setFont(new Font("Verdana", 80));
            gc.setFill(Color.BLACK);
            gc.fillText("You lost!\n Please press ENTER to continue...", ESettings.SCENE_WIDTH.getVal() / 2, ESettings.SCENE_HEIGHT.getVal() / 2);
            InputHandler.lostGame();
        } else {
        render_scoreboard();
        frame_count++;

        // Taxi modification.
        if (pickup_flag && pickup_slowdown < taxi.getSlowDown()) {
            if (pickup_slowdown % 15 == 0) {
                speedAdjust -= 0.5;
            }
            pickup_slowdown += 1;
            police.changeDistance(1);
        } else {
            pickup_slowdown = 0;
            speedAdjust = -7;
            pickup_flag = false;
        }

        if (frame_count % 8 == 0) {
            taxi.swapImage();
            police.swapImage();
        }
        // Sprite drawing and animation

        for (Sprite sprite : sprites) {

            sprite.setX((int) Math.ceil(sprite.getX() + speedAdjust));

            checkPassengerIntersection(sprite);

            // Only render the sprite if its going to be behind the taxi & police.
            if (!bottom_render_list.contains(sprite))
                render_sprite(sprite);

            // Out of bounds checker
            if (sprite.getX() < -ESettings.SCENE_WIDTH.getVal()) {
                sprite.setX((int) ESettings.SCENE_WIDTH.getVal()); // Clean cast
            }
        }
        render_sprite(taxi);
        if (!police.isHidden())
        {
            police.setX(police.getDistance()-300);
            render_sprite(police);
        }
        // Taxi out of bounds
        if (taxi.getX() >= ESettings.SCENE_WIDTH.getVal()-80)
        {
            taxi.setX(ESettings.SCENE_WIDTH.getVal()-80);
        }

        if (taxi.getX() <= -140)
        {
            taxi.setX(-140);
        }

        if (taxi.intersects(police))
        {
            // LOSE CONDITION
            stop_game = true;
        }



        for (Passenger p : bottom_render_list) {
            render_sprite(p);
        }
        bottom_render_list.clear();

        // Text pop up for cash received. may rework this 2022/05/22
        /*
         * ArrayList<TempText> temp = new ArrayList<TempText>();
         * for (TempText t : text_hang) {
         * t.tickDown();
         * if (t.getHold() != 0)
         * {
         * gc.setFill(Color.GREEN);
         * gc.setFont(new Font("Arial", 20));
         * gc.fillText(t.getText(), t.getX(), t.getY());
         * temp.add(t);
         * }
         * }
         * text_hang.clear();
         * text_hang = temp;
         */
        if (frame_count % 8 == 0) {
            police.changeDistance(1);
        }

        if (frame_count == 60) {
            InputHandler.pickupBlock();
            frame_count = 0;
            if (passenger_count < 10) {
                sprites.add(passengerPool.aquirePassenger());
                passenger_count++;
            }
        }

    }
}
}
