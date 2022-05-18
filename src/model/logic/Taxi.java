package model.logic;

import javafx.scene.image.Image;
import model.utility.Acceptor;
import model.utility.Visitor;

public class Taxi extends Sprite implements Acceptor
{

    /**
     * 
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     * @param width Width of the taxi
     * @param height Height of the taxi
     */
    public Taxi(int x, int y) {
        super(x, y);
        this.myImageView.setPreserveRatio(true);
        this.myImageView.setFitHeight(150);
        this.myImageView.setFitWidth(350);
    }

    public Taxi(int x, int y, Image image)
    {
        super(x, y, image);
    }

    @Override
    public void accept(Visitor visitor) {
        // TODO: Add something here lol
        
    }

    

    
}