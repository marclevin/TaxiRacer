package model.logic;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.utility.BoundBox;

public abstract class Sprite
{
    protected int x,y;
    protected int xSpeed,ySpeed;
    protected BoundBox myBound;
    protected Image myImage;
    protected ImageView myImageView;
    public Sprite(int x, int y)
    {
        this.x = x;
        this.y = y;
        myBound = new BoundBox(0,0,0,0);
        myImageView = new ImageView();
    };

    public void setPosition(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public void setSpeed(int xSpeed, int ySpeed)
    {
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
    };

    public void changeSpeed(int xSpeed, int ySpeed)
    {
        this.xSpeed += xSpeed;
        this.ySpeed += ySpeed;
    };

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
        myBound.setX(x);
        myBound.setY(y);
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


}