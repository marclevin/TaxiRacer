package model.utility;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;

public final class InputImg {

    private static ImageView img;
    private static EPositions position = EPositions.SECOND_LANE;
    private static int positionMarker = 2;
    private InputImg(){};


    public static void setImageView(ImageView i)
    {
        img = i;
        img.setPreserveRatio(true);
    }

    public static void processKeyPress(KeyEvent event)
    {
        if (img == null)
            return;

        switch (event.getCode())
        {
            case UP:
                if (positionMarker > 0)
                {
                    positionMarker--;
                    position = EPositions.values()[positionMarker];
                    img.setY(position.getLocation());
                    img.setFitWidth(position.getWidthScale());
                    img.setFitHeight(position.getHeightScale());
                }
                break;
            case DOWN:
            if (positionMarker < 3)
            {
                positionMarker++;
                position = EPositions.values()[positionMarker];
                img.setY(position.getLocation());
                img.setFitWidth(position.getWidthScale());
                img.setFitHeight(position.getHeightScale());
            }
                break;

            case LEFT:
                img.setX(img.getX() - 5);
                break;

            case RIGHT:
                img.setX(img.getX() + 5);
                break;
            default:
                break;  // do nothing, like a chad
        }
    }
    
}
