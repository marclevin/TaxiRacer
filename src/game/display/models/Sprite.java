package game.display.models;

import java.io.Serializable;

import game.logic.Acceptor;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * This super class represents a sprite, which constitutes most of the game objects
 */
public abstract class Sprite implements Serializable, Acceptor
{
    protected transient int x,y;
    protected transient BoundBox myBound;
    protected transient Image myImage = null;
    protected transient ImageView myImageView =null;
    private transient SnapshotParameters param = null;
    public Sprite(int x, int y)
    {
        this.x = x;
        this.y = y;
        myImageView = new ImageView();
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        myBound = new BoundBox(0, 0, x, y);
    };


    /**
     * Checks for intersection with another sprite.
     * @param other The other sprite to check.
     */
    public boolean intersects(Sprite other)
    {
        return myBound.intersects(other.myBound);
    };


    /**
     * Sets the image of the sprite.
     * @param image The image to be set.
     */
    public void setImage(Image image)
    {
        myImage = image;
        myImageView.setImage(myImage);
        myImageView.setPreserveRatio(true);
        
    }

    /**
     * Gets the ImageView of the sprite.
     * @return The ImageView of the sprite.
     */
    public ImageView getMyImageView()
    {
        return myImageView;
    }

    /**
     * Gets the BoundBox of the sprite.
     * @return The BoundBox of the sprite.
     */
    public BoundBox getBoundary()
    {
        return myBound;
    }
    /**
     * Gets the X coordinate of the sprite.
     * @return The X coordinate of the sprite.
     */
    public int getX()
    {
        return x;
    }
    /**
     * Gets the Y coordinate of the sprite.
     * @return The Y coordinate of the sprite.
     */
    public int getY()
    {
        return y;
    }
    /**
     * Sets the X coordinate of the sprite.
     * @param x The new X coordinate of the sprite.
     */
    public void setX(int x)
    {
        this.x = x;
        this.myBound.setX(x);
    }
    /**
     * Sets the Y coordinate of the sprite.
     * @param y The new Y coordinate of the sprite.
     */
    public void setY(int y)
    {
        this.y = y;
        this.myBound.setY(y);
    }


}