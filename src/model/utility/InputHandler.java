package model.utility;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import model.logic.Sprite;

public final class InputHandler {

    private static Sprite mainSprite = null;
    private static ImageView spriteImage = null;
    private static EPositions position = EPositions.SECOND_LANE; // By Default we will start in the second lane.
    private static int positionMarker = 2;
    private InputHandler(){};


    public static void setSprite(Sprite s)
    {
        try 
        {
            mainSprite = s;
            spriteImage = mainSprite.getMyImageView();
        }
        catch(Exception e)
        {
            System.out.println("Error: " + e.getMessage());
        }
        spriteImage.setPreserveRatio(true);
    }

    public static Sprite getMainSprite()
    {
        return mainSprite;
    }

    public static void processKeyPress(KeyEvent event)
    {
        if (spriteImage == null)
            return;

        switch (event.getCode())
        {
            case UP:
                if (positionMarker > 0)
                {
                    positionMarker--;
                    position = EPositions.values()[positionMarker];
                    mainSprite.setY(position.getLocation());
                    spriteImage.setFitWidth(position.getWidthScale());
                    spriteImage.setFitHeight(position.getHeightScale());
                }
                break;
            case DOWN:
            if (positionMarker < 3)
            {
                positionMarker++;
                position = EPositions.values()[positionMarker];
                mainSprite.setY(position.getLocation());
                spriteImage.setFitWidth(position.getWidthScale());
                spriteImage.setFitHeight(position.getHeightScale());
            }
                break;

            case LEFT:
                mainSprite.setX(mainSprite.getX() - 15);
                break;

            case RIGHT:
                mainSprite.setX(mainSprite.getX() + 15);
                break;
            default:
                break;  // do nothing, like a chad
        }
    }
    
}
