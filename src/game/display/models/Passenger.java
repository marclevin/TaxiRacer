package game.display.models;

import java.util.Random;


import game.logic.Visitor;
import game.utility.EPassenger;

/**
 * This class represents a passenger.
 */
public class Passenger extends Sprite {

    private EPassenger passenger_location=null;

    private double cash = 0;

    /**
     * Constructor for the passenger.
     * @param x X coordinate of the passenger
     * @param y Y coordinate of the passenger
     */
    public Passenger(int x, int y) {
        super(x, y);
        setCash();
    }


    /**
     * Gets the cash of the passenger.
     * @return The cash of the passenger.
     */
    public double getCash()
    {
        return this.cash;
    }

    

    /**
     * This function sets the passenger to have random cash in the range 1 - 10.
     */
    public void setCash()
    {
        Random rand = new Random();
        this.cash = rand.nextDouble(10.0) + 1.0;
    }

    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    /**
     * Sets the passenger location.
     * @param passenger_location The passenger location.
     */
    public void setLocation(EPassenger passenger_location) {
        this.passenger_location = passenger_location;
    }
    /**
     * Gets the passenger location.
     * @return The passenger location.
     */
    public EPassenger getEPassenger() {
        return passenger_location;
    }

    /**
     * Sets the passenger scale
     * @param info The passenger scale.
     */
    public void scale(EPassenger info)
    {
        this.passenger_location = info;
        this.setY(passenger_location.getLocation());
        this.myImageView.setFitHeight(passenger_location.getHeightScale());
        this.myBound.setHeight(passenger_location.getHeightScale());
        this.myBound.setWidth(100);
        if (passenger_location == EPassenger.PASSENGER_BOTTOM)
        {
           this.myBound.setHeight(100);
           this.myBound.setY(this.myBound.getY() - 40);
        }
    }

    
}
