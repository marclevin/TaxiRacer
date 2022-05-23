package game.display.models;



import game.logic.Visitor;
import game.utility.ETaxiPositions;
import javafx.scene.image.Image;

public class Taxi extends Sprite
{

    /**
     * 
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     * @param width Width of the taxi
     * @param height Height of the taxi
     */
    private double wallet = 0;
    private int career_passengers = 0;
    private transient boolean isPrime = true;
    private transient Image primeImage, secondImage = null;
    private int slowDownPenalty = 120;
    private transient ETaxiPositions myPosition = null;



    public Taxi(int x, int y)
    {
        super(x, y);
    }




    public void setImageSet(Image prime, Image secondary)
    {
        this.primeImage = prime;
        this.secondImage = secondary;
    }
    public void setSlowDown(int slowdown)
    {
        this.slowDownPenalty = slowdown;
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
        career_passengers++;
    }

    public int getCareerPassengers()
    {
        return this.career_passengers;
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
        this.myBound.setY(this.myBound.getY() + 30);
        this.myBound.setHeight(this.myImageView.getFitHeight() * 0.5);
        if (myPosition == ETaxiPositions.FOURTH_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 60);
            this.myBound.setHeight(this.myBound.getHeight() - 40);
        };
        if (myPosition == ETaxiPositions.THIRD_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 50);
            this.myBound.setHeight(this.myBound.getHeight() - 35);
        };
        if (myPosition == ETaxiPositions.SECOND_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() + 40);
            this.myBound.setHeight(this.myBound.getHeight() - 30);
        };
        if (myPosition == ETaxiPositions.FIRST_LANE_TAXI)
        {
            this.myBound.setY(this.myBound.getY() - 20);
        };
        }






    public void setCareerPassengers(int career_passengers) {
        this.career_passengers = career_passengers;
    }

    

    
}