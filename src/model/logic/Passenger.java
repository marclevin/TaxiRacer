package model.logic;

import javafx.scene.image.Image;
import model.utility.EPassenger;

public class Passenger extends Sprite implements Acceptor {

    private EPassenger passenger_location=null;

    public Passenger(int x, int y, Image image) {
        super(x, y,image);
        myBound.setHeight(image.getHeight());
        myBound.setWidth(image.getWidth());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void setLocation(EPassenger passenger_location) {
        this.passenger_location = passenger_location;
    }
    public EPassenger getLocation() {
        return passenger_location;
    }

    public void scale(EPassenger info)
    {
        this.setY(info.getLocation());
        this.myImageView.setFitHeight(info.getHeightScale());
        this.myBound.setHeight(info.getHeightScale());
        if (info == EPassenger.PASSENGER_BOTTOM)
        {
           this.myBound.setHeight(100);
           this.myBound.setY(this.myBound.getY() - 40);
        }
    }

    
}
