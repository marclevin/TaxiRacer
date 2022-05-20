package model.logic;

import javafx.scene.image.Image;

public class Passenger extends Sprite implements Acceptor {

    public Passenger(int x, int y, Image image) {
        super(x, y,image);
        myBound.setHeight(image.getHeight());
        myBound.setWidth(image.getWidth());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }

    
}
