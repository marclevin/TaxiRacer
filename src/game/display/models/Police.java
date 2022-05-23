package game.display.models;

import game.logic.Visitor;
import game.utility.EPolicePositions;
import javafx.scene.image.Image;

public class Police extends Sprite {
    private boolean isPrime = true;
    private int distance = -1080;
    private Image primeImage, secondImage = null;

    /**
     * @param x
     * @param y
     * @param image
     */
    public Police(int x, int y) {
        super(x, y);
    }

    public void setImageSet(Image prime, Image secondary)
    {
        this.primeImage = prime;
        this.secondImage = secondary;
    }

    public boolean isHidden()
    {
        return (this.distance < 0);
    }

    public void resetDistance()
    {
        this.distance = -1080;
    }
    public void changeDistance(int distance)
    {
        this.distance+=distance;
    }
    public int getDistance()
    {
        return this.distance;
    }
    

    public void scale(EPolicePositions position)
    {
        this.setY(position.getLocation());
        // By having the police move back a bit when they scale, we can make it easier for the player.
        if (!this.isHidden()) {this.setX(this.getX() - 25);}
        this.myImageView.setFitWidth(position.getWidthScale());
        this.myBound.setWidth(this.myImageView.getFitWidth());
        this.myBound.setHeight(this.myImageView.getFitWidth() * 0.5);

    }

    
    public void swapImage()
    {
        if (isPrime)
        {
            this.setImage(secondImage);
            isPrime = false;
        } else
        {
            this.setImage(primeImage);
            isPrime = true;
        }
    }

    @Override
    public void accept(Visitor visitor) {
        // TODO Auto-generated method stub
        
    }
}
