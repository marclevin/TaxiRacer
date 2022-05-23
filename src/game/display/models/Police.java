package game.display.models;

import game.logic.Visitor;
import game.utility.EPolicePositions;
import javafx.scene.image.Image;

public class Police extends Sprite {
    private boolean isPrime = true;
    private int distance = -1080;
    private Image primeImage, secondImage = null;

    /**
     * Constructor for the Police class
     * @param x X coordinate of the police
     * @param y Y coordinate of the police
     */
    public Police(int x, int y) {
        super(x, y);
    }

    /**
     * Sets the image set of the police
     * @param prime The first image of the police
     * @param secondary The second image of the police
     */
    public void setImageSet(Image prime, Image secondary)
    {
        this.primeImage = prime;
        this.secondImage = secondary;
    }

    /**
     * Checks if the police are off screen
     * @return True if the police are off screen, false otherwise
     */
    public boolean isHidden()
    {
        return (this.distance < 0);
    }

    /**
     * Changes the distance of the police from the Taxi
     * @param distance Distance to add
     */
    public void changeDistance(int distance)
    {
        this.distance+=distance;
    }
    /**
     * Gets the distance of the police from the taxi
     * @return The distance of the police from the taxi
     */
    public int getDistance()
    {
        return this.distance;
    }
    

    /**
     * Scales the police based on {@code EPolicePositions}.
     * @param position The position of the police.
     */
    public void scale(EPolicePositions position)
    {
        this.setY(position.getLocation());
        // By having the police move back a bit when they scale, we can make it easier for the player.
        if (!this.isHidden()) {this.setX(this.getX() - 25);}
        this.myImageView.setFitWidth(position.getWidthScale());
        this.myBound.setWidth(this.myImageView.getFitWidth());
        this.myBound.setHeight(this.myImageView.getFitWidth() * 0.5);

    }

    /**
     * Swaps the image of the police, used to make the car appear like it is moving.
     */
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

    /**
     * Acceptor method for the Visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        // This does nothing as the police do not need to accept visitors.
        
    }
}
