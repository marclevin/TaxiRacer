package model.logic;

import javafx.scene.image.Image;
import model.utility.Acceptor;
import model.utility.Visitor;

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
    public Taxi(int x, int y) {
        super(x, y);
        this.myImageView.setPreserveRatio(true);
        this.myImageView.setFitHeight(150);
        this.myImageView.setFitWidth(350);
    }

    public Taxi(int x, int y, Image prime, Image secondary)
    {
        super(x, y, prime);
        this.primeImage = prime;
        this.secondImage = secondary;
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

    

    
}