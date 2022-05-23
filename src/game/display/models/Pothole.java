package game.display.models;

import game.logic.Visitor;

/**
 * This class represents a pothole.
 */
public class Pothole extends Sprite  {

    /**
     * Constructor for the pothole.
     * @param x X coordinate of the pothole
     * @param y Y coordinate of the pothole
     */
    public Pothole(int x, int y) {
        super(x, y);
        this.myBound.setHeight(25);
        this.myBound.setWidth(25);

    }


    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }
    
}
