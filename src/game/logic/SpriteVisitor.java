package game.logic;

import java.util.ArrayList;

import game.display.models.Passenger;
import game.display.models.Pothole;
import game.display.models.Road;
import game.utility.EPassenger;

public class SpriteVisitor implements Visitor
{
    private ArrayList<Passenger> bottom_render_list;
    private ArrayList<Passenger> clean_list;
    // This class will be used to help us save game objects by visiting them
    public SpriteVisitor()
    {
        bottom_render_list = new ArrayList<Passenger>();
        clean_list = new ArrayList<Passenger>();
    }

    public ArrayList<Passenger> getBottomRenderList()
    {
        return bottom_render_list;
    }

    public ArrayList<Passenger> getCleanList()
    {
        return clean_list;
    }

    @Override
    public void visit(Pothole pothole) {
        System.out.println("Touched pothole");
        
    }

    @Override
    public void visit(Passenger passenger) {
        if (passenger.getEPassenger() == EPassenger.PASSENGER_BOTTOM) {
            bottom_render_list.add(passenger);
    }
    if (InputHandler.pickupAttempted())
    {
    clean_list.add(passenger);
    InputHandler.pickupBlock();
    }
}

    @Override
    public void visit(Road road) {
        // This doesn't need to do anything, but it's here for completeness
        return;
        
        
    }


}