package model.logic;

import javafx.scene.image.Image;
import model.utility.EPositions;

public class Taxi extends Sprite implements Acceptor
{

    /**
     * 
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     * @param width Width of the taxi
     * @param height Height of the taxi
     */
    private boolean isPrime = true;
    private Image primeImage, secondImage = null;
    private int slowDownPenalty = 120;

    public Taxi(int x, int y, Image prime, Image secondary)
    {
        super(x, y, prime);
        this.primeImage = prime;
        this.secondImage = secondary;
    }

    public int getSlowDown()
    {
        return slowDownPenalty;
    }

    public void foo()
    {
        
    }

    @Override
    public void accept(Visitor visitor) {
        // TODO: Add something here lol
        
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

    public void scale(EPositions position)
    {
        this.setY(position.getLocation());
        this.myImageView.setFitWidth(position.getWidthScale());
        this.myImageView.setFitHeight(position.getHeightScale());
        this.myBound.setWidth(position.getWidthScale() / 2.5);
        this.myBound.setHeight(position.getHeightScale() / 2.5);

    }

    

    
}