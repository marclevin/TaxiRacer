package game.display.models;

import game.logic.Acceptor;
import game.logic.Visitor;
import javafx.scene.image.Image;

public class Road extends Sprite implements Acceptor {

    /**
     * 
     * @param x X coordinate of the road
     * @param y Y coordinate of the road
     * @param width Width of the road
     * @param height Height of the road
     */
   
    public Road(int x, int y, Image image)
    {
        super(x, y, image);
    }

    @Override
    public void accept(Visitor visitor) {
        // TODO Auto-generated method stub
        
    }


    
}
