package game.display.models;

import game.logic.Visitor;


public class Road extends Sprite {

    /**
     * 
     * @param x X coordinate of the road
     * @param y Y coordinate of the road
     */
    public Road(int x, int y)
    {
        super(x, y);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        
    }


    
}
