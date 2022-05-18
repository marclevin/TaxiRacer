package model.logic;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.utility.BoundBox;

public abstract class Sprite
{
    protected int x,y;
    protected BoundBox myBound;
    protected Image myImage = null;
    protected ImageView myImageView =null;
    public Sprite(int x, int y)
    {
        this.x = x;
        this.y = y;
        myImageView = new ImageView();
        myBound = new BoundBox(0, 0, x, y);
        myBound.setX(x);
        myBound.setY(y);
    };

    public Sprite(int x, int y, Image image)
    {
        this(x,y);
        setImage(image);
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
        myBound.setWidth(image.getWidth());
        myBound.setHeight(image.getHeight());
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
    }
    public void setY(int y)
    {
        this.y = y;
    }


}