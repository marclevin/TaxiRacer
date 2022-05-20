package model.logic;

import javafx.scene.input.KeyEvent;
import model.utility.EPositions;

public final class InputHandler {

    private static Taxi taxi = null;
    private static EPositions position = EPositions.SECOND_LANE; // By Default we will start in the second lane.
    private static int positionMarker = 2;
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

    public static Taxi getTaxi()
    {
        return taxi;
    }

    public static void processKeyPress(KeyEvent event)
    {
            if (taxi == null)
            return;
        switch (event.getCode())
        {
            case UP:
                if (positionMarker > 0)
                {
                    positionMarker--;
                    position = EPositions.values()[positionMarker];
                    taxi.scale(position);
                    
                }
                break;
            case DOWN:
            if (positionMarker < 3)
            {
                positionMarker++;
                position = EPositions.values()[positionMarker];
                taxi.scale(position);
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
            default:
                break;  // do nothing, like a chad
        }
    }
    
}
