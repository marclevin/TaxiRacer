package model.utility;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import model.logic.Taxi;

public final class InputHandler {

    private static Taxi mainTaxi = null;
    private static ImageView taxiImageView = null;
    private static EPositions position = EPositions.SECOND_LANE; // By Default we will start in the second lane.
    private static int positionMarker = 2;
    private InputHandler(){};


    public static void setTaxi(Taxi s)
    {
        try 
        {
            mainTaxi = s;
            taxiImageView = mainTaxi.getMyImageView();
        }
        catch(Exception e)
        {
            System.out.println("Error: " + e.getMessage());
        }
        taxiImageView.setPreserveRatio(true);
    }

    public static Taxi getMainTaxi()
    {
        return mainTaxi;
    }

    public static void processKeyPress(KeyEvent event)
    {
        if (taxiImageView == null)
            return;

        switch (event.getCode())
        {
            case UP:
                if (positionMarker > 0)
                {
                    positionMarker--;
                    position = EPositions.values()[positionMarker];
                    mainTaxi.setY(position.getLocation());
                    taxiImageView.setFitWidth(position.getWidthScale());
                    taxiImageView.setFitHeight(position.getHeightScale());
                }
                break;
            case DOWN:
            if (positionMarker < 3)
            {
                positionMarker++;
                position = EPositions.values()[positionMarker];
                mainTaxi.setY(position.getLocation());
                taxiImageView.setFitWidth(position.getWidthScale());
                taxiImageView.setFitHeight(position.getHeightScale());
            }
                break;

            case LEFT:
                mainTaxi.setX(mainTaxi.getX() - 15);
                break;

            case RIGHT:
                mainTaxi.setX(mainTaxi.getX() + 15);
                break;
            default:
                break;  // do nothing, like a chad
        }
    }
    
}
