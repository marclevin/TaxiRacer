package game.display.models;

import game.logic.Acceptor;
import game.logic.Visitor;
import game.utility.ETaxiPositions;
import javafx.scene.image.Image;

public class Taxi extends Sprite implements Acceptor
{

    /**
     * 
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     * @param width Width of the taxi
     * @param height Height of the taxi
     */
    private double wallet = 0;
    private boolean isPrime = true;
    private Image primeImage, secondImage = null;
    private int slowDownPenalty = 120;
    private ETaxiPositions myPosition = null;

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

    public ETaxiPositions getPosition()
    {
        return myPosition;
    }


    public void addCash(double cash)
    {
        wallet+= cash;
    }

    public double getWallet()
    {
        return this.wallet;
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


    public void scale(ETaxiPositions position)
    {
        myPosition = position;
        this.setY(myPosition.getLocation());
        this.myImageView.setFitHeight(myPosition.getHeightScale());
        this.myBound.setWidth(this.myImageView.getFitHeight()*1.5);
        this.myBound.setHeight(this.myImageView.getFitHeight() * 0.5);

    }

    

    
}