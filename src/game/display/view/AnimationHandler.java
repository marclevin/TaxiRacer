package game.display.view;

import java.util.ArrayList;

import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.logic.SpriteVisitor;
import game.utility.EPolicePositions;
import game.utility.ESettings;
import game.utility.ETaxiPositions;
import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * This class is the defacto game, its loop is the main loop of the game.
 */
public class AnimationHandler extends AnimationTimer {

    // Variables
    private PassengerPool passengerPool = null;
    private GraphicsContext gc = null;
    private SnapshotParameters param = null;
    // This is the speed of the game
    private double speedAdjust = -7;
    private ArrayList<Sprite> sprites = null;
    private Taxi taxi = null;
    private Police police = null;

    // Frame counters
    private int frame_count, passenger_count = 0;
    private int pickup_count = 0;
    private int warning_buffer = 0;
    private int NOS_counter = 0;

    // This variable is used to restore the Taxi's pothole resistance after using NOS.
    private int anti_pothole = 0;
    
    // Visitor Design Pattern
    private SpriteVisitor spriteVisitor = null;

    // Game stop checks
    private boolean stop_game = false;
    private boolean lost_game = false;

    // Fonts
    private Font common_font = new Font("Verdana", 20);
    private Font warning_font = new Font("Verdana", 40);

    /**
     * Constructor for the AnimationHandler class.
     * @param gc The GraphicsContext of the canvas.
     * @param sprites The ArrayList of sprites.
     */
    public AnimationHandler(GraphicsContext gc, ArrayList<Sprite> sprites) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        taxi = InputHandler.getTaxi();
        police = InputHandler.getPolice();
        passengerPool = PassengerPool.getInstance();
        spriteVisitor = new SpriteVisitor();
        anti_pothole = taxi.getPotholeResistance();
    };

    /**
     * This function cleans the GraphicsContext of the canvas.
     */
    private void cleanPage() {
        gc.clearRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
    }

    /**
     * This function renders a sprite on the GraphicsContext of the canvas.
     * @param s The sprite to render
     */
    private void render_sprite(Sprite s) {
        gc.drawImage(getInstanceImage(s), s.getX(), s.getY());
    }

    /**
     * This function resets the game, setting all actors back to their initial states.
     */
    public void resetGame()
    {
        stop_game = false;
        pickup_count = 0;
        passenger_count = 0;
        frame_count = 0;
        warning_buffer = 0;
        police.setX(-ESettings.SCENE_WIDTH.getVal());
        taxi.scale(ETaxiPositions.values()[2]);
        taxi.setX(0);
        InputHandler.begunGame();
        
    }

    /**
     * This function renders the scoreboard of the game, including instance taxi info.
     */
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
        // flashing colors to make players aware of their impending loss.
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
            if (currentDistance > -200) {
                // This warning buffer is just to ensure that the message doesn't display infinitely.
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
        for (Passenger p : spriteVisitor.getCleanList()) {
            sprites.remove(p);
            taxi.addCash(p.getCash());
            passengerPool.releasePassenger(p);
            pickup_count++;
            passenger_count--;
        }
        // Passenger cleanup
        spriteVisitor.getCleanList().clear();
    }

    /**
     * This function gets the instant snapshot of the ImageView of the sprite as that is what we are manipulating with scaling.
     * @param s the sprite to get the image from.
     * @return the image of the sprite.
     */
    private Image getInstanceImage(Sprite s) {
        return s.getMyImageView().snapshot(param, null);
    }


    /**
     * This function checks for sprite intersection and uses the visitor design pattern to accomplish this task.
     * @param sprite to check for intersection.
     */
    private void checkSpriteTouch(Sprite sprite) {
            if (taxi.intersects(sprite)) {
                sprite.accept(spriteVisitor);
                
            }
        }


    /**
     * This function is called 60 times a second and is the de facto main game loop.
     */
    @Override
    public void handle(long now) {
        // Local variable setup.
        
        // Cleaning
        cleanPage();
        cleanPassenger();

        // Loss / Win Detection
        if (stop_game)
        {
            if (lost_game) {
            gc.setFont(new Font("Verdana", 30));
            gc.setFill(Color.BLACK);
            gc.fillText("You lost!\nDon't give up!\nPurchase NITRO if you're getting caught often.\nPlease press ENTER to continue...", 50, 200);
            InputHandler.endGame();
            }
            else {
                gc.setFont(new Font("Verdana", 30));
                gc.setFill(Color.BLACK);
                gc.fillText("You WIN!\nCongratulations!\nYou evaded the police.\nPlease press ENTER to continue...", 50, 200);
                InputHandler.endGame();
            }
        } else {

        // Scoreboard rendering.
        render_scoreboard();

        // Frame increase
        frame_count++;

        // This handles the upgrade system and allows the taxi to slow down and speed up.
        if (taxi.getPunishment() > 0) {
            taxi.minusPunishment();
            speedAdjust = -5 - taxi.getEngineUpgrade();
            police.changeDistance(2);
        } else {speedAdjust = -7 - taxi.getEngineUpgrade();}
        // At any point if they use the NOS button.
        if (taxi.hasUsedNos())
        {
            NOS_counter++;
            if (NOS_counter < 240)
            {
                taxi.setPotHoleResistance(4);
                speedAdjust = -20;
                police.changeDistance(-12);
            } else {
                NOS_counter = 0;
                taxi.setNOS(false);
                taxi.setUsedNos(false);
                // it's fun not to be burded by potholes when going fast.
                taxi.setPotHoleResistance(anti_pothole);
            }
        }

        // Sprite drawing and animation
        for (Sprite sprite : sprites) {

            sprite.setX((int) Math.ceil(sprite.getX() + speedAdjust));

            checkSpriteTouch(sprite);

            // Only render the sprite if its going to be behind the taxi & police.
            if (!spriteVisitor.getBottomRenderList().contains(sprite))
                render_sprite(sprite);
            // Out of bounds checker
            if (sprite.getX() < -ESettings.SCENE_WIDTH.getVal()) {
                sprite.setX((int) ESettings.SCENE_WIDTH.getVal()); // Clean cast
            }
        }
        // Rendering the taxi.
        render_sprite(taxi);

        // Don't render the police unless we have to.
        if (!police.isHidden())
        {
            police.setX(police.getDistance()-200);
            render_sprite(police);
        }
        // Taxi out of bounds, the 80 is fairly arbitrary.
        if (taxi.getX() >= ESettings.SCENE_WIDTH.getVal()-80)
        {
            taxi.setX(ESettings.SCENE_WIDTH.getVal()-80);
        }

        // Taxi out of bounds, again the 140 is purely arbitrary.
        if (taxi.getX() <= -140)
        {
            taxi.setX(-140);
        }

        // LOSE CONDITION
        if (taxi.intersects(police))
        {
            stop_game = true;
            lost_game = true;
        }

        // WIN CONDITION
        if (pickup_count == 100)
        {
            stop_game = true;
            lost_game = false;
        }


        // Clever rendering to ensure that the taxi is always realistically placed.
        for (Passenger p : spriteVisitor.getBottomRenderList()) {
            render_sprite(p);
        }
        spriteVisitor.getBottomRenderList().clear();

        // Every 8 frames.
        if (frame_count % 8 == 0) {
            police.changeDistance(-4);
            taxi.swapImage();
            police.swapImage();
        }

        // Every 60 frames.
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
