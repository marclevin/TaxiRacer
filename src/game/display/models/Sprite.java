package game.display.models;

import java.io.Serializable;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public abstract class Sprite implements Serializable
{
    protected transient int x,y;
    protected BoundBox myBound;
    protected transient Image myImage = null;
    protected transient ImageView myImageView =null;
    private transient SnapshotParameters param = null;
    public Sprite(int x, int y, Image image)
    {
        this.x = x;
        this.y = y;
        myImageView = new ImageView();
        setImage(image);
        param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        myBound = new BoundBox(0, 0, x, y);
    };

    public Sprite()
    {
        this(0, 0, null);
    }


    public void setPosition(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public boolean intersects(Sprite other)
    {
        return myBound.intersects(other.myBound);
    };


    public void setImage(Image image)
    {
        myImage = image;
        myImageView.setImage(myImage);
        myImageView.setPreserveRatio(true);
        
    }

    public ImageView getMyImageView()
    {
        return myImageView;
    }

    public BoundBox getBoundary()
    {
        return myBound;
    }
    public int getX()
    {
        return x;
    }
    public int getY()
    {
        return y;
    }
    public void setX(int x)
    {
        this.x = x;
        this.myBound.setX(x);
    }
    public void setY(int y)
    {
        this.y = y;
        this.myBound.setY(y);
    }


}