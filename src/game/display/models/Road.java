package game.display.models;

import game.logic.Visitor;


public class Road extends Sprite {

    /**
     * Constructor for the road.
     * @param x X coordinate of the road
     * @param y Y coordinate of the road
     */
    public Road(int x, int y)
    {
        super(x, y);
    }

    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }


    
}
