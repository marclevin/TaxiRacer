package model.logic;

import model.utility.Acceptor;
import model.utility.Visitor;

public class Passenger extends Sprite implements Acceptor {

    public Passenger(int x, int y, double width, double height) {
        super(x, y);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }

    
}
