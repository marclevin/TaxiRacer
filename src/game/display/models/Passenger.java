package game.display.models;

import java.util.Random;


import game.logic.Visitor;
import game.utility.EPassenger;

public class Passenger extends Sprite {

    private EPassenger passenger_location=null;

    private double cash = 0;

    public Passenger(int x, int y) {
        super(x, y);
        setCash();
    }


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

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void setLocation(EPassenger passenger_location) {
        this.passenger_location = passenger_location;
    }
    public EPassenger getEPassenger() {
        return passenger_location;
    }

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
