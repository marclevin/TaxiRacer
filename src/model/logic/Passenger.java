package model.logic;

import javafx.scene.image.Image;
import model.utility.Acceptor;
import model.utility.Visitor;

public class Passenger extends Sprite implements Acceptor {

    public Passenger(int x, int y, Image image) {
        super(x, y,image);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }

    
}
