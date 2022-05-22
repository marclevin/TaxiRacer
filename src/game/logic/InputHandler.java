package game.logic;

import game.display.models.Police;
import game.display.models.Taxi;
import game.display.view.GameCanvas;
import game.utility.EPolicePositions;
import game.utility.ETaxiPositions;
import javafx.scene.input.KeyEvent;

public final class InputHandler {

    private static Taxi taxi = null;
    private static Police police = null;
    private static Boolean game_state = true;
    private static GameCanvas gameCanvas = null;
    private static ETaxiPositions taxi_position = ETaxiPositions.SECOND_LANE_TAXI;
    private static int marker = 2;
    private InputHandler(){};
    private static boolean attemptedPickup = false;


    public static void pickupBlock()
    {
        attemptedPickup = false;
    }

    public static boolean pickupAttempted()
    {
    return attemptedPickup;
    }


    public static void setTaxi(Taxi s)
    {
            taxi = s;
    }

    public static void setCanvas(GameCanvas g)
    {
        gameCanvas = g;
    }

    public static void lostGame()
    {
        game_state = false;
    }


    public static Taxi getTaxi()
    {
        return taxi;
    }
    public static void setPolice(Police p)
    {
        police = p;
    }
    public static Police getPolice()
    {
        return police;
    }

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

            case ENTER:
                if (!game_state)
                {
                gameCanvas.stopAnimator();
                }
                break;
            default:
                break;  // do nothing, like a chad
        }
    }
    
}
