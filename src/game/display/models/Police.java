package game.display.models;

import game.utility.EPositions;
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
    public Police(int x, int y, Image prime, Image second) {
        super(x, y, prime);
        this.primeImage = prime;
        this.secondImage = second;
    }

    public boolean isHidden()
    {
        return (this.distance < 0);
    }

    public void changeDistance(int distance)
    {
        this.distance+=distance;
    }
    public int getDistance()
    {
        return this.distance;
    }
    

    public void scale(EPositions position)
    {
        this.setY(position.getLocation());
        this.myImageView.setFitWidth(position.getWidthScale());
        this.myImageView.setFitHeight(position.getHeightScale());
        this.myBound.setWidth(position.getWidthScale() / 2);
        this.myBound.setHeight(position.getHeightScale() / 2);

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
}
