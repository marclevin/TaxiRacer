package game.display.models;

import game.logic.Visitor;

public class Pothole extends Sprite  {

    public Pothole(int x, int y) {
        super(x, y);
        this.myBound.setHeight(25);
        this.myBound.setWidth(25);

    }


    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }
    
}
