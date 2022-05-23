package game.logic;

import game.display.models.Police;
import game.display.models.Taxi;
import game.display.view.GameCanvas;
import game.utility.EPolicePositions;
import game.utility.ETaxiPositions;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * This class is responsible for handling the input from the user.
 */
public final class InputHandler {

    private static Taxi taxi = null;
    private static Stage mainStage = null;
    private static Scene upgradeScene = null;
    private static Police police = null;
    private static Boolean game_state = true;
    private static GameCanvas gameCanvas = null;
    private static ETaxiPositions taxi_position = ETaxiPositions.SECOND_LANE_TAXI;
    private static int marker = 2;
    private InputHandler(){};
    private static boolean attemptedPickup = false;


    /**
     * This method prevents passengers from being picked up.
     */
    public static void pickupBlock()
    {
        attemptedPickup = false;
    }

    /**
     * This method sets the main stage for the handler.
     * @param stage The stage to set.
     */
    public static void setMainStage(Stage stage)
    {
        mainStage = stage;
    }
    /**
     * This method sets the upgrade screen for the handler.
     * @param scene the scene to set.
     */
    public static void setUpgradeScene(Scene scene)
    {
        upgradeScene = scene;
    }

    /**
     * This method returns true if a pickup has been attempted.
     * @return true if a pickup has been attempted, false otherwise.
     */
    public static boolean pickupAttempted()
    {
    return attemptedPickup;
    }


    /**
     * This method sets the taxi for the handler
     * @param s The taxi to be set.
     */
    public static void setTaxi(Taxi s)
    {
            taxi = s;
    }

    /**
     * This method sets the canvas for the handler.
     * @param g the canvas to be set.
     */
    public static void setCanvas(GameCanvas g)
    {
        gameCanvas = g;
    }

    /**
     * This changes the internal state when a game should be over to prevent unwarranted input.
     */
    public static void endGame()
    {
        game_state = false;
    }

    /**
     * This allows input to be recorded
     */
    public static void begunGame()
    {
        game_state = true;
    }

    /**
     * This returns the handler's taxi instance
     * @return the instance of Taxi
     */
    public static Taxi getTaxi()
    {
        return taxi;
    }
    /**
     * This sets the handler's police instance
     * @param p the police instance
     */
    public static void setPolice(Police p)
    {
        police = p;
    }
    /**
     * This gets the handler's police instance
     * @return the police instance
     */
    public static Police getPolice()
    {
        return police;
    }

    /**
     * This method processes {@code KeyEvent} events from it's assigned parent.
     * @param event The event to process.
     */
    public static void processKeyPress(KeyEvent event)
    {
            if (taxi == null)
            return;
            if (police == null)
            return;
        attemptedPickup = false;
        switch (event.getCode())
        {
            case UP:
                if (marker > 0)
                {
                    marker--;
                    taxi_position = ETaxiPositions.values()[marker];
                    taxi.scale(taxi_position);
                    if (!police.isHidden())
                    {
                        police.scale(EPolicePositions.values()[marker]);
                    }
                    
                }
                break;
            case DOWN:
            if (marker < 3)
            {
                marker++;
                taxi_position = ETaxiPositions.values()[marker];
                taxi.scale(taxi_position);
                if (!police.isHidden())
                {
                    police.scale(EPolicePositions.values()[marker]);
                }
            }
                break;
            case LEFT:
                taxi.setX(taxi.getX() - 15);
                break;

            case RIGHT:
                taxi.setX(taxi.getX() + 15);
                break;

            case SPACE:
                   attemptedPickup = true;
                   break;

            case ENTER:
                if (!game_state)
                {
                gameCanvas.stopAnimator();
                
                game_state = true;
                mainStage.setScene(upgradeScene);
                }
                break;
            case ESCAPE:
                if (game_state)
                {
                    gameCanvas.stopAnimator();
                    game_state = true;
                    mainStage.setScene(upgradeScene);
                }
                break;

            case E:
                if (taxi.hasNOS() && !taxi.hasUsedNos())
                {
                    taxi.setUsedNos(true);
                }
            default:
                break;
        }
    }
    
}
